package com.sanathanatattva.partner.ui.loggedin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.around
import com.sanathanatattva.partner.ui.components.dot
import com.sanathanatattva.partner.ui.components.fill
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.line
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.seg
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.delay
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

private const val TOTAL_MS = 3300f
private val Pour = CubicBezierEasing(0.3f, 0.1f, 0.2f, 1f)
private val Check = svgPath("M58 86l18 18 36-40")
private val Wave = svgPath("M0 10Q22 0 44 10T88 10T132 10T176 10T220 10T264 10T308 10T352 10V18H0Z")
private val BurstDrop = svgPath("M0 -8C0 -8 -6 -1 -6 3a6 6 0 0 0 12 0C6 -1 0 -8 0 -8Z")

/** "You're in" (canvas "Logged in"): the badge fills with oil, a tick draws, drops burst out. */
@Composable
fun LoggedInScreen(firstName: String, onDone: () -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    val clock = remember { Animatable(if (reduceMotion) TOTAL_MS else 0f) }
    val surface by rememberLoop(1500)
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        if (reduceMotion) delay(1200) else clock.animateTo(TOTAL_MS, tween(TOTAL_MS.toInt(), easing = LinearEasing))
        onDone()
    }
    LaunchedEffect(Unit) {
        delay(if (reduceMotion) 0L else 1500L)
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    val t = clock.value
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(260.dp)) {
            viewBox(260f, 260f) {
                val ring = kf(t, 1450f to 0f, 1550f to 1f, 2500f to 2f)
                if (ring in 0.01f..1.99f) {
                    val grow = if (ring <= 1f) 0.8f + 0.02f * ring else 0.82f + 0.53f * OilEase.transform(ring - 1f)
                    val alpha = if (ring <= 1f) 0.9f * ring else 0.9f * (1f - OilEase.transform(ring - 1f))
                    dot(130f, 130f, 118.5f * grow, stroke = St.Gold, strokeWidth = 3f, alpha = alpha)
                }
                val burst = kf(t, 1500f to 0f, 1650f to 1f, 2600f to 2f)
                if (burst in 0.01f..1.99f) {
                    val out = if (burst <= 1f) 96f * OilEase.transform(burst) else 96f + 54f * OilEase.transform(burst - 1f)
                    val scale = if (burst <= 1f) 0.6f + 0.4f * burst else 1f - 0.3f * (burst - 1f)
                    val alpha = if (burst <= 1f) burst else 1f - OilEase.transform(burst - 1f)
                    repeat(8) { i ->
                        rotate(45f * i, Offset(130f, 130f)) {
                            translate(130f, 130f - out) {
                                val s = scale * if (i % 2 == 0) 1f else 0.85f
                                around(0f, 0f, sx = s) {
                                    fill(BurstDrop, if (i % 2 == 0) St.Gold else St.Green, alpha = alpha)
                                }
                            }
                        }
                    }
                }
                // The badge: a sand well that fills with oil, then the tick.
                val badge = Path().apply { addOval(Rect(Offset(130f, 130f), 86f)) }
                clipPath(badge) {
                    drawRect(St.Sand, Offset(44f, 44f), Size(172f, 172f))
                    val level = seg(t, 0f, 1500f, Pour)
                    val top = 44f + 172f * (1f - level)
                    drawRect(St.Gold, Offset(44f, top), Size(172f, 172f))
                    translate(44f - 176f * surface, top - 16f) { fill(Wave, St.Gold) }
                }
                dot(130f, 130f, 86f, stroke = St.GreenDeep, strokeWidth = 4f)
                val tick = seg(t, 1400f, 2000f)
                if (tick > 0f) {
                    translate(46f, 46f) { line(partOf(Check, tick), St.GreenDeep, 12f) }
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.graphicsLayer {
                val a = seg(t, 1550f, 2100f)
                alpha = a
                translationY = 14.dp.toPx() * (1f - a)
            },
        ) {
            Text(
                stringResource(R.string.loggedin_title, firstName),
                style = MaterialTheme.typography.headlineLarge,
                color = St.Ink,
                textAlign = TextAlign.Center,
            )
            Text(stringResource(R.string.loggedin_body), style = MaterialTheme.typography.bodyLarge, color = St.Ink2, textAlign = TextAlign.Center)
            val progress = seg(t, 1900f, 3100f, LinearEasing)
            val loading = stringResource(R.string.loggedin_loading)
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(168.dp, 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(St.Track)
                    .semantics {
                        contentDescription = loading
                        progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                    },
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(3.dp)).background(St.Green))
            }
        }
    }
}

private fun partOf(path: Path, fraction: Float): Path {
    val measure = PathMeasure().apply { setPath(path, false) }
    return Path().also { measure.getSegment(0f, measure.length * fraction, it, true) }
}
