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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.signup.LiveMap
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

private val LocationBlue = Color(0xFF1A73E8)

/**
 * "Waiting for orders near you": a still street map centred on the partner, with the
 * Google-Maps-style blue dot breathing a halo (orders come by this position).
 */
@Composable
fun NearYouMap(center: Pair<Double, Double>?, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val pulse by rememberLoop(2000)
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
        Canvas(Modifier.fillMaxSize()) {
            val dot = 8.dp.toPx()
            if (!reduce) {
                // Two halos, half a beat apart, spreading out and fading like a radar ping.
                listOf(pulse, (pulse + 0.5f) % 1f).forEach { p ->
                    drawCircle(LocationBlue.copy(alpha = 0.35f * (1f - p)), radius = dot + 30.dp.toPx() * p, center = this.center)
                }
            } else {
                drawCircle(LocationBlue.copy(alpha = 0.18f), radius = dot * 2.6f, center = this.center)
            }
            drawCircle(Color.Black.copy(alpha = 0.18f), radius = dot + 3.5.dp.toPx(), center = this.center.copy(y = this.center.y + 1.dp.toPx()))
            drawCircle(Color.White, radius = dot + 3.dp.toPx(), center = this.center)
            drawCircle(LocationBlue, radius = dot, center = this.center)
        }
    }
}
