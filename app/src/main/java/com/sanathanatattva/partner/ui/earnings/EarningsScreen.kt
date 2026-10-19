package com.sanathanatattva.partner.ui.earnings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.EarningDay
import com.sanathanatattva.partner.data.api.Earnings
import com.sanathanatattva.partner.ui.components.DockContentClearance
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.rupees
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Earnings (canvas "Earnings"): last 7 days, a bar per day, what's been paid out. */
@Composable
fun EarningsScreen(onPayout: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    var data by remember { mutableStateOf(container.partner.lastEarnings) }
    var bank by remember { mutableStateOf(data?.bank?.ifsc?.let { container.banks.cached(it)?.substringBefore(" · ") }) }
    LaunchedEffect(Unit) {
        (container.partner.earnings() as? ApiResult.Success)?.value?.let { e ->
            data = e
            e.bank?.ifsc?.let { ifsc -> bank = container.banks.describe(ifsc)?.substringBefore(" · ") }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().background(St.Cream)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp + DockContentClearance),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.earnings_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                    color = St.Ink,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(St.Beige).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.earnings_last7), style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = St.Ink)
                }
            }
            val e = data
            Column(Modifier.padding(start = 4.dp, top = 10.dp)) {
                // CSS "line-height: 1": the box is exactly 52dp tall. Anek's tall Kannada metrics
                // would otherwise leave a big gap under Latin digits.
                Box(Modifier.height(52.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        rupees(e?.total ?: 0.0),
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 52.sp, lineHeight = 52.sp, fontWeight = FontWeight.ExtraBold),
                        color = St.Green,
                        modifier = Modifier.wrapContentHeight(unbounded = true),
                    )
                }
                if (e != null) {
                    Text(
                        pluralStringResource(R.plurals.earnings_summary, e.deliveries, e.deliveries, range(e.from, e.to)),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = St.Ink3,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (e != null && e.days.isNotEmpty()) {
                Chart(e.days, Modifier.padding(top = 14.dp))
                PaidRow(e, bank, onPayout, Modifier.padding(top = 12.dp))
                DayList(e.days.takeLast(3).reversed(), Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
private fun Chart(days: List<EarningDay>, modifier: Modifier) {
    var sel by remember(days) { mutableIntStateOf(days.lastIndex) }
    val max = days.maxOf { it.amount }
    // The canvas scales the tallest day to ~83% of the plot and rules a line at a round amount under it.
    val scale = if (max > 0) max * 1.2 else 1.0
    val grid = if (max > 0) niceFloor(max) else null
    val s = days[sel]
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(St.Beige)
            .border(1.5.dp, St.BeigeLine, RoundedCornerShape(24.dp)).padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(fullDay(s.date), style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = St.Ink3)
                Text(dayCount(s.deliveries), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = St.Ink2)
            }
            Text(rupees(s.amount), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink)
        }
        Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(150.dp)) {
            if (grid != null) {
                val frac = (grid / scale).toFloat() * (140f / 150f)
                Canvas(Modifier.fillMaxSize()) {
                    val y = size.height * (1f - frac)
                    drawLine(St.ChartGrid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
                }
                Text(
                    rupees(grid),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = St.ChartLabel,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = (150 * frac + 3).dp),
                )
            }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEachIndexed { i, d ->
                    val desc = stringResource(R.string.earnings_bar_desc, fullDay(d.date), dayCount(d.deliveries), rupees(d.amount))
                    Bar(
                        index = i,
                        fraction = (d.amount / scale).toFloat() * (140f / 150f),
                        on = i == sel,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .selectable(selected = i == sel, role = Role.Tab, onClick = { sel = i })
                            .semantics { contentDescription = desc },
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.5.dp, color = St.ChartAxis)
        Row(Modifier.padding(top = 6.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { i, d ->
                Text(
                    shortDay(d.date),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = if (i == sel) FontWeight.ExtraBold else FontWeight.SemiBold),
                    color = if (i == sel) St.Ink else St.Ink2,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** "er-bar": each bar grows up from the axis, 60ms after the one before. */
@Composable
private fun Bar(index: Int, fraction: Float, on: Boolean, modifier: Modifier) {
    val reduce = LocalReduceMotion.current
    val grow = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        grow.animateTo(1f, tween(800, easing = OilEase))
    }
    val color by animateColorAsState(if (on) St.Green else St.ChartBar, tween(if (reduce) 0 else 250), label = "bar")
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier.widthIn(max = 30.dp).fillMaxWidth()
                .heightIn(min = 3.dp).fillMaxHeight(fraction.coerceIn(0f, 1f))
                .graphicsLayer { scaleY = grow.value; transformOrigin = TransformOrigin(0.5f, 1f) }
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(color),
        )
    }
}

@Composable
private fun PaidRow(e: Earnings, bank: String?, onPayout: () -> Unit, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(St.GreenTint).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(StIcons.Bank, contentDescription = null, tint = St.Green, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            val tail = e.bank?.last4
            Text(
                if (tail != null) stringResource(R.string.earnings_sent_to, rupees(e.paid), bank ?: stringResource(R.string.earnings_your_bank), tail)
                else stringResource(R.string.earnings_sent, rupees(e.paid)),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = St.Green,
            )
            if (e.pending > 0) Text(stringResource(R.string.earnings_on_the_way, rupees(e.pending)), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = St.Ink3)
        }
        Text(
            stringResource(R.string.earnings_details),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = St.Green,
            modifier = Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onPayout).padding(horizontal = 6.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun DayList(days: List<EarningDay>, modifier: Modifier) {
    Column(modifier) {
        days.forEachIndexed { i, d ->
            val today = i == 0 && d.date == LocalDate.now().toString()
            Row(Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (today) stringResource(R.string.earnings_today, mediumDay(d.date)) else mediumDay(d.date),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = if (today) FontWeight.Bold else FontWeight.SemiBold),
                    color = St.Ink,
                    modifier = Modifier.weight(1f),
                )
                Text(dayCount(d.deliveries), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = St.Ink2)
                Text(
                    rupees(d.amount),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
                    color = St.Ink,
                    modifier = Modifier.width(72.dp),
                )
            }
            if (i < days.lastIndex) HorizontalDivider(thickness = 1.5.dp, color = St.RowLine)
        }
    }
}

@Composable
private fun dayCount(n: Int) = if (n == 0) stringResource(R.string.earnings_day_off) else pluralStringResource(R.plurals.earnings_deliveries, n, n)

private fun locale(): Locale = Locale.getDefault()

@Composable
private fun shortDay(date: String): String {
    val d = LocalDate.parse(date)
    return if (d == LocalDate.now()) stringResource(R.string.earnings_today_short) else d.dayOfWeek.getDisplayName(TextStyle.SHORT, locale())
}

@Composable
private fun fullDay(date: String): String {
    val d = LocalDate.parse(date)
    val text = d.format(DateTimeFormatter.ofPattern("EEE d MMM", locale()))
    return if (d == LocalDate.now()) stringResource(R.string.earnings_today, text) else text
}

private fun mediumDay(date: String): String = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEE d", locale()))

/** "13–19 Oct", or "28 Sep – 4 Oct" across months. */
private fun range(from: String, to: String): String {
    if (from.isBlank() || to.isBlank()) return ""
    val a = LocalDate.parse(from)
    val b = LocalDate.parse(to)
    val month = DateTimeFormatter.ofPattern("MMM", locale())
    return if (a.month == b.month) "${a.dayOfMonth}–${b.dayOfMonth} ${b.format(month)}"
    else "${a.dayOfMonth} ${a.format(month)} – ${b.dayOfMonth} ${b.format(month)}"
}

/** The largest 1/2/5 × 10ⁿ at or below [v] (225 → 200). */
private fun niceFloor(v: Double): Double {
    val p = 10.0.pow(floor(log10(v)))
    return listOf(5.0, 2.0, 1.0).map { it * p }.first { it <= v }
}
