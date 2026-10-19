package com.sanathanatattva.partner.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.PrimaryButton
import com.sanathanatattva.partner.ui.components.around
import com.sanathanatattva.partner.ui.components.box
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

private object Art {
    val ray = svgPath("M175 200L165 20h20z")
    val glass = svgPath("M161 140h28v18q0 7 7 11l18 9q18 9 18 28v66q0 22-22 22h-70q-22 0-22-22v-66q0-19 18-28l18-9q7-4 7-11z")
    val wave = svgPath("M110 166q12-6 24 0t24 0t24 0t24 0t24 0t24 0t24 0t24 0V200H110z")
    val tick = svgPath("M160 239l10 10 20-22")
    val bigDrop = svgPath("M0 0c0 0-5 7-5 11a5 5 0 0 0 10 0C5 7 0 0 0 0z")
    val smallDrop = svgPath("M0 0c0 0-4 6-4 9a4 4 0 0 0 8 0C4 6 0 0 0 0z")
    /** x, width, height, period ms, delay ms, round(drop) or leaf (canvas values). */
    val confetti = listOf(
        floatArrayOf(28f, 9f, 12f, 4600f, -400f, 1f), floatArrayOf(64f, 8f, 14f, 5400f, -2800f, 0f),
        floatArrayOf(102f, 7f, 10f, 4200f, -1600f, 1f), floatArrayOf(140f, 8f, 13f, 5800f, -4100f, 0f),
        floatArrayOf(182f, 9f, 12f, 4900f, -3300f, 1f), floatArrayOf(220f, 8f, 14f, 5100f, -900f, 0f),
        floatArrayOf(258f, 7f, 10f, 4400f, -2200f, 1f), floatArrayOf(296f, 8f, 13f, 5600f, -3900f, 0f),
        floatArrayOf(334f, 9f, 12f, 4700f, -1200f, 1f), floatArrayOf(360f, 8f, 14f, 5300f, -4600f, 0f),
    )
}

/** "You're approved!" (canvas "Approved"): the one moment that gets a real celebration. */
@Composable
fun ApprovedScreen(firstName: String, onContinue: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val intro = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1600, easing = LinearEasing)) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(if (reduce) 0L else 480L)
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    val spin by rememberLoop(48_000)
    val wave by rememberLoop(3200)
    val fall by rememberLoop(60_000)
    val description = stringResource(R.string.approved_title)

    Box(Modifier.fillMaxSize()) {
        // Oil drops and leaves drifting down behind everything.
        if (!reduce) {
            Canvas(Modifier.fillMaxWidth().height(520.dp)) {
                viewBox(390f, 520f) {
                    val ms = fall * 60_000f
                    Art.confetti.forEach { c ->
                        val (x, w, h, period, delay) = c
                        val round = c[5]
                        val p = (((ms - delay) % period) + period) % period / period
                        val a = kf(p, 0f to 0f, 0.08f to 1f, 0.85f to 1f, 1f to 0f)
                        val y = -30f + 500f * p
                        val color = when ((x.toInt() / 38) % 4) { 0 -> St.Gold; 1 -> St.Green; 2 -> St.GoldDeep; else -> St.GreenDone }
                        around(x + w / 2, y + h / 2, rotate = 300f * p) {
                            if (round == 1f) drawOval(color, Offset(x, y), Size(w, h), alpha = a)
                            else drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(w, h), alpha = a)
                        }
                    }
                }
            }
        }
        val words = Modifier.graphicsLayer {
            val a = OilEase.transform(((intro.value * 1600f - 200f) / 1000f).coerceIn(0f, 1f))
            alpha = a
            translationY = 16.dp.toPx() * (1f - a)
        }
        // Button at the bottom when there's room (canvas: margin-top auto); scrolls on short phones.
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        val artHeight = if (maxHeight < 700.dp) 280.dp else 380.dp
        Column(
            Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.fillMaxWidth().height(artHeight).semantics { contentDescription = description }) {
                viewBox(350f, 380f) {
                    val t = intro.value * 1600f
                    rotate(360f * spin, Offset(175f, 200f)) {
                        repeat(12) { i -> rotate(30f * i, Offset(175f, 200f)) { fill(Art.ray, St.GoldGlint) } }
                    }
                    dot(175f, 200f, 118f, fill = St.Sand)
                    // Drops leaping out of the can's mouth.
                    val spray = seg(t, 220f, 1100f)
                    if (spray in 0.01f..0.99f) {
                        listOf(-50f, -25f, 0f, 25f, 50f).forEachIndexed { i, angle ->
                            rotate(angle, Offset(175f, 136f)) {
                                val rise = 40f + 80f * spray
                                translate(175f, 136f - rise) {
                                    fill(if (i % 2 == 0) Art.bigDrop else Art.smallDrop, if (i % 2 == 0) St.Gold else St.Green, alpha = 1f - spray)
                                }
                            }
                        }
                    }
                    clipPath(Art.glass) {
                        box(110f, 138f, 130f, 160f, fill = St.Gold)
                        box(110f, 138f, 130f, 26f, fill = St.Paper)
                        translate(-48f * wave, 0f) { fill(Art.wave, St.Gold) }
                        box(132f, 184f, 11f, 84f, 5.5f, fill = Color.White, alpha = 0.45f)
                    }
                    line(Art.glass, St.Ink, 3.5f)
                    box(157f, 130f, 36f, 12f, 4f, fill = St.Paper, stroke = St.Ink, strokeWidth = 3.5f)
                    val badge = seg(t, 160f, 560f)
                    if (badge > 0f) around(175f, 238f, sx = 0.3f + 0.7f * badge) {
                        dot(175f, 238f, 34f, fill = St.Green, stroke = St.Cream, strokeWidth = 5f, alpha = badge)
                        val tick = seg(t, 290f, 520f)
                        if (tick > 0f) line(partial(Art.tick, tick), St.Cream, 7f)
                    }
                }
            }
            Text(
                stringResource(R.string.approved_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp),
                textAlign = TextAlign.Center,
                color = St.Ink,
                modifier = words.padding(top = 12.dp).semantics { heading() },
            )
            Text(
                stringResource(R.string.approved_body, firstName),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = St.Ink2,
                modifier = words.padding(top = 14.dp),
            )
        }
            PrimaryButton(text = stringResource(R.string.approved_cta), onClick = onContinue, modifier = words.padding(top = 24.dp))
        }
        }
    }
}

private fun partial(path: Path, fraction: Float): Path {
    val measure = PathMeasure().apply { setPath(path, false) }
    return Path().also { measure.getSegment(0f, measure.length * fraction, it, true) }
}
