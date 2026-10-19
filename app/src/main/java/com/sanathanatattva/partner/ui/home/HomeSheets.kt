package com.sanathanatattva.partner.ui.home

import android.location.Location
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.data.api.Place
import com.sanathanatattva.partner.data.partner.StartPoint
import com.sanathanatattva.partner.ui.components.HideDock
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.PrimaryButton
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.line
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.components.around
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St

/** A bottom sheet that rises 60px into place ("bk-sheet" / "ao-sheet"). */
@Composable
private fun RisingSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    HideDock()
    val reduce = LocalReduceMotion.current
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(700, easing = OilEase)) }
    Box(Modifier.fillMaxSize().background(St.Ink.copy(alpha = 0.45f * enter.value)).clickable(onClick = onDismiss, indication = null, interactionSource = null)) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = 60.dp.toPx() * (1f - enter.value)
                    alpha = 0.6f + 0.4f * enter.value
                }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(St.Cream)
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 44.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(St.Line))
            content()
        }
    }
}

private sealed interface Choice {
    data class Saved(val id: Long) : Choice
    data object Here : Choice
}

/** "Where are you starting from?" (canvas "Go online · pick start place"). */
@Composable
fun StartPlacePicker(
    places: List<Place>,
    here: Pair<Double, Double>?,
    busy: Boolean,
    onClose: () -> Unit,
    onPick: (StartPoint) -> Unit,
) {
    val near = places.firstOrNull { here != null && metres(here, it) < 300 }
    var chosenId by rememberSaveable { mutableStateOf((near ?: places.firstOrNull { it.isDefault } ?: places.firstOrNull())?.id ?: -1L) }
    val choice: Choice = if (chosenId == -1L) Choice.Here else Choice.Saved(chosenId)

    RisingSheet(onClose) {
        Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.base_title), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp), color = St.Ink, modifier = Modifier.padding(top = 14.dp).semantics { heading() })
            Text(stringResource(R.string.base_body), style = MaterialTheme.typography.bodyMedium, color = St.Ink2, modifier = Modifier.padding(top = 4.dp))
            Column(Modifier.padding(top = 16.dp).selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                places.forEach { p ->
                    val title = stringResource(
                        when (p.label) { "shop" -> R.string.label_shop; "other" -> R.string.label_other; else -> R.string.label_home },
                    )
                    val km = here?.let { "%.1f".format(metres(it, p) / 1000.0) }
                    Option(
                        icon = if (p.label == "shop") StIcons.Shop else if (p.label == "other") StIcons.Pin else StIcons.Home,
                        title = title,
                        place = p.address.substringBefore(", Bengaluru"),
                        badge = if (p == near) stringResource(R.string.base_you_are_here) else km?.let { stringResource(R.string.base_km_away, it) },
                        badgeHere = p == near,
                        selected = choice == Choice.Saved(p.id),
                        gps = false,
                        onClick = { chosenId = p.id },
                    )
                }
                Option(
                    icon = StIcons.Locate,
                    title = stringResource(R.string.base_here_title),
                    place = stringResource(R.string.base_here_body),
                    badge = null,
                    badgeHere = false,
                    selected = choice == Choice.Here,
                    gps = true,
                    onClick = { chosenId = -1L },
                )
            }
            Text(stringResource(R.string.base_note), style = MaterialTheme.typography.bodySmall, color = St.Ink2, modifier = Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(16.dp))
        }
        val chosenPlace = places.firstOrNull { choice == Choice.Saved(it.id) }
        PrimaryButton(
            text = if (chosenPlace != null) stringResource(R.string.base_go_from, stringResource(
                when (chosenPlace.label) { "shop" -> R.string.label_shop; "other" -> R.string.label_other; else -> R.string.label_home },
            )) else stringResource(R.string.base_go_here),
            loading = busy,
            enabled = chosenPlace != null || here != null,
            onClick = {
                if (chosenPlace != null) onPick(StartPoint.Saved(chosenPlace))
                else here?.let { onPick(StartPoint.Here(it.first, it.second)) }
            },
        )
    }
}

private fun metres(a: Pair<Double, Double>, p: Place): Float {
    val out = FloatArray(1)
    Location.distanceBetween(a.first, a.second, p.latitude, p.longitude, out)
    return out[0]
}

@Composable
private fun Option(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    place: String,
    badge: String?,
    badgeHere: Boolean,
    selected: Boolean,
    gps: Boolean,
    onClick: () -> Unit,
) {
    // "bk-opt": colours ease over 0.3s; "bk-dot": the chosen dot pops in.
    val border by animateColorAsState(if (selected) St.Green else St.LineSoft, tween(300, easing = OilEase), label = "b")
    val bg by animateColorAsState(if (selected) St.GreenWash else St.Paper, tween(300, easing = OilEase), label = "bg")
    val tile by animateColorAsState(if (selected) Color(0xFFDCEBD7) else St.Sand, tween(300, easing = OilEase), label = "t")
    val shape = RoundedCornerShape(22.dp)
    val gpsClock by rememberLoop(2000, still = 1f)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).clip(shape).background(bg).border(2.dp, border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(46.dp)
                .drawBehind {
                    // "bk-gps": a slow pulse round the live-location tile.
                    if (gps) {
                        val p = (gpsClock / 0.8f).coerceAtMost(1f)
                        drawRoundRect(St.Green.copy(alpha = 0.45f * (1f - p)), topLeft = androidx.compose.ui.geometry.Offset(-8.dp.toPx() * p, -8.dp.toPx() * p),
                            size = androidx.compose.ui.geometry.Size(size.width + 16.dp.toPx() * p, size.height + 16.dp.toPx() * p),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx() + 8.dp.toPx() * p))
                    }
                }
                .clip(RoundedCornerShape(14.dp)).background(tile),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = St.Green, modifier = Modifier.size(24.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = St.Ink)
            Text(place, style = MaterialTheme.typography.bodySmall, color = St.Ink2, maxLines = 2)
            if (badge != null) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                    color = if (badgeHere) St.Green else St.Ink2,
                    modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(10.dp)).background(if (badgeHere) St.GreenTint else St.Sand).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Box(Modifier.size(26.dp).border(2.5.dp, if (selected) St.Green else St.FieldBorder, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) {
                val pop = remember { Animatable(0f) }
                LaunchedEffect(Unit) { pop.animateTo(1f, tween(450, easing = OilEase)) }
                Box(Modifier.size(14.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.clip(CircleShape).background(St.Green))
            }
        }
    }
}

private val Bell = svgPath("M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z")
private val Clapper = svgPath("M10 20.5a2 2 0 0 0 4 0")
private val MoonPath = svgPath("M19 14.5A7.5 7.5 0 0 1 9.5 5a7.5 7.5 0 1 0 9.5 9.5z")
private val BellSwing = CubicBezierEasing(0.36f, 0f, 0.64f, 1f)

/** "We've set you offline" after 3 missed offers: three bells ring and fade in turn, then a moon. */
@Composable
fun AutoOfflineSheet(onGoOnline: () -> Unit, onNotNow: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val t by rememberLoop(5400, still = 0.8f)
    RisingSheet(onNotNow) {
        Canvas(Modifier.padding(top = 18.dp).fillMaxWidth().height(70.dp)) {
            viewBox(240f, 70f) {
                for (i in 0..2) {
                    // "ao-b1..3": swing ±12° then dim to 35%, each 18% of the loop after the last.
                    val s = 0.18f * i
                    val swing = if (reduce) 0f else kf(t, s to 0f, s + 0.03f to -12f, s + 0.06f to 12f, s + 0.09f to -6f, s + 0.12f to 0f, easing = BellSwing)
                    val a = if (reduce) 0.35f else kf(t, 0f to 1f, s + 0.12f to 1f, s + 0.18f to 0.35f, 0.92f to 0.35f, 1f to 1f)
                    val x = 30f + i * 50f
                    around(x + 12f, 12f, rotate = swing) {
                        translate(x, 10f) {
                            line(Bell, St.Brick, 2.2f, alpha = a)
                            line(Clapper, St.Brick, 2.2f, alpha = a)
                        }
                    }
                }
                // "ao-moon": comes in at 56–66%, rotating from -30°.
                val m = if (reduce) 1f else kf(t, 0.56f to 0f, 0.66f to 1f, 0.92f to 1f, 1f to 0f, easing = OilEase)
                if (m > 0f) around(194f, 22f, rotate = -30f * (1f - m), sx = 0.4f + 0.6f * m) {
                    translate(182f, 10f) { line(MoonPath, St.Green, 2.2f, alpha = m) }
                }
            }
        }
        Text(stringResource(R.string.home_auto_offline_title), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp), color = St.Ink, modifier = Modifier.padding(top = 12.dp).semantics { heading() })
        Text(stringResource(R.string.home_auto_offline_body), style = MaterialTheme.typography.bodyLarge, color = St.Ink2, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = stringResource(R.string.home_go_online), onClick = onGoOnline)
        TextButton(onClick = onNotNow, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) {
            Text(stringResource(R.string.close), style = MaterialTheme.typography.titleMedium, color = St.Green)
        }
    }
}
