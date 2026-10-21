package com.sanathanatattva.partner.push

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sanathanatattva.partner.MainActivity
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.data.shop.whatsappNumber
import com.sanathanatattva.partner.ui.shop.ShopLink
import com.sanathanatattva.partner.ui.shop.ShopTarget

/**
 * My Shop's phone alerts (canvas "Phone alerts"). The server sends them as data so the app can
 * add the buttons: "Ask for stock · Later", "See money", "See order", "Call · WhatsApp".
 */
object ShopAlerts {
    private val TYPES = setOf("stock_low", "commission_paid", "restock", "sub_dealer_stock")

    /** True when [data] was a shop alert and has been shown. */
    fun show(context: Context, data: Map<String, String>): Boolean {
        val type = data["type"] ?: return false
        if (type !in TYPES) return false
        if (!canPostNotifications(context)) return true
        val title = data["title"] ?: return true
        val body = data["body"].orEmpty()
        val id = (type + (data["product_id"] ?: data["restock_id"] ?: data["sub_dealer_id"] ?: "")).hashCode()

        val tapTarget = when (type) {
            "stock_low" -> ShopTarget.AskStock
            "commission_paid" -> ShopTarget.Money
            "restock" -> ShopTarget.Orders
            else -> data["sub_dealer_id"]?.toLongOrNull()?.let { ShopTarget.SubDealer(it) } ?: ShopTarget.Orders
        }
        val builder = NotificationCompat.Builder(context, context.getString(R.string.channel_account_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_green))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open(context, id, tapTarget))

        when (type) {
            "stock_low" -> {
                builder.addAction(0, context.getString(R.string.notif_ask), open(context, id + 1, ShopTarget.AskStock))
                builder.addAction(0, context.getString(R.string.notif_later), dismiss(context, id))
            }
            "commission_paid" -> builder.addAction(0, context.getString(R.string.notif_see_money), open(context, id + 1, ShopTarget.Money))
            "restock" -> builder.addAction(0, context.getString(R.string.notif_see_order), open(context, id + 1, ShopTarget.Orders))
            "sub_dealer_stock" -> data["phone"]?.takeIf { it.isNotBlank() }?.let { phone ->
                val digits = phone.filter { it.isDigit() || it == '+' }
                builder.addAction(0, context.getString(R.string.call), activity(context, id + 2, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))))
                whatsappNumber(phone)?.let { wa ->
                    builder.addAction(0, context.getString(R.string.whatsapp), activity(context, id + 3, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$wa"))))
                }
            }
        }
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(id, builder.build())
        return true
    }

    private fun open(context: Context, requestCode: Int, target: ShopTarget): PendingIntent {
        val intent = ShopLink.extras(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            target,
        )
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun activity(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(context, requestCode, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun dismiss(context: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, id,
            Intent(context, DismissReceiver::class.java).putExtra(DismissReceiver.EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

/** "Later" on a stock alert: just puts the notification away. */
class DismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_ID, 0))
    }

    companion object {
        const val EXTRA_ID = "notification_id"
    }
}
