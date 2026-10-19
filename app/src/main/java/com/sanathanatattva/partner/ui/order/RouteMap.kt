package com.sanathanatattva.partner.ui.order

import android.graphics.PointF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.data.orders.Routing
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.fill
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.signup.LiveMap
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

typealias Spot = Pair<Double, Double>

enum class Pin { Shop, Customer, Rider, Me }

/**
 * A live OpenStreetMap map, painted like Google Maps (handoff §10): the real road route from OSRM
 * as a blue line, the destination ([to] as a shop or customer pin) and whoever is moving ([mover]:
 * the rider for a shop, "me" for the person driving). Still: no panning, it frames itself.
 */
@Composable
fun RouteMap(
    to: Spot,
    toPin: Pin,
    mover: Spot?,
    moverPin: Pin,
    modifier: Modifier = Modifier,
    from: Spot? = null,
    label: String? = null,
) {
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var frame by remember { mutableIntStateOf(0) } // bumps when the camera settles, to re-place the markers
    val start = mover ?: from ?: to
    LaunchedEffect(map, start, to) {
        val m = map ?: return@LaunchedEffect
        val route = Routing.route(start, to)
        val line = route?.points ?: listOf(start, to)
        m.style?.let { style ->
            val feature = Feature.fromGeometry(LineString.fromLngLats(line.map { Point.fromLngLat(it.second, it.first) }))
            val source = style.getSourceAs<GeoJsonSource>("route") ?: GeoJsonSource("route").also { style.addSource(it) }
            source.setGeoJson(feature)
            if (style.getLayer("route-line") == null) {
                // Google-style route: a solid blue line on a darker blue edge.
                style.addLayer(
                    LineLayer("route-casing", "route").withProperties(
                        PropertyFactory.lineColor(RouteEdge.toArgb()),
                        PropertyFactory.lineWidth(8.5f),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    ),
                )
                style.addLayer(
                    LineLayer("route-line", "route").withProperties(
                        PropertyFactory.lineColor(RouteBlue.toArgb()),
                        PropertyFactory.lineWidth(5.5f),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    ),
                )
            }
        }
        val bounds = LatLngBounds.Builder().apply {
            line.forEach { include(LatLng(it.first, it.second)) }
            include(LatLng(to.first, to.second))
            mover?.let { include(LatLng(it.first, it.second)) }
        }.build()
        runCatching { m.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 70, 60, 70, 60), 600) }
    }

    Box(modifier.clip(RoundedCornerShape(24.dp)).background(St.Beige).border(1.5.dp, St.BeigeLine, RoundedCornerShape(24.dp))) {
        LiveMap(
            start = LatLng(start.first, start.second),
            onReady = { m ->
                m.uiSettings.setAllGesturesEnabled(false)
                m.uiSettings.isLogoEnabled = false
                m.addOnCameraIdleListener { frame++ }
                m.addOnCameraMoveListener { frame++ }
                map = m
            },
            onMoveStarted = {},
            onIdle = {},
            modifier = Modifier.fillMaxSize(),
        )
        val m = map
        // Keyed on the camera frame so markers re-place as the map moves under them.
        if (m != null) key(frame) {
            fun place(s: Spot): PointF? = runCatching { m.projection.toScreenLocation(LatLng(s.first, s.second)) }.getOrNull()
            from?.let { s -> place(s)?.let { p -> Marker(p) { StartDot() } } }
            place(to)?.let { p -> Marker(p, anchorBottom = toPin == Pin.Customer) { if (toPin == Pin.Shop) ShopMarker() else CustomerPin() } }
            mover?.let { s -> place(s)?.let { p -> GlidingMarker(p) { if (moverPin == Pin.Rider) RiderMarker() else MeDot() } } }
        }
        if (label != null) {
            Box(
                Modifier.align(Alignment.BottomStart).padding(12.dp).height(34.dp).clip(RoundedCornerShape(17.dp)).background(St.Cream).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink) }
        }
    }
}

/** Places a marker centred on a screen point (or with its tip on it, for pins). */
@Composable
private fun Marker(at: PointF, anchorBottom: Boolean = false, content: @Composable () -> Unit) {
    Box(Modifier.offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }.layoutCentered(anchorBottom)) { content() }
}

/** The mover slides to each new fix instead of jumping (the canvas's "rc-rider" glide). */
@Composable
private fun GlidingMarker(at: PointF, content: @Composable () -> Unit) {
    val reduce = LocalReduceMotion.current
    val x = remember { Animatable(at.x) }
    val y = remember { Animatable(at.y) }
    LaunchedEffect(at.x, at.y) {
        if (reduce) { x.snapTo(at.x); y.snapTo(at.y); return@LaunchedEffect }
        coroutineScope {
            launch { x.animateTo(at.x, tween(900, easing = OilEase)) }
            launch { y.animateTo(at.y, tween(900, easing = OilEase)) }
        }
    }
    Marker(PointF(x.value, y.value), content = content)
}

private fun Modifier.layoutCentered(anchorBottom: Boolean) = this.then(
    Modifier.layout { measurable, constraints ->
        val p = measurable.measure(constraints)
        layout(0, 0) { p.place(-p.width / 2, if (anchorBottom) -p.height else -p.height / 2) }
    },
)

/** "rc-shop": the shop's green disc sends out a soft ring every 2 s. */
@Composable
private fun ShopMarker() {
    val reduce = LocalReduceMotion.current
    val t by rememberLoop(2000)
    Box(
        Modifier.size(36.dp).drawBehind {
            if (!reduce) {
                val p = OilEase.transform((t / 0.75f).coerceAtMost(1f))
                drawCircle(St.Green.copy(alpha = 0.45f * (1f - p)), radius = size.minDimension / 2 + 12.dp.toPx() * p)
            }
        }.clip(CircleShape).background(St.Green),
        contentAlignment = Alignment.Center,
    ) { Icon(StIcons.Shop, contentDescription = null, tint = St.Cream, modifier = Modifier.size(20.dp)) }
}

private val PinPath = svgPath("M14 34s-14-13-14-24a14 14 0 0 1 28 0c0 11-14 24-14 24z")

// Google Maps' route blue, destination red and "you are here" dot.
private val RouteBlue = Color(0xFF4285F4)
private val RouteEdge = Color(0xFF1967D2)
private val PinRed = Color(0xFFEA4335)
private val PinRedDark = Color(0xFFB31412)

@Composable
private fun CustomerPin() {
    Canvas(Modifier.size(width = 28.dp, height = 34.dp)) {
        viewBox(28f, 34f) {
            fill(PinPath, PinRed)
            drawCircle(PinRedDark, 5.5f, Offset(14f, 10f))
        }
    }
}

@Composable
private fun RiderMarker() {
    Box(
        Modifier.size(34.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(St.Cream).padding(3.dp).clip(CircleShape).background(St.Gold),
        contentAlignment = Alignment.Center,
    ) { Icon(StIcons.Scooter, contentDescription = null, tint = St.Ink, modifier = Modifier.size(18.dp)) }
}

/** The blue "you are here" dot: white ring, soft blue halo. */
@Composable
private fun MeDot() {
    Box(Modifier.size(34.dp).clip(CircleShape).background(RouteBlue.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        Box(Modifier.size(18.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White).padding(3.dp).clip(CircleShape).background(RouteBlue))
    }
}

@Composable
private fun StartDot() {
    Box(Modifier.size(18.dp).clip(CircleShape).background(St.Cream).padding(3.dp).clip(CircleShape).background(St.Green))
}
