package com.sanathanatattva.partner.alerts

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sanathanatattva.partner.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.sanathanatattva.partner.MainActivity
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiProblem
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.ui.components.PrimaryButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.sanathanatattva.partner.push.canPostNotifications
import com.sanathanatattva.partner.ui.ring.RingScreen
import com.sanathanatattva.partner.ui.theme.St
import com.sanathanatattva.partner.ui.theme.StTheme

/**
 * The full-screen ring (handoff §6.1): opens over the lock screen, turns the screen on,
 * rings on loop and reads the order out until the partner accepts or declines.
 */
class RingActivity : AppCompatActivity() {
    private lateinit var sound: AlertSound
    private var offer by mutableStateOf<Offer?>(null)
    private var problem by mutableStateOf<String?>(null)
    private var answering by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        // The screen takes over from the notification's own looping sound, and rings at once:
        // the order details arrive a moment later.
        NotificationManagerCompat.from(this).cancel(OrderAlert.NOTIFICATION_ID)
        sound = AlertSound(this, getString(R.string.narration_locale))
        sound.ring()
        load(intent)

        setContent {
            StTheme {
                Box(Modifier.fillMaxSize().background(St.Cream)) {
                    val current = offer
                    if (current == null) {
                        RingLoading(problem, onClose = { finishWith(null) })
                    } else {
                        // Measured once per offer: a new total every frame would restart the countdown and the slider.
                        val total = remember(current.id) { (current.expiresAt - System.currentTimeMillis()).coerceAtLeast(1) }
                        RingScreen(
                            offer = current,
                            totalMs = total,
                            onHearAgain = { sound.announce(announcementFor(current)) },
                            onAccept = { finishWith(accepted = true) },
                            onDecline = { finishWith(accepted = false) },
                        )
                    }
                }
            }
        }
    }

    /** A re-ring for the same offer just keeps this screen; a different offer replaces it. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotificationManagerCompat.from(this).cancel(OrderAlert.NOTIFICATION_ID)
        if (intent.getStringExtra(EXTRA_ID) != offer?.id) load(intent)
    }

    private fun load(intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: "test"
        val forRider = intent.getBooleanExtra(EXTRA_RIDER, false)
        if (intent.getBooleanExtra(EXTRA_TEST, false) || id.toLongOrNull() == null) {
            show(Offer.sample(forRider = forRider, id = id))
            return
        }
        problem = null
        lifecycleScope.launch {
            repeat(3) { attempt ->
                when (val r = appContainer.orders.offer(id.toLong())) {
                    is ApiResult.Success -> {
                        if (!r.value.open) {
                            Toast.makeText(this@RingActivity, getString(R.string.ring_gone), Toast.LENGTH_LONG).show()
                            finishWith(null)
                        } else {
                            show(Offer.from(r.value))
                        }
                        return@launch
                    }
                    is ApiResult.Failure -> if (attempt < 2) delay(1500)
                }
            }
            problem = getString(R.string.ring_load_failed)
        }
    }

    private fun show(o: Offer) {
        offer = o
        sound.announce(announcementFor(o))
    }

    /** accepted = null closes without answering (offer gone, or the partner backed out of a broken load). */
    private fun finishWith(accepted: Boolean?) {
        if (answering) return
        val current = offer
        sound.stop()
        if (accepted == null || current == null || current.test || current.offerId == null) {
            if (current?.test == true && accepted == true) {
                Toast.makeText(this, getString(R.string.ring_test_done_title), Toast.LENGTH_LONG).show()
            }
            finish()
            return
        }
        answering = true
        lifecycleScope.launch {
            val orders = appContainer.orders
            val r = if (accepted) orders.accept(current.offerId) else orders.decline(current.offerId)
            when (r) {
                is ApiResult.Success -> {
                    val orderId = r.value.orderId
                    if (accepted && orderId != null) {
                        startActivity(
                            Intent(this@RingActivity, MainActivity::class.java)
                                .putExtra(OpenOrder.EXTRA, orderId)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        )
                    }
                }
                is ApiResult.Failure -> {
                    val message = (r.problem as? ApiProblem.Server)?.message ?: getString(R.string.error_network)
                    Toast.makeText(this@RingActivity, message, Toast.LENGTH_LONG).show()
                }
            }
            finish()
        }
    }

    override fun onDestroy() {
        sound.release()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ID = "offer_id"
        private const val EXTRA_RIDER = "for_rider"
        private const val EXTRA_TEST = "test"

        fun intent(context: Context, id: String, forRider: Boolean, test: Boolean) =
            Intent(context, RingActivity::class.java)
                .putExtra(EXTRA_ID, id).putExtra(EXTRA_RIDER, forRider).putExtra(EXTRA_TEST, test)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}

/** The second or so between the ring starting and the order arriving. */
@Composable
private fun RingLoading(problem: String?, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.ring_title_trader), style = MaterialTheme.typography.headlineMedium, color = St.Ink)
        if (problem == null) {
            CircularProgressIndicator(color = St.Green, strokeWidth = 3.dp)
        } else {
            Text(problem, style = MaterialTheme.typography.bodyLarge, color = St.Ink2, textAlign = TextAlign.Center)
            PrimaryButton(text = stringResource(R.string.close), arrow = false, onClick = onClose)
        }
    }
}

/** Posts the ringing notification whose full-screen intent opens [RingActivity]. */
object OrderAlert {
    const val NOTIFICATION_ID = 4201

    fun show(context: Context, id: String, forRider: Boolean, test: Boolean, summary: String? = null, expiresAt: Long? = null) {
        if (!canPostNotifications(context)) return
        OrderChannel.ensure(context)
        val open = PendingIntent.getActivity(
            context, id.hashCode(),
            RingActivity.intent(context, id, forRider, test),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val sample = Offer.sample(forRider, id)
        val first = sample.lines.first()
        val text = summary ?: ("${first.qty} × ${first.litres} L ${first.oil}" + if (sample.lines.size > 1) " + ${sample.lines.size - 1}" else "")
        val until = expiresAt ?: sample.expiresAt
        val notification = NotificationCompat.Builder(context, OrderChannel.id(context))
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_green))
            .setContentTitle(context.getString(if (forRider) R.string.ring_title_rider else R.string.ring_title_trader))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(open, true)
            .setContentIntent(open)
            .setOngoing(true)
            .setAutoCancel(true)
            .setTimeoutAfter((until - System.currentTimeMillis()).coerceAtLeast(60_000))
            .build()
        // Ring until it's answered, like a phone call.
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }
}
