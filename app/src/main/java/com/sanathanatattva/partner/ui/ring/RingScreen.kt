package com.sanathanatattva.partner.ui.ring

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.alerts.LineKind
import com.sanathanatattva.partner.alerts.Offer
import com.sanathanatattva.partner.alerts.OfferLine
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.SlideButton
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.seg
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.VoiceBars
import com.sanathanatattva.partner.ui.components.box
import com.sanathanatattva.partner.ui.components.label
import com.sanathanatattva.partner.ui.components.line
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.theme.AnekNarrow
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.delay
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Full-screen ring for a new order (canvas "Ringing · trader" / "Ringing · rider"). */
@Composable
fun RingScreen(offer: Offer, totalMs: Long, onHearAgain: () -> Unit, onAccept: () -> Unit, onDecline: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val left = (offer.expiresAt - now).coerceAtLeast(0)
    val mm = left / 60_000
    val ss = (left / 1000) % 60

    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
    ) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PulseDot()
            Text(
                stringResource(if (offer.forRider) R.string.ring_title_rider else R.string.ring_title_trader),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = St.Ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            OutlinedButton(
                onClick = onHearAgain,
                border = BorderStroke(2.dp, St.Green),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = St.GreenWash, contentColor = St.Green),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                VoiceBars(Modifier.size(40.dp, 22.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ring_hear_again), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
        Column(Modifier.padding(horizontal = 4.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "%d:%02d".format(mm, ss),
                    style = MaterialTheme.typography.displayMedium.copy(fontFamily = AnekNarrow, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold),
                    color = St.Ink,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Text(stringResource(R.string.ring_left), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp), color = St.Ink2, modifier = Modifier.padding(bottom = 6.dp))
            }
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(St.Track)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((left.toFloat() / totalMs).coerceIn(0f, 1f)).clip(RoundedCornerShape(3.dp)).background(St.Green))
            }
            if (!offer.forRider) {
                Text(stringResource(R.string.ring_trader_note), style = MaterialTheme.typography.bodySmall, color = St.Ink2, modifier = Modifier.padding(top = 8.dp))
            }
            if (offer.test) {
                Text(
                    stringResource(R.string.ring_test_note),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = St.GoldInk,
                    modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(St.GoldGlint).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        // "rg-card": rises 20px and fades in over 0.7s.
        val reduce = LocalReduceMotion.current
        val enter = remember { Animatable(if (reduce) 1f else 0f) }
        LaunchedEffect(Unit) { enter.animateTo(1f, tween(1500, easing = LinearEasing)) }
        val t = enter.value * 1500f
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.graphicsLayer {
                val p = seg(t, 0f, 700f)
                alpha = p
                translationY = 20.dp.toPx() * (1f - p)
            }) { OrderCard(offer, t) }
        }

        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SlideButton(label = stringResource(R.string.ring_slide_accept), onComplete = onAccept)
            val haptics = LocalHapticFeedback.current
            OutlinedButton(
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.Reject); onDecline() },
                border = BorderStroke(2.dp, St.Green),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = St.Green),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Text(
                    stringResource(if (offer.forRider) R.string.ring_skip else R.string.ring_cant_deliver),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

@Composable
private fun OrderCard(offer: Offer, t: Float) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(St.Beige)
            .drawBehind {
                drawRoundRect(
                    St.Green, cornerRadius = CornerRadius(28.dp.toPx()),
                    style = Stroke(2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))),
                )
            }
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
    ) {
        if (offer.forRider) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ring_what_you_carry), style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp), color = St.Ink2, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.ring_load, offer.cans, offer.totalLitres),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
                    color = St.Ink,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(St.Cream).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
        }
        offer.lines.forEachIndexed { i, line ->
            if (i > 0) {
                Box(
                    Modifier.padding(vertical = 10.dp).fillMaxWidth().height(2.dp).drawBehind {
                        drawLine(St.LineSoft, center.copy(x = 0f), center.copy(x = size.width), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                    },
                )
            }
            Line(line)
            // "cn-stamp": 1.1x → 1x with a fade, first at 0.55s, the next at 0.75s.
            val start = if (i == 0) 550f else 750f
            Box(Modifier.graphicsLayer {
                val p = seg(t, start, start + 900f)
                alpha = p
                scaleX = 1.1f - 0.1f * p
                scaleY = scaleX
            }) { Stamp(line.kind, offer.forRider) }
        }
        Box(Modifier.padding(vertical = 12.dp).fillMaxWidth().height(1.5.dp).background(St.BeigeLine))
        if (offer.forRider) {
            Route(offer)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.ring_you_earn), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = St.Ink3, modifier = Modifier.weight(1f))
                Text(offer.money, style = MaterialTheme.typography.displayMedium.copy(fontFamily = AnekNarrow, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold), color = St.Green)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(St.Cream), contentAlignment = Alignment.Center) {
                    Icon(StIcons.Person, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(offer.customer, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = St.Ink)
                    Text(stringResource(R.string.ring_area_km, offer.area, offer.distanceKm), style = MaterialTheme.typography.bodySmall, color = St.Ink2)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.ring_commission), style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = St.Ink2)
                    Text(offer.money, style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold), color = St.Green)
                }
            }
        }
    }
}

@Composable
private fun Line(line: OfferLine) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Cans(line, Modifier.size(width = 62.dp, height = 62.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${line.qty} × ${line.litres} L",
                style = MaterialTheme.typography.displayMedium.copy(fontFamily = AnekNarrow, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold),
                color = St.Ink,
            )
            Text(line.oil, style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight(650)), color = St.Ink)
        }
    }
}

private val CanHandle = svgPath("M19 13q9-10 18 0")

/** Steel cans from the canvas: one, or two stacked when the line has more than one. */
@Composable
private fun Cans(line: OfferLine, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        viewBox(76f, 78f) {
            fun can(dx: Float, dy: Float, back: Boolean) {
                translate(dx, dy) {
                    line(CanHandle, St.Ink, 2.5f)
                    box(8f, 12f, 40f, 50f, 7f, fill = if (back) St.SteelDark else St.Steel, stroke = St.Ink, strokeWidth = 2f)
                    box(8f, 12f, 40f, 8f, 4f, fill = if (back) Color(0xFFAEB7BA) else St.SteelDark, stroke = St.Ink, strokeWidth = 2f)
                    if (!back) {
                        box(13f, 22f, 4f, 34f, 2f, fill = Color.White, alpha = 0.8f)
                        box(20f, 28f, 24f, 22f, 3f, fill = St.Green)
                        label(measurer, "${line.litres}L", 32f, 43.5f, if (line.litres.length > 2) 10.5f else 11.5f, St.Cream, weight = FontWeight.ExtraBold)
                    }
                }
            }
            if (line.qty > 1) {
                can(20f, 0f, back = true)
                can(0f, 14f, back = false)
            } else {
                can(10f, 8f, back = false)
            }
        }
    }
}

/** REFILL ONLY (solid gold) / PACK A NEW CAN (solid green): never missable (handoff §10). */
@Composable
private fun Stamp(kind: LineKind, forRider: Boolean) {
    if (kind == LineKind.Standard) return
    val refill = kind == LineKind.Refill
    val (title, body) = when {
        refill && forRider -> R.string.stamp_refill to R.string.stamp_refill_rider
        refill -> R.string.stamp_refill to R.string.stamp_refill_trader
        forRider -> R.string.stamp_new_rider to R.string.stamp_new_body
        else -> R.string.stamp_new_trader to R.string.stamp_new_body
    }
    val bg = if (refill) St.Gold else St.Green
    val ink = if (refill) St.Ink else St.Cream
    Row(
        Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(if (refill) StIcons.CanNoNew else StIcons.CanNew, contentDescription = null, tint = ink, modifier = Modifier.size(30.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.03.sp), color = ink)
            Text(stringResource(body), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = if (refill) ink else St.OnGreenMuted)
        }
    }
}

@Composable
private fun Route(offer: Offer) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.padding(top = 5.dp).width(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(St.Cream).border(4.dp, St.Green, CircleShape))
            Box(Modifier.width(3.dp).height(28.dp).drawBehind {
                drawLine(St.FieldBorder, center.copy(y = 0f), center.copy(y = size.height), 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 5f)))
            })
            Icon(StIcons.Pin, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                Text(stringResource(R.string.ring_pickup_km, offer.pickupKm.orEmpty()), style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = St.Ink2)
                Text(offer.pickupName.orEmpty(), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Ink)
            }
            Column {
                Text(stringResource(R.string.ring_deliver_km, offer.distanceKm), style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = St.Ink2)
                Text(offer.area, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Ink)
            }
        }
    }
}

/** "rg-dot": a green dot with a ring that spreads 10dp and fades, every 1.4s. */
@Composable
private fun PulseDot() {
    val clock by rememberLoop(1400, still = 1f)
    Box(
        Modifier.size(12.dp).drawBehind {
            val p = OilEase.transform((clock / 0.75f).coerceAtMost(1f))
            drawCircle(St.Green.copy(alpha = 0.5f * (1f - p)), radius = size.minDimension / 2f + 10.dp.toPx() * p)
        }.clip(CircleShape).background(St.Green),
    )
}
