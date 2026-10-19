package com.sanathanatattva.partner.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.DayHours
import com.sanathanatattva.partner.ui.components.BackBar
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.login.UiText
import com.sanathanatattva.partner.ui.login.text
import com.sanathanatattva.partner.ui.login.toUiText
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val MIN = 360   // 6 AM
private const val MAX = 1320  // 10 PM

private enum class SaveState { Idle, Saving, Saved, Failed }

/** Delivery hours (canvas "Delivery hours"): a week strip to pick a day, and an editor for that day. Saves as you go. */
@Composable
fun DeliveryHoursScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = context.appContainer.partner
    val haptics = LocalHapticFeedback.current
    var days by remember { mutableStateOf<List<DayHours>?>(null) }
    var sel by remember { mutableIntStateOf(todayIndex()) }
    var edits by remember { mutableIntStateOf(0) }
    var save by remember { mutableStateOf(SaveState.Idle) }
    var error by remember { mutableStateOf<UiText?>(null) }
    LaunchedEffect(Unit) {
        (repo.hours() as? ApiResult.Success)?.value?.let { h ->
            days = h.days
            if (h.saved) save = SaveState.Saved
        }
    }
    // Every change saves after a short pause, so tapping + a few times sends one request.
    LaunchedEffect(edits) {
        if (edits == 0) return@LaunchedEffect
        save = SaveState.Saving
        delay(700)
        val current = days ?: return@LaunchedEffect
        when (val r = repo.saveHours(current)) {
            is ApiResult.Success -> { save = SaveState.Saved; error = null; haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) }
            is ApiResult.Failure -> { save = SaveState.Failed; error = r.problem.toUiText() }
        }
    }
    fun update(i: Int, change: (DayHours) -> DayHours) {
        days = days?.mapIndexed { j, d -> if (j == i) change(d) else d }
        edits++
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 22.dp)) {
        BackBar(stringResource(R.string.profile_delivery_hours), onBack, trailing = { SavedChip(save) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.hours_title), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp), color = St.Ink, modifier = Modifier.padding(start = 4.dp, top = 8.dp).semantics { heading() })
            Text(stringResource(R.string.hours_body), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), color = St.Ink2, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            val d = days
            if (d != null) {
                Week(d, sel, onPick = { sel = it }, modifier = Modifier.padding(top = 10.dp))
                DayEditor(
                    day = d[sel],
                    index = sel,
                    onChange = { change -> update(sel, change) },
                    onCopy = {
                        val targets = if (sel < 5) 0..4 else 5..6
                        days = d.mapIndexed { j, x -> if (j in targets) d[sel].copy(weekday = j) else x }
                        edits++
                    },
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            error?.let { Text(text(it), style = MaterialTheme.typography.bodySmall, color = St.Brick, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) }
            Text(stringResource(R.string.hours_note), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = St.Ink2, modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp))
        }
    }
}

@Composable
private fun SavedChip(state: SaveState) {
    AnimatedVisibility(state == SaveState.Saved || state == SaveState.Saving, enter = fadeIn(), exit = fadeOut()) {
        Row(
            Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(St.GreenTint).padding(horizontal = 10.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (state == SaveState.Saved) Icon(StIcons.Check, contentDescription = null, tint = St.Green, modifier = Modifier.size(13.dp))
            Text(
                stringResource(if (state == SaveState.Saved) R.string.hours_saved else R.string.hours_saving),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = St.Green,
            )
        }
    }
}

@Composable
private fun Week(days: List<DayHours>, sel: Int, onPick: (Int) -> Unit, modifier: Modifier) {
    val today = todayIndex()
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(St.Beige)
            .border(1.5.dp, St.BeigeLine, RoundedCornerShape(22.dp)).padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 8.dp),
    ) {
        // Hour ruler over the bars: 6 AM · 10 · 2 PM · 6 · 10 PM.
        BoxWithConstraints(Modifier.padding(start = 50.dp, end = 6.dp).fillMaxWidth().height(18.dp)) {
            val w = maxWidth
            listOf(0f to hourLabel(6, true), 0.25f to "10", 0.5f to hourLabel(14, true), 0.75f to "6", 1f to hourLabel(22, true)).forEach { (f, label) ->
                val shift = when (f) { 0f -> 0.3f; 1f -> 0.7f; else -> 0.5f }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = St.ChartLabel,
                    maxLines = 1,
                    modifier = Modifier.offset(x = w * f).graphicsLayer { translationX = -size.width * shift },
                )
            }
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            days.forEachIndexed { i, d ->
                val bg by animateColorAsState(if (i == sel) St.Cream else St.Beige, tween(250), label = "row")
                val desc = dayName(i) + ", " + if (d.enabled) stringResource(R.string.hours_range, clock(d.start), clock(d.end)) else stringResource(R.string.hours_closed_word)
                Row(
                    Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(10.dp)).background(bg)
                        .selectable(selected = i == sel, role = Role.RadioButton) { onPick(i) }
                        .semantics(mergeDescendants = true) { contentDescription = desc }
                        .padding(start = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        shortDay(i),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = if (i == sel) FontWeight.ExtraBold else FontWeight.SemiBold),
                        color = St.Ink,
                        maxLines = 1,
                        modifier = Modifier.width(36.dp),
                    )
                    Track(d, now = if (i == today) nowMinutes() else null, modifier = Modifier.weight(1f).padding(end = 6.dp))
                }
            }
        }
    }
}

/** One day's bar on the 6 AM–10 PM track; "dh-bar" glides when the times change. */
@Composable
private fun Track(d: DayHours, now: Int?, modifier: Modifier) {
    val reduce = LocalReduceMotion.current
    BoxWithConstraints(modifier.height(16.dp)) {
        val w = maxWidth
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(St.Cream)) {
            Canvas(Modifier.fillMaxSize()) {
                listOf(0.25f, 0.5f, 0.75f).forEach { f ->
                    drawLine(St.TrackDash, Offset(size.width * f, 0f), Offset(size.width * f, size.height), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())))
                }
            }
            if (d.enabled) {
                val spec = tween<androidx.compose.ui.unit.Dp>(if (reduce) 0 else 350, easing = OilEase)
                val left by animateDpAsState(w * ((d.start - MIN) / (MAX - MIN).toFloat()), spec, label = "l")
                val width by animateDpAsState(w * ((d.end - d.start) / (MAX - MIN).toFloat()), spec, label = "w")
                Box(Modifier.offset(x = left).width(width).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(St.Green))
            } else {
                Text(
                    stringResource(R.string.hours_closed_caps),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.06.em),
                    color = St.ChartLabel,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        if (now != null && now in MIN..MAX) {
            // "dh-now": a gold tick at the current time, breathing slowly.
            val alpha = if (reduce) 1f else rememberInfiniteTransition(label = "now").animateFloat(
                1f, 0.45f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "a",
            ).value
            Box(
                Modifier.offset(x = w * ((now - MIN) / (MAX - MIN).toFloat()) - 1.5.dp, y = (-5).dp)
                    .width(3.dp).height(26.dp).graphicsLayer { this.alpha = alpha }
                    .clip(RoundedCornerShape(2.dp)).background(St.Gold),
            )
        }
    }
}

@Composable
private fun DayEditor(day: DayHours, index: Int, onChange: ((DayHours) -> DayHours) -> Unit, onCopy: () -> Unit, modifier: Modifier) {
    val name = dayName(index) + if (index == todayIndex()) stringResource(R.string.hours_today_suffix) else ""
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(St.Beige)
            .border(2.dp, St.Green, RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink, modifier = Modifier.weight(1f).semantics { heading() })
            OpenSwitch(day.enabled) { on -> onChange { it.copy(enabled = on) } }
        }
        AnimatedVisibility(day.enabled, enter = expandVertically(tween(300, easing = OilEase)) + fadeIn(), exit = shrinkVertically(tween(250)) + fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper(
                    label = stringResource(R.string.hours_opens),
                    value = clock(day.start),
                    lessDesc = stringResource(R.string.hours_open_earlier),
                    moreDesc = stringResource(R.string.hours_open_later),
                    onLess = { onChange { it.copy(start = (it.start - 30).coerceAtLeast(MIN)) } },
                    onMore = { onChange { it.copy(start = (it.start + 30).coerceAtMost(it.end - 60)) } },
                )
                Stepper(
                    label = stringResource(R.string.hours_closes),
                    value = clock(day.end),
                    lessDesc = stringResource(R.string.hours_close_earlier),
                    moreDesc = stringResource(R.string.hours_close_later),
                    onLess = { onChange { it.copy(end = (it.end - 30).coerceAtLeast(it.start + 60)) } },
                    onMore = { onChange { it.copy(end = (it.end + 30).coerceAtMost(MAX)) } },
                )
            }
        }
        Row(
            Modifier.offset(x = (-6).dp).heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onCopy).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(StIcons.Copy, contentDescription = null, tint = St.Green, modifier = Modifier.size(20.dp))
            Text(
                stringResource(if (index < 5) R.string.hours_copy_weekdays else R.string.hours_copy_weekend),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = St.Green,
            )
        }
    }
}

/** The canvas's Open/Closed switch: 52×32 track, knob glides 250ms. */
@Composable
private fun OpenSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    val reduce = LocalReduceMotion.current
    val knob by animateDpAsState(if (on) 24.dp else 4.dp, tween(if (reduce) 0 else 250, easing = OilEase), label = "k")
    val track by animateColorAsState(if (on) St.Green else St.Cream, tween(if (reduce) 0 else 250), label = "t")
    val edge by animateColorAsState(if (on) St.Green else St.FieldBorder, tween(if (reduce) 0 else 250), label = "e")
    Row(
        Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp))
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(if (on) R.string.hours_open_word else R.string.hours_closed_word),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = St.Ink3,
        )
        Box(Modifier.size(width = 52.dp, height = 32.dp).clip(RoundedCornerShape(16.dp)).background(track).border(2.dp, edge, RoundedCornerShape(16.dp))) {
            Box(Modifier.offset(x = knob, y = 4.dp).size(20.dp).clip(CircleShape).background(if (on) St.Cream else St.FieldBorder))
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, lessDesc: String, moreDesc: String, onLess: () -> Unit, onMore: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp)).background(St.Cream).padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.Ink3, modifier = Modifier.weight(1f))
        StepButton("−", lessDesc, onLess)
        Text(
            value,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
            color = St.Ink,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 96.dp).padding(horizontal = 6.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
        StepButton("+", moreDesc, onMore)
    }
}

@Composable
private fun StepButton(symbol: String, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).border(2.dp, St.Green, CircleShape)
            .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold), color = St.Green)
    }
}

private fun todayIndex() = LocalDate.now().dayOfWeek.value - 1

private fun nowMinutes() = LocalTime.now().let { it.hour * 60 + it.minute }

private fun dayName(i: Int) = DayOfWeek.of(i + 1).getDisplayName(TextStyle.FULL, Locale.getDefault())

private fun shortDay(i: Int) = DayOfWeek.of(i + 1).getDisplayName(TextStyle.SHORT, Locale.getDefault())

/** 480 → "8:00 AM". */
private fun clock(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

private fun hourLabel(hour: Int, @Suppress("SameParameterValue") withPeriod: Boolean): String =
    LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern(if (withPeriod) "h a" else "h", Locale.getDefault()))
