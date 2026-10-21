package com.sanathanatattva.partner.ui.review

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.ui.components.Emblem
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.SecondaryButton
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.around
import com.sanathanatattva.partner.ui.components.box
import com.sanathanatattva.partner.ui.components.delayed
import com.sanathanatattva.partner.ui.components.dot
import com.sanathanatattva.partner.ui.components.fill
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.label
import com.sanathanatattva.partner.ui.components.line
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.login.dialSupport
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.push.canPostNotifications
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sanathanatattva.partner.data.api.ApiProblem
import com.sanathanatattva.partner.data.api.ApiResult
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Rider waiting for admin review (canvas "Under review"). Checks the decision whenever
 * the screen is open (every 30 s) and moves on to Approved or "fix & resend" by itself.
 */
@Composable
fun UnderReviewScreen(onLoggedOut: () -> Unit, onApproved: (firstName: String) -> Unit, onSentBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logOutHaptics = LocalHapticFeedback.current
    val container = context.appContainer
    var sentAt by remember { mutableStateOf<String?>(null) }
    // "We'll send you a notification": ask once, right where that promise is made.
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scope.launch { container.push.register() }
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !canPostNotifications(context)) askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val today = stringResource(R.string.time_today)
    val yesterday = stringResource(R.string.time_yesterday)
    LifecycleResumeEffect(Unit) {
        val job = scope.launch {
            while (true) {
                when (val result = container.riders.application()) {
                    is ApiResult.Success -> {
                        val app = result.value
                        sentAt = app.submittedAt?.let { friendlyTime(it, today, yesterday) }
                        when (app.reviewStatus) {
                            "approved" -> {
                                container.auth.refresh()
                                onApproved(app.application?.name?.trim()?.substringBefore(' ').orEmpty())
                                return@launch
                            }
                            "rejected" -> {
                                onSentBack()
                                return@launch
                            }
                        }
                    }
                    is ApiResult.Failure -> {
                        val p = result.problem
                        if (p is ApiProblem.Server && (p.code == 401 || p.code == 403)) {
                            container.auth.logOut()
                            onLoggedOut()
                            return@launch
                        }
                    }
                }
                delay(30_000)
            }
        }
        onPauseOrDispose { job.cancel() }
    }
    val reduceMotion = LocalReduceMotion.current
    val enter = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(1100)) }
    fun Modifier.rise(delayFraction: Float) = graphicsLayer {
        val p = OilEase.transform(((enter.value - delayFraction) / (1f - delayFraction)).coerceIn(0f, 1f))
        alpha = p
        translationY = 14.dp.toPx() * (1f - p)
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Emblem(40.dp)
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize * (20f / 22f)), color = St.GreenDeep, modifier = Modifier.weight(1f))
            TextButton(
                onClick = { logOutHaptics.performHapticFeedback(HapticFeedbackType.ToggleOff); scope.launch { context.appContainer.auth.logOut(); onLoggedOut() } },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.review_log_out), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight(650)), color = St.Green)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            FillingCanArt(Modifier.padding(top = 8.dp).fillMaxWidth().height(250.dp))
            Text(
                stringResource(R.string.review_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = St.Ink,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).rise(0f).semantics { heading() },
            )
            Text(
                stringResource(R.string.review_body),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize * (18f / 17f)),
                textAlign = TextAlign.Center,
                color = St.Ink2,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).rise(0.1f),
            )
            Column(
                Modifier
                    .padding(top = 20.dp, bottom = 16.dp)
                    .rise(0.2f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(St.Paper)
                    .border(2.dp, St.LineSoft, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                ReviewStep(StepState.Done, stringResource(R.string.review_step_sent), sentAt)
                ReviewStep(StepState.Now, stringResource(R.string.review_step_checking), stringResource(R.string.review_step_checking_body))
                ReviewStep(StepState.Next, stringResource(R.string.review_step_start), stringResource(R.string.review_step_start_body), last = true)
            }
        }
        SecondaryButton(
            text = stringResource(R.string.call_support),
            onClick = { context.dialSupport() },
            green = true,
            leading = { Icon(StIcons.Phone, contentDescription = null, modifier = Modifier.size(22.dp)) },
        )
    }
}

private enum class StepState { Done, Now, Next }

@Composable
private fun ReviewStep(state: StepState, title: String, detail: String?, last: Boolean = false) {
    val pulse by rememberLoop(1800, still = 1f)
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (state) {
                StepState.Done -> Box(Modifier.size(28.dp).clip(CircleShape).background(St.Green), contentAlignment = Alignment.Center) {
                    Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(16.dp))
                }
                StepState.Now -> Box(
                    Modifier
                        .size(28.dp)
                        .drawBehind {
                            val p = OilEase.transform((pulse / 0.75f).coerceAtMost(1f))
                            drawCircle(St.Gold.copy(alpha = 0.6f * (1f - p)), radius = size.minDimension / 2f + 12.dp.toPx() * p)
                        }
                        .clip(CircleShape)
                        .background(St.Paper)
                        .border(3.dp, St.Gold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.size(12.dp).clip(CircleShape).background(St.Gold)) }
                StepState.Next -> Box(Modifier.size(28.dp).clip(CircleShape).background(St.Paper).border(3.dp, St.Line, CircleShape))
            }
            if (!last) {
                val solid = state == StepState.Done
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 18.dp)
                        .width(3.dp)
                        .drawBehind {
                            if (solid) drawRect(St.Green)
                            else drawLine(
                                St.Line, center.copy(y = 0f), center.copy(y = size.height), 3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                            )
                        },
                )
            }
        }
        Column(Modifier.padding(bottom = if (last) 0.dp else 14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleMedium.fontSize * (18f / 19f)),
                color = if (state == StepState.Now) St.GoldInk else St.Ink,
                modifier = Modifier.heightIn(min = 28.dp),
            )
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = St.Ink2)
        }
    }
}

private object Can {
    val glass = svgPath("M164 112h22v14q0 6 6 9l14 7q14 7 14 22v52q0 18-18 18h-54q-18 0-18-18v-52q0-15 14-22l14-7q6-3 6-9z")
    val spout = svgPath("M168 32h14l-3 14h-8z")
    val drop = svgPath("M175 46c0 0-6 8-6 12.5a6 6 0 0 0 12 0C181 54 175 46 175 46z")
    val wave = svgPath("M120 170q12-6 24 0t24 0t24 0t24 0t24 0t24 0t24 0t24 0V250H120z")
    val glints = listOf(
        Triple(svgPath("M74 92l3 7 7 3-7 3-3 7-3-7-7-3 7-3z"), St.Gold, 400),
        Triple(svgPath("M276 150l2.5 6 6 2.5-6 2.5-2.5 6-2.5-6-6-2.5 6-2.5z"), St.Gold, 1900),
        Triple(svgPath("M258 64l2 5 5 2-5 2-2 5-2-5-5-2 5-2z"), St.Green, 2800),
    )
    val glintCentres = listOf(74f to 102f, 276f to 158.5f, 258f to 71f)
}

/** A can slowly filling under the oil press, drop by drop: patient, not stuck. */
@Composable
private fun FillingCanArt(modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val drip by rememberLoop(2600, still = 0.9f)
    val waveClock by rememberLoop(3200)
    val levelClock by rememberLoop(10400, still = 0.5f)
    val bubbles by rememberLoop(4000)
    val glint by rememberLoop(3400)
    val description = stringResource(R.string.review_art)
    Canvas(modifier.semantics { contentDescription = description }) {
        viewBox(350f, 250f) {
            dot(175f, 140f, 108f, fill = St.Sand)
            Can.glints.forEachIndexed { i, (path, color, delay) ->
                val t = glint.delayed(delay, 3400)
                val a = kf(t, 0.6f to 0f, 0.75f to 1f, 1f to 0f, easing = EaseInOut)
                if (a > 0f) {
                    val (cx, cy) = Can.glintCentres[i]
                    around(cx, cy, rotate = 45f * a, sx = 0.4f + 0.6f * a) { fill(path, color, alpha = a) }
                }
            }
            box(108f, 14f, 134f, 18f, 7f, fill = St.Wood, stroke = St.Ink, strokeWidth = 2.5f)
            box(116f, 8f, 12f, 30f, 4f, fill = St.WoodDark, stroke = St.Ink, strokeWidth = 2.5f)
            box(222f, 8f, 12f, 30f, 4f, fill = St.WoodDark, stroke = St.Ink, strokeWidth = 2.5f)
            fill(Can.spout, St.GoldDeep); line(Can.spout, St.Ink, 2.5f)

            // The drop swells at the spout, falls, and vanishes into the oil.
            val dropAlpha = if (drip < 0.54f) 1f else 0f
            if (dropAlpha > 0f) {
                val grow = kf(drip, 0f to 0.25f, 0.34f to 1f, easing = EaseInOut)
                val fall = kf(drip, 0.34f to 0f, 0.52f to 104f, easing = EaseIn)
                val sx = if (drip < 0.34f) grow else kf(drip, 0.34f to 1f, 0.52f to 0.9f)
                val sy = if (drip < 0.34f) grow else kf(drip, 0.34f to 1f, 0.52f to 1.15f)
                translate(0f, fall) { around(175f, 46f, sx = sx, sy = sy) { fill(Can.drop, St.Gold) } }
            }

            clipPath(Can.glass) {
                box(120f, 110f, 110f, 140f, fill = St.Paper)
                val level = kf(levelClock, 0f to 4f, 0.5f to -6f, 1f to 4f, easing = EaseInOut)
                translate(0f, level) {
                    translate(-48f * waveClock, 0f) { fill(Can.wave, St.Gold) }
                    box(120f, 180f, 110f, 70f, fill = St.Gold)
                    listOf(Triple(150f, 226f, 3f) to -1000, Triple(196f, 230f, 2.4f) to -2600, Triple(172f, 232f, 2f) to -3400)
                        .forEach { (b, delay) ->
                            val p = bubbles.delayed(delay, 4000)
                            val a = kf(p, 0f to 0f, 0.2f to 0.7f, 1f to 0f)
                            dot(b.first, b.second - 52f * EaseIn.transform(p), b.third, fill = St.GoldGlint, alpha = a)
                        }
                    val rip = kf(drip, 0.52f to 0f, 0.55f to 1f, 0.90f to 2f)
                    if (rip in 0.01f..1.99f) {
                        val s = if (rip <= 1f) 0.2f + 0.15f * rip else 0.35f + 1.05f * OilEase.transform(rip - 1f)
                        val a = if (rip <= 1f) 0.9f * rip else 0.9f * (1f - OilEase.transform(rip - 1f))
                        around(175f, 171f, sx = s) {
                            drawOval(St.GoldGlint, Offset(155f, 167f), Size(40f, 8f), alpha = a, style = Stroke(2.5f))
                        }
                    }
                }
                box(134f, 146f, 10f, 70f, 5f, fill = Color.White, alpha = 0.5f)
            }
            line(Can.glass, St.Ink, 3f)
            box(160f, 104f, 30f, 10f, 3f, fill = St.Paper, stroke = St.Ink, strokeWidth = 3f)
            box(146f, 188f, 58f, 30f, 7f, fill = St.Cream, stroke = St.Ink)
            label(measurer, "ST", 175f, 209f, 16f, St.Green, weight = FontWeight.ExtraBold, tracking = 1f)
        }
    }
}

/** SQLite CURRENT_TIMESTAMP ("2026-10-19 06:20:11", UTC) → "Today, 11:50 AM". */
fun friendlyTime(utc: String, today: String, yesterday: String): String? = runCatching {
    val local = LocalDateTime.parse(utc.trim().replace(' ', 'T')).atOffset(ZoneOffset.UTC)
        .atZoneSameInstant(ZoneId.systemDefault())
    val time = local.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
    val date = local.toLocalDate()
    val now = LocalDate.now()
    when (date) {
        now -> "$today, $time"
        now.minusDays(1) -> "$yesterday, $time"
        else -> local.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)) + ", " + time
    }
}.getOrNull()
