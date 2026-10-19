package com.sanathanatattva.partner.alerts

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sanathanatattva.partner.MainActivity
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiProblem
import com.sanathanatattva.partner.data.api.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * While online: a location foreground service with the ongoing "You're online"
 * notification (handoff §6.1). It shares the phone's position every minute or so, and
 * keeps the app alive on phones that kill background apps.
 */
class OnlineService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val fix = result.lastLocation ?: return
            scope.launch {
                val sent = applicationContext.appContainer.partner.sendLocation(fix.latitude, fix.longitude, fix.accuracy)
                val p = (sent as? ApiResult.Failure)?.problem
                // 409: set offline elsewhere (admin, auto-offline). 401: logged out.
                if (p is ApiProblem.Server && (p.code == 409 || p.code == 401)) stopOnline(this@OnlineService)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_GO_OFFLINE) {
            scope.launch {
                applicationContext.appContainer.partner.goOffline()
                stopOnline(this@OnlineService)
            }
            return START_NOT_STICKY
        }
        ensureChannel(this)
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        val started = runCatching { ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type) }.isSuccess
        if (!started) {
            stopSelf()
            return START_NOT_STICKY
        }
        _running.value = true
        if (intent?.action == ACTION_DELIVERING) fast = intent.getBooleanExtra(EXTRA_ON, false)
        requestUpdates()
        return START_STICKY
    }

    private var fast = false

    /**
     * Idle online: a fix a minute is enough to match orders. While carrying an order the
     * shop and customer watch the dot move, so every 10 s at full accuracy.
     */
    @SuppressLint("MissingPermission")
    private fun requestUpdates() {
        runCatching { client.removeLocationUpdates(callback) }
        val request = if (fast) {
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L).setMinUpdateIntervalMillis(5_000L).build()
        } else {
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60_000L)
                .setMinUpdateIntervalMillis(30_000L)
                .setMinUpdateDistanceMeters(30f)
                .build()
        }
        runCatching { client.requestLocationUpdates(request, callback, Looper.getMainLooper()) }
    }

    private fun notification() = NotificationCompat.Builder(this, getString(R.string.channel_online_id))
        .setSmallIcon(R.drawable.ic_notification)
        .setColor(ContextCompat.getColor(this, R.color.notification_green))
        .setContentTitle(getString(R.string.online_notification_title))
        .setContentText(getString(R.string.online_notification_body))
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(
            PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE),
        )
        .addAction(
            0, getString(R.string.online_notification_stop),
            PendingIntent.getService(this, 2, Intent(this, OnlineService::class.java).setAction(ACTION_GO_OFFLINE), PendingIntent.FLAG_IMMUTABLE),
        )
        .build()

    override fun onDestroy() {
        runCatching { client.removeLocationUpdates(callback) }
        _running.value = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 4101
        private const val ACTION_GO_OFFLINE = "go_offline"
        private const val ACTION_DELIVERING = "delivering"
        private const val EXTRA_ON = "on"
        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, OnlineService::class.java)) }
        }

        /** Faster, precise location while an order is on its way (only if already online). */
        fun delivering(context: Context, on: Boolean) {
            if (!running.value) return
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, OnlineService::class.java).setAction(ACTION_DELIVERING).putExtra(EXTRA_ON, on))
            }
        }

        fun stopOnline(context: Context) {
            context.stopService(Intent(context, OnlineService::class.java))
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    context.getString(R.string.channel_online_id),
                    context.getString(R.string.channel_online_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = context.getString(R.string.channel_online_body) },
            )
        }
    }
}
