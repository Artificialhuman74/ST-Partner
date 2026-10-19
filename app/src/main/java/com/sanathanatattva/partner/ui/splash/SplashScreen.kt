package com.sanathanatattva.partner.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.ui.components.Emblem
import com.sanathanatattva.partner.ui.components.FallEase
import com.sanathanatattva.partner.ui.components.around
import com.sanathanatattva.partner.ui.components.dot
import com.sanathanatattva.partner.ui.components.fill
import com.sanathanatattva.partner.ui.components.seg
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.nav.startRoute
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private const val TOTAL_MS = 2600f
private val Drop = svgPath("M15 1C15 1 3 17 3 26a12 12 0 0 0 24 0C27 17 15 1 15 1Z")
private val DropGlint = svgPath("M10 21a2.6 5 0 1 0 0.01 0Z")

/** Splash (canvas "Main"): played once, then hands off to wherever this person should land. */
@Composable
fun SplashScreen(onDone: (Any) -> Unit) {
    val context = LocalContext.current
    val reduceMotion = LocalReduceMotion.current
    val clock = remember { Animatable(if (reduceMotion) TOTAL_MS else 0f) }

    LaunchedEffect(Unit) {
        val container = context.appContainer
        val route = coroutineScope {
            val decision = async { startRoute(container.settings.current(), container.sessions.current()) }
            if (reduceMotion) delay(900) else clock.animateTo(TOTAL_MS, tween(TOTAL_MS.toInt(), easing = LinearEasing))
            decision.await()
        }
        onDone(route)
    }

    val t = clock.value
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            // Ripples where the drop lands, then the gold ring drawing round.
            Canvas(Modifier.size(240.dp)) {
                viewBox(240f, 240f) {
                    val r1 = seg(t, 540f, 1300f)
                    if (t in 540f..1300f) dot(120f, 120f, 100f * (0.12f + 1.73f * r1), stroke = St.Gold, strokeWidth = 2f, alpha = 0.95f * (1f - r1))
                    val r2 = seg(t, 680f, 1600f)
                    if (t in 680f..1600f) dot(120f, 120f, 100f * (0.12f + 2.38f * r2), stroke = St.Green, strokeWidth = 2f, alpha = 0.55f * (1f - r2))
                    val ring = seg(t, 820f, 1800f)
                    if (ring > 0f) drawArc(St.Gold, -90f, 360f * ring, false, Offset(19f, 19f), Size(202f, 202f), style = Stroke(3f, cap = StrokeCap.Round))
                }
            }
            val reveal = seg(t, 560f, 1300f)
            // Soft lift under the emblem (the canvas's 0 18px 36px shadow), fading in with it.
            Canvas(Modifier.size(240.dp)) {
                if (reveal > 0f) drawCircle(
                    Brush.radialGradient(
                        listOf(St.GreenDeep.copy(alpha = 0.30f * reveal), Color.Transparent),
                        center = center.copy(y = center.y + 14.dp.toPx()),
                        radius = 104.dp.toPx(),
                    ),
                    radius = 104.dp.toPx(),
                    center = center.copy(y = center.y + 14.dp.toPx()),
                )
            }
            Emblem(
                size = 180.dp,
                describe = true,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = 0.9f + 0.1f * reveal
                        scaleY = scaleX
                        alpha = if (reveal > 0f) 1f else 0f
                    }
                    .drawWithContent {
                        val circle = Path().apply {
                            addOval(Rect(center, size.minDimension / 2f * reveal))
                        }
                        clipPath(circle) { this@drawWithContent.drawContent() }
                    },
            )
            // The falling drop: stretched while falling, squashed flat on impact.
            if (t < 640f) {
                Canvas(Modifier.size(240.dp)) {
                    viewBox(240f, 240f) {
                        val fall = seg(t, 0f, 520f, FallEase)
                        val impact = seg(t, 520f, 640f)
                        val y = -440f * (1f - fall) + 8f * impact
                        val sx = if (t < 520f) 0.86f + 0.06f * fall else 0.92f + 0.58f * impact
                        val sy = if (t < 520f) 1.22f - 0.10f * fall else 1.12f - 0.87f * impact
                        translate(105f, 80f + y) {
                            around(15f, 40f, sx = sx, sy = sy) {
                                fill(Drop, St.Gold, alpha = 1f - impact)
                                fill(DropGlint, St.GoldGlint, alpha = 0.85f * (1f - impact))
                            }
                        }
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val word = seg(t, 1250f, 1700f)
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = St.GreenDeep,
                modifier = Modifier.graphicsLayer { alpha = word; translationY = 18.dp.toPx() * (1f - word) },
            )
            val tag = seg(t, 1500f, 1950f)
            Text(
                stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight(450)),
                color = St.Ink2,
                modifier = Modifier.graphicsLayer { alpha = tag; translationY = 12.dp.toPx() * (1f - tag) },
            )
        }
    }
}
