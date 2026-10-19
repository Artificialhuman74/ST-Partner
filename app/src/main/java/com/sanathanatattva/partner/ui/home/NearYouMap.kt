package com.sanathanatattva.partner.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.EaseInOut
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.delayed
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.svgPath
import androidx.compose.ui.unit.dp
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.signup.LiveMap
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

private val DropPath = svgPath("M15 1C15 1 3 17 3 26a12 12 0 0 0 24 0C27 17 15 1 15 1Z")

/**
 * "Waiting for orders near you" (canvas "HomeOnline"): a still street map centred on the
 * partner, with the canvas's oil drop and its spreading rings marking where orders come by.
 */
@Composable
fun NearYouMap(center: Pair<Double, Double>?, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val ripple by rememberLoop(4500)
    val bobClock by rememberLoop(3000)
    // Until the first fix arrives the dot pulses on plain sand, then the streets fade in under it.
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(St.MapLand)) {
        if (center != null) {
            val target = LatLng(center.first, center.second)
            // Follow the phone as fresh fixes arrive; a glide, not a jump.
            LaunchedEffect(map, center) { map?.animateCamera(CameraUpdateFactory.newLatLng(target), 800) }
            LiveMap(
                start = target,
                onReady = { m ->
                    // A picture of where you are, not something to pan: every gesture off.
                    m.uiSettings.setAllGesturesEnabled(false)
                    m.uiSettings.isAttributionEnabled = true
                    m.uiSettings.isLogoEnabled = false
                    m.moveCamera(CameraUpdateFactory.zoomTo(15.0))
                    map = m
                },
                onMoveStarted = {},
                onIdle = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Canvas "hn-rip" ×3 + "hn-drop": oil rings spreading from a bobbing gold drop where you are.
        Canvas(Modifier.fillMaxSize()) {
            val mid = this.center
            val ringW = 260.dp.toPx()
            val ringH = 130.dp.toPx()
            val stroke = Stroke(2.dp.toPx())
            if (!reduce) {
                // 4.5 s each, 1.5 s apart: scale .15 → 1, opacity 0 → .7 (at 8%) → 0, eased like oil.
                listOf(0, 1500, 3000).forEach { delayMs ->
                    val t = ripple.delayed(delayMs, 4500)
                    val scale = kf(t, 0f to 0.15f, 1f to 1f, easing = OilEase)
                    val alpha = kf(t, 0f to 0f, 0.08f to 0.7f, 1f to 0f, easing = OilEase)
                    drawOval(
                        St.Green.copy(alpha = alpha),
                        topLeft = Offset(mid.x - ringW * scale / 2, mid.y - ringH * scale / 2),
                        size = Size(ringW * scale, ringH * scale),
                        style = stroke,
                    )
                }
            } else {
                // Reduced motion: the canvas's resting frame (one faint ring, one at 60%).
                drawOval(St.Green.copy(alpha = 0.3f), topLeft = Offset(mid.x - ringW / 2, mid.y - ringH / 2), size = Size(ringW, ringH), style = stroke)
                drawOval(St.Green.copy(alpha = 0.3f), topLeft = Offset(mid.x - ringW * 0.3f, mid.y - ringH * 0.3f), size = Size(ringW * 0.6f, ringH * 0.6f), style = stroke)
            }
            // The drop: 34×46 on a 30×40 box, its tip 38 dp above centre, bobbing 5 dp over 3 s.
            val bob = if (reduce) 0f else -5.dp.toPx() * kf(bobClock, 0f to 0f, 0.5f to 1f, 1f to 0f, easing = EaseInOut)
            translate(mid.x - 17.dp.toPx(), mid.y - 38.dp.toPx() + bob) {
                scale(34.dp.toPx() / 30f, 46.dp.toPx() / 40f, pivot = Offset.Zero) {
                    drawPath(DropPath, St.Gold)
                    drawOval(St.GoldGlint.copy(alpha = 0.85f), topLeft = Offset(10f - 2.6f, 26f - 5f), size = Size(5.2f, 10f))
                }
            }
        }
    }
}
