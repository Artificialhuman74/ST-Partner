package com.sanathanatattva.partner.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sanathanatattva.partner.BuildConfig
import com.sanathanatattva.partner.MainActivity
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.alerts.OfferEvents
import com.sanathanatattva.partner.alerts.OnlineService
import com.sanathanatattva.partner.alerts.OrderAlert
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.SessionStore
import com.sanathanatattva.partner.data.api.DeviceRegistration
import com.sanathanatattva.partner.data.api.DeviceToken
import com.sanathanatattva.partner.data.api.PartnerApi
import com.sanathanatattva.partner.data.api.apiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Tells the backend which phone to push to for the signed-in partner, and stops it on
 * logout. Quietly does nothing when Firebase isn't set up in the build.
 */
class PushRegistrar(private val context: Context, private val api: PartnerApi, private val sessions: SessionStore) {
    private fun ready() = FirebaseApp.getApps(context).isNotEmpty()

    suspend fun register(token: String? = null) {
        if (!ready() || sessions.current() == null) return
        val fcm = token ?: runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        apiCall { api.registerDevice(DeviceRegistration(fcm, appVersion = BuildConfig.VERSION_NAME)) }
    }

    /** Call while still signed in (the server needs to know whose phone it is). */
    suspend fun unregister() {
        if (!ready() || sessions.current() == null) return
        val fcm = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        apiCall { api.unregisterDevice(DeviceToken(fcm)) }
    }
}

object Channels {
    /** Approval and profile news. Order alerts get their own loud channel later (handoff §4.5). */
    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                context.getString(R.string.channel_account_id),
                context.getString(R.string.channel_account_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.channel_account_body) },
        )
        // "Pack this order", "Ravi is coming", "Picked up", "Delivered": the server sends these on orders_info.
        manager.createNotificationChannel(
            NotificationChannel(
                ORDER_UPDATES,
                context.getString(R.string.channel_updates_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.channel_updates_body) },
        )
    }

    const val ORDER_UPDATES = "orders_info"
}

fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

class PartnerMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { applicationContext.appContainer.push.register(token) }
    }

    /**
     * Notification messages are shown by the system while the app is in the background;
     * this shows them when the app is open too, so "You're approved!" is never missed.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        when (message.data["type"]) {
            "offer_closed" -> {
                val id = message.data["offer_id"] ?: return
                OrderAlert.cancelIf(this, id)
                OfferEvents.close(id)
                return
            }
            "auto_offline" -> {
                // Three missed orders: the server set them offline; stop sharing location and say why.
                OnlineService.stopOnline(this)
                show(getString(R.string.home_auto_offline_title), getString(R.string.home_auto_offline_body), channel = getString(R.string.channel_account_id), data = message.data)
                return
            }
        }
        if (message.data["type"] == "order_offer") {
            OrderAlert.show(
                this,
                id = message.data["offer_id"] ?: "offer",
                forRider = message.data["role"] == "rider",
                test = message.data["test"] == "1",
                summary = message.data["summary"],
                expiresAt = message.data["expires_at"]?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() },
            )
            return
        }
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val channel = message.notification?.channelId?.takeIf { it == Channels.ORDER_UPDATES } ?: getString(R.string.channel_account_id)
        show(title, body, channel, message.data, message.messageId)
    }

    /** Posts a notification; tapping it opens the app, straight on the order when it is about one. */
    private fun show(title: String, body: String, channel: String, data: Map<String, String>, messageId: String? = null) {
        if (!canPostNotifications(this)) return
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        data["order_id"]?.toLongOrNull()?.let { intent.putExtra("type", data["type"]).putExtra("order_id", it.toString()) }
        val open = PendingIntent.getActivity(
            this, (messageId ?: title).hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.notification_green))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(this).notify(messageId?.hashCode() ?: title.hashCode(), notification)
    }
}
