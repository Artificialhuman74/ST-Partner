package com.sanathanatattva.partner.ui.order

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.data.api.OrderLine
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.dashedBorder
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St

/* ── Text bits ─────────────────────────────────────────────────────────── */

/** "2 × 5 L" */
fun OrderLine.qtyText(): String {
    val l = litres
    val size = when {
        l != null && l % 1.0 == 0.0 -> "${l.toInt()} L"
        l != null -> "$l L"
        else -> size.orEmpty()
    }
    return if (size.isBlank()) "$quantity ×" else "$quantity × $size"
}

/** "Groundnut oil" → "Groundnut" for the compact summaries, as on the canvas. */
fun OrderLine.shortName(): String = name.replace(Regex("(?i)\\s+oil$"), "")

@Composable
fun OrderHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 34.sp),
        color = St.Ink,
        modifier = modifier.padding(start = 4.dp, end = 4.dp).semantics { heading() },
    )
}

@Composable
fun OrderBody(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp), color = St.Ink2, modifier = modifier.padding(horizontal = 4.dp))
}

/* ── Header chips ──────────────────────────────────────────────────────── */

@Composable
fun GreenChip(text: String, tick: Boolean = false) {
    Row(
        Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(St.GreenTint).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (tick) Icon(StIcons.Check, contentDescription = null, tint = St.Green, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold), color = St.Green, maxLines = 1)
    }
}

@Composable
fun BeigeChip(text: String) {
    Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(St.Beige).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = St.Ink, maxLines = 1)
    }
}

/* ── Steps ─────────────────────────────────────────────────────────────── */

/**
 * The three-step bar. Steps before [current] are done (green, ticked label); the current one
 * is bold. [goldNow] paints the current step gold with "oc-now"'s pulsing dot (still packing).
 */
@Composable
fun Steps(labels: List<String>, current: Int, modifier: Modifier = Modifier, goldNow: Boolean = false, tickDone: Boolean = true) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, label ->
            val done = i < current
            val now = i == current
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (now && goldNow) St.Gold else if (done || now) St.Green else St.Track),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (now && goldNow) NowDot()
                    if (done && tickDone) Icon(StIcons.Check, contentDescription = null, tint = St.Green, modifier = Modifier.size(14.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = if (now) FontWeight.ExtraBold else if (done) FontWeight.Bold else FontWeight.SemiBold,
                        ),
                        color = if (now) St.Ink else if (done) St.Green else St.Ink2,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** "oc-now": a gold dot breathing an 8dp halo every 1.8 s. */
@Composable
private fun NowDot() {
    val reduce = LocalReduceMotion.current
    val t by rememberLoop(1800)
    Box(
        Modifier.size(9.dp).drawBehind {
            if (!reduce) {
                val p = OilEase.transform((t / 0.75f).coerceAtMost(1f))
                drawCircle(St.Gold.copy(alpha = 0.6f * (1f - p)), radius = size.minDimension / 2 + 8.dp.toPx() * p)
            }
        }.clip(CircleShape).background(St.Gold),
    )
}

/* ── What's in the order ───────────────────────────────────────────────── */

/** The dashed "what you carry" panel with a solid REFILL ONLY / NEW CAN chip per line. */
@Composable
fun LinesSummary(lines: List<OrderLine>, modifier: Modifier = Modifier, title: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(St.Beige).dashedBorder(2.5.dp, St.Green, 22.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (title != null || trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title.orEmpty(), style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = St.Ink2, modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
        }
        lines.forEach { l ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    buildQty(l),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                    color = St.Ink,
                    modifier = Modifier.weight(1f),
                )
                CanChip(l.lineType)
            }
        }
    }
}

@Composable
private fun buildQty(l: OrderLine) = androidx.compose.ui.text.buildAnnotatedString {
    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.ExtraBold))
    append(l.qtyText())
    pop()
    append(" ")
    append(l.shortName())
}

@Composable
fun CanChip(lineType: String) {
    if (lineType != "refill" && lineType != "new") return
    val refill = lineType == "refill"
    Row(
        Modifier.height(32.dp).clip(RoundedCornerShape(10.dp)).background(if (refill) St.Gold else St.Green).padding(start = 8.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(if (refill) StIcons.CanNoNew else StIcons.CanNew, contentDescription = null, tint = if (refill) St.Ink else St.Cream, modifier = Modifier.size(18.dp))
        Text(
            stringResource(if (refill) R.string.stamp_refill else R.string.chip_new_can),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.02.em),
            color = if (refill) St.Ink else St.Cream,
        )
    }
}

/* ── People and places ─────────────────────────────────────────────────── */

/** Name + full address with two outlined actions underneath (Call · Google Maps). */
@Composable
fun AddressCard(
    name: String,
    address: String,
    modifier: Modifier = Modifier,
    callLabel: String?,
    onCall: (() -> Unit)?,
    mapsLabel: String,
    onMaps: () -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(St.Beige).border(1.5.dp, St.BeigeLine, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column {
            Text(name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = St.Ink)
            Text(address, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp), color = St.Ink3)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (callLabel != null && onCall != null) OutlineAction(callLabel, StIcons.Phone, Modifier.weight(1f), onCall)
            OutlineAction(mapsLabel, StIcons.Map, Modifier.weight(1f), onMaps)
        }
    }
}

@Composable
fun OutlineAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(50.dp).clip(RoundedCornerShape(16.dp)).background(St.Cream).border(2.dp, St.Green, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = St.Green, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Green, maxLines = 1)
    }
}

/** Indian number plate: white, ink border, the blue IND strip. */
@Composable
fun PlateBadge(plate: String, big: Boolean = true) {
    Row(
        Modifier.height(if (big) 34.dp else 30.dp).clip(RoundedCornerShape(6.dp)).background(Color.White).border(2.5.dp, St.Ink, RoundedCornerShape(6.dp))
            .padding(end = if (big) 10.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (big) 8.dp else 6.dp),
    ) {
        Box(Modifier.width(if (big) 18.dp else 14.dp).fillMaxHeight().background(Color(0xFF1F4FA3)))
        Text(
            spacedPlate(plate),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = if (big) 17.sp else 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.06.em),
            color = St.Ink,
            maxLines = 1,
        )
    }
}

/** "KA05MX2481" → "KA 05 MX 2481". */
fun spacedPlate(plate: String): String {
    val p = plate.uppercase().replace(" ", "")
    val m = Regex("^([A-Z]{2})(\\d{1,2})([A-Z]{0,3})(\\d{1,4})$").find(p) ?: return plate
    return m.groupValues.drop(1).filter { it.isNotEmpty() }.joinToString(" ")
}

/** The rider's face (or the canvas's placeholder silhouette) in a green ring. */
@Composable
fun Avatar(photo: ImageBitmap?, size: Dp, ring: Dp = 3.dp, description: String? = null) {
    Box(
        Modifier.size(size).clip(CircleShape).border(ring, St.Green, CircleShape).padding(ring).clip(CircleShape).background(St.GreenMist)
            .semantics { if (description != null) contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Icon(StIcons.Person, contentDescription = null, tint = Color(0xFF5F7D5A), modifier = Modifier.size(size * 0.6f))
        }
    }
}

/* ── Codes ─────────────────────────────────────────────────────────────── */

/** Entry boxes with a blinking caret ("rc-caret"/"hc-caret"); all green once complete. */
@Composable
fun CodeBoxes(code: String, length: Int, modifier: Modifier = Modifier, height: Dp = 72.dp, gap: Dp = 12.dp, font: Int = 36) {
    val done = code.length == length
    val reduce = LocalReduceMotion.current
    val blink by rememberLoop(1000)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(length) { i ->
            val filled = i < code.length
            val cur = !done && i == code.length
            val border by animateColorAsState(
                when {
                    done || cur -> St.Green
                    filled -> St.Line
                    else -> St.BeigeLine
                },
                tween(250), label = "b",
            )
            val bg by animateColorAsState(if (done) St.GreenTint else if (filled) St.Beige else St.Paper, tween(250), label = "bg")
            Box(
                Modifier.weight(1f).height(height).clip(RoundedCornerShape(16.dp)).background(bg).border(2.5.dp, border, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (filled) {
                    Text(code[i].toString(), style = MaterialTheme.typography.displaySmall.copy(fontSize = font.sp, fontWeight = FontWeight.ExtraBold), color = if (done) St.Green else St.Ink)
                } else if (cur && (reduce || blink < 0.5f)) {
                    Box(Modifier.width(3.dp).height(height * 0.46f).clip(RoundedCornerShape(2.dp)).background(St.Green))
                }
            }
        }
    }
}

/** A code the rider shows: big digits on cream tiles. */
@Composable
fun ShowCode(code: String, faded: Boolean, tile: Dp = 66.dp, height: Dp = 84.dp, font: Int = 52, label: String) {
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = label + " " + code.toList().joinToString(" ") },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        code.forEach { d ->
            Box(Modifier.size(width = tile, height = height).clip(RoundedCornerShape(16.dp)).background(St.Paper), contentAlignment = Alignment.Center) {
                Text(d.toString(), style = MaterialTheme.typography.displayMedium.copy(fontSize = font.sp, fontWeight = FontWeight.ExtraBold), color = if (faded) Color(0xFF6F8A72) else St.Ink)
            }
        }
    }
}

/** 3×4 number pad: digits, Clear, 0, delete. A light tick on every press. */
@Composable
fun NumPad(onDigit: (Char) -> Unit, onClear: () -> Unit, onDelete: () -> Unit, keyHeight: Dp = 58.dp) {
    val haptics = LocalHapticFeedback.current
    val clear = stringResource(R.string.pad_clear)
    val delete = stringResource(R.string.pad_delete)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d -> Key(d.toString(), St.Beige, keyHeight, Modifier.weight(1f)) { haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap); onDigit(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Key(clear, St.Cream, keyHeight, Modifier.weight(1f), small = true) { haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap); onClear() }
            Key("0", St.Beige, keyHeight, Modifier.weight(1f)) { haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap); onDigit('0') }
            Box(
                Modifier.weight(1f).height(keyHeight).clip(RoundedCornerShape(16.dp)).background(St.Cream)
                    .clickable(role = Role.Button, onClick = { haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap); onDelete() })
                    .semantics { contentDescription = delete },
                contentAlignment = Alignment.Center,
            ) { Icon(StIcons.Backspace, contentDescription = null, tint = St.Ink, modifier = Modifier.size(28.dp)) }
        }
    }
}

@Composable
private fun Key(label: String, bg: Color, height: Dp, modifier: Modifier, small: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier.height(height).clip(RoundedCornerShape(16.dp)).background(bg).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.headlineSmall.copy(fontSize = if (small) 18.sp else 30.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink)
    }
}

/** "pc-dot": three dots bobbing 0.2 s apart while we wait for the other side. */
@Composable
fun WaitingDots() {
    val reduce = LocalReduceMotion.current
    val t by rememberLoop(1400)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(0f, 0.2f / 1.4f, 0.4f / 1.4f).forEach { offset ->
            val p = ((t - offset) % 1f + 1f) % 1f
            val k = if (reduce) 1f else if (p < 0.4f) p / 0.4f else (1f - p) / 0.6f
            Box(
                Modifier.size(6.dp).graphicsLayer { alpha = 0.25f + 0.75f * k; translationY = -3.dp.toPx() * k }
                    .clip(CircleShape).background(St.Green),
            )
        }
    }
}

/* ── The big action ────────────────────────────────────────────────────── */

/**
 * The 64dp action at the foot of every order screen. Disabled = the canvas's sand look.
 * [done] swaps the label for a tick (with a confirm buzz) once the server has said yes.
 */
@Composable
fun OrderAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
    loading: Boolean = false,
    done: Boolean = false,
    color: Color = St.Green,
) {
    val bg by animateColorAsState(if (enabled || loading || done) color else Color(0xFFE3DAC8), tween(400, easing = OilEase), label = "bg")
    val ink by animateColorAsState(if (enabled || loading || done) St.Cream else Color(0xFF5F5A4C), tween(400, easing = OilEase), label = "ink")
    Row(
        modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(20.dp)).background(bg)
            .clickable(enabled = enabled && !loading && !done, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            done -> {
                val pop = remember { Animatable(0f) }
                LaunchedEffect(Unit) { pop.animateTo(1f, tween(420, easing = OilEase)) }
                Icon(StIcons.Check, contentDescription = stringResource(R.string.saved), tint = ink, modifier = Modifier.size(30.dp).graphicsLayer { scaleX = 0.4f + 0.6f * pop.value; scaleY = scaleX; alpha = pop.value })
            }
            loading -> CircularProgressIndicator(color = ink, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
            else -> {
                if (leading != null) Icon(leading, contentDescription = null, tint = ink, modifier = Modifier.size(24.dp))
                Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 21.sp), color = ink, maxLines = 2, textAlign = TextAlign.Center)
                if (trailing != null) Icon(trailing, contentDescription = null, tint = ink, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun TextAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = St.Green)
    }
}

/** A soft green note row (e.g. "Paid online. Don't take any cash."). */
@Composable
fun GreenNote(text: String, icon: ImageVector = StIcons.CircleCheck, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).background(St.GreenWash).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = St.Green, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.Green)
    }
}

@Composable
fun BeigePanel(modifier: Modifier = Modifier, dashed: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(St.Beige)
            .then(if (dashed) Modifier.dashedBorder(2.5.dp, St.Green, 22.dp) else Modifier.border(1.5.dp, St.BeigeLine, shape))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/* ── Leaving the app ───────────────────────────────────────────────────── */

/** Turn-by-turn is Google Maps' job (handoff §10); falls back to the browser if it isn't installed. */
fun openNavigation(context: Context, lat: Double, lng: Double) {
    val app = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng&mode=d")).setPackage("com.google.android.apps.maps")
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving"))
    runCatching { context.startActivity(app) }.onFailure { runCatching { context.startActivity(web) } }
}

/** Opens the dialler with the number filled in; the partner taps call. */
fun dial(context: Context, phone: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}"))) }
}
