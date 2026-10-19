package com.sanathanatattva.partner.alerts

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.sanathanatattva.partner.R

/** The five settings that decide whether an order alert gets through (handoff §4.6). */
enum class AlertCheck { Notifications, Location, Battery, FullScreen, DoNotDisturb }

data class Readiness(val on: Set<AlertCheck>) {
    val missing: List<AlertCheck> get() = AlertCheck.entries.filter { it !in on }
    val ready: Boolean get() = missing.isEmpty()
}

object AlertReadiness {
    fun check(context: Context): Readiness {
        val nm = context.getSystemService(NotificationManager::class.java)
        val on = buildSet {
            if (notificationsOn(context)) add(AlertCheck.Notifications)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                add(AlertCheck.Location)
            }
            if (context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)) add(AlertCheck.Battery)
            if (Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent()) add(AlertCheck.FullScreen)
            if (nm.isNotificationPolicyAccessGranted) add(AlertCheck.DoNotDisturb)
        }
        return Readiness(on)
    }

    private fun notificationsOn(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        // The order channel itself can be muted separately in system settings.
        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(OrderChannel.id(context))
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun appUri(context: Context): Uri = "package:${context.packageName}".toUri()

    /** The settings page that turns [check] on. Runtime permissions are asked in-app instead. */
    @SuppressLint("BatteryLife")
    fun settingsIntent(context: Context, check: AlertCheck): Intent = when (check) {
        AlertCheck.Notifications -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        AlertCheck.Location -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri(context))
        // The direct "allow" dialog; Play allows it for apps whose core job is time-critical alerts.
        AlertCheck.Battery -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, appUri(context))
        AlertCheck.FullScreen -> if (Build.VERSION.SDK_INT >= 34) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, appUri(context))
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri(context))
        }
        AlertCheck.DoNotDisturb -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/**
 * The loud channel orders ring on: plays the phone's ringtone, vibrates, and is allowed
 * through Do Not Disturb once the app has DND access. Android fixes a channel's sound and
 * DND setting when it is created, so when DND access arrives later the channel is
 * recreated under a new id.
 */
object OrderChannel {
    private const val PREFS = "order_channel"
    private const val KEY = "id"
    private const val BASE = "order_alerts"

    fun id(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "${BASE}_1") ?: "${BASE}_1"

    fun ensure(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        var id = id(context)
        val existing = nm.getNotificationChannel(id)
        if (existing != null && (existing.canBypassDnd() || !nm.isNotificationPolicyAccessGranted)) return
        if (existing != null) {
            // DND access was granted after the channel was made: start a fresh one that can bypass it.
            nm.deleteNotificationChannel(id)
            val n = id.substringAfterLast('_').toIntOrNull() ?: 1
            id = "${BASE}_${n + 1}"
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, id).apply()
        }
        val channel = NotificationChannel(id, context.getString(R.string.channel_orders_name), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.channel_orders_body)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 900, 600, 900, 600, 900)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            if (nm.isNotificationPolicyAccessGranted) setBypassDnd(true)
        }
        nm.createNotificationChannel(channel)
    }
}

/**
 * Phone makers that stop background apps unless "Autostart" (or similar) is on. Android
 * can't report that switch, so the guide asks the partner to confirm it themselves.
 */
data class AutostartGuide(val brand: String, val steps: List<Pair<Int, Int>>, val intents: List<Intent>)

object Autostart {
    fun guideFor(manufacturer: String = Build.MANUFACTURER): AutostartGuide? {
        val m = manufacturer.lowercase()
        return when {
            m in setOf("xiaomi", "redmi", "poco") -> AutostartGuide(
                "Redmi",
                listOf(R.string.guide_mi_1_title to R.string.guide_mi_1_body, R.string.guide_mi_2_title to R.string.guide_mi_2_body, R.string.guide_mi_3_title to R.string.guide_mi_3_body),
                listOf(component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            )
            m in setOf("oppo", "realme", "oneplus") -> AutostartGuide(
                when (m) { "oppo" -> "Oppo"; "realme" -> "Realme"; else -> "OnePlus" },
                listOf(R.string.guide_coloros_1_title to R.string.guide_coloros_1_body, R.string.guide_coloros_2_title to R.string.guide_coloros_2_body, R.string.guide_coloros_3_title to R.string.guide_coloros_3_body),
                listOf(
                    component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                    component("com.oplus.battery", "com.oplus.startupapp.view.StartupAppListActivity"),
                    component("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                ),
            )
            m == "vivo" || m == "iqoo" -> AutostartGuide(
                "Vivo",
                listOf(R.string.guide_vivo_1_title to R.string.guide_vivo_1_body, R.string.guide_vivo_2_title to R.string.guide_vivo_2_body, R.string.guide_vivo_3_title to R.string.guide_vivo_3_body),
                listOf(component("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"), component("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            )
            m == "samsung" -> AutostartGuide(
                "Samsung",
                listOf(R.string.guide_samsung_1_title to R.string.guide_samsung_1_body, R.string.guide_samsung_2_title to R.string.guide_samsung_2_body, R.string.guide_samsung_3_title to R.string.guide_samsung_3_body),
                emptyList(),
            )
            else -> null
        }
    }

    private fun component(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Tries the maker's own autostart screen, then falls back to this app's settings page. */
    fun open(context: Context, guide: AutostartGuide) {
        for (intent in guide.intents) {
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private const val PREFS = "autostart"
    fun confirmed(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("confirmed", false)
    fun confirm(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("confirmed", true).apply()
}
