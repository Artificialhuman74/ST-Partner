package com.sanathanatattva.partner.ui.shop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.CustomerOrder
import com.sanathanatattva.partner.data.shop.Ist
import com.sanathanatattva.partner.data.shop.Restock
import com.sanathanatattva.partner.data.shop.Stock
import com.sanathanatattva.partner.data.shop.SubShop
import com.sanathanatattva.partner.data.shop.areaOf
import com.sanathanatattva.partner.data.shop.dayMonth
import com.sanathanatattva.partner.data.shop.litresOf
import com.sanathanatattva.partner.data.shop.needsAttention
import com.sanathanatattva.partner.data.shop.paidOut
import com.sanathanatattva.partner.data.shop.parseServerTime
import com.sanathanatattva.partner.data.shop.shortName
import com.sanathanatattva.partner.data.shop.shortOrderNumber
import com.sanathanatattva.partner.data.shop.sizeLabel
import com.sanathanatattva.partner.data.shop.stock
import com.sanathanatattva.partner.data.shop.weekOf
import com.sanathanatattva.partner.data.shop.weeklyTotals
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.rupees
import com.sanathanatattva.partner.ui.theme.St
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ── Orders ───────────────────────────────────────────────────────────────────

/** Orders (canvas "Orders"): stock asked for, on a 3-step track, and customers' orders. */
@Composable
fun ShopOrdersScreen(onBack: () -> Unit) {
    val shop = rememberShop()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
        ShopHeader(stringResource(R.string.orders_title), null, onBack)
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp).fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(St.Beige).padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(R.string.orders_tab_stock, R.string.orders_tab_customers).forEachIndexed { i, label ->
                val on = tab == i
                val bg by animateColorAsState(if (on) St.Green else Color.Transparent, tween(250), label = "tab")
                Box(
                    Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(bg)
                        .selectable(selected = on, role = Role.Tab) { tab = i },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(label), style = MaterialTheme.typography.labelMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = if (on) Color.White else St.Ink)
                }
            }
        }
        if (tab == 0) {
            val list = shop.restocks
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    list == null -> Unit
                    list.isEmpty() -> EmptyLine(stringResource(R.string.orders_none_stock))
                    else -> list.forEach { RestockCard(it) }
                }
            }
        } else {
            val list = shop.customers
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    list == null -> Unit
                    list.isEmpty() -> EmptyLine(stringResource(R.string.orders_none_customers))
                    else -> list.forEach { CustomerCard(it) }
                }
            }
        }
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = St.Ink2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
}

/** "Today, 10:42 AM" or "Monday, 14 Oct". */
@Composable
private fun whenLabel(time: LocalDateTime?): String {
    time ?: return ""
    val locale = Locale.getDefault()
    return if (time.toLocalDate() == LocalDate.now(Ist)) stringResource(R.string.today_at, time.format(DateTimeFormatter.ofPattern("h:mm a", locale)))
    else time.format(DateTimeFormatter.ofPattern("EEEE, d MMM", locale))
}

@Composable
private fun RestockCard(r: Restock) {
    val o = r.order
    val at = when (o.status) {
        "shipped" -> 1
        "delivered" -> 2
        else -> 0
    }
    val sub = buildList {
        add(stringResource(R.string.orders_number, shortOrderNumber(o.orderNumber, o.id)))
        r.productCount?.let { add(pluralStringResource(R.plurals.shop_products, it, it)) }
        if (o.status == "shipped") parseServerTime(o.updatedAt)?.let { add(stringResource(R.string.orders_sent_on, it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()))) }
    }.joinToString(" · ")
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(St.Paper).border(1.5.dp, St.BeigeLine, shape).padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(whenLabel(parseServerTime(o.createdAt)), style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = St.Ink)
                Text(sub, style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp), color = St.Ink2)
            }
            Text(rupees(o.total.let { kotlin.math.ceil(it) }), style = displayStyle(20, FontWeight.ExtraBold), color = St.Ink)
        }
        if (o.status == "cancelled") {
            Text(stringResource(R.string.track_cancelled), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Brick)
        } else {
            Track(at)
        }
    }
}

/** Asked → On the way → Delivered: three dots on a line, the current one gold. */
@Composable
private fun Track(at: Int) {
    val labels = listOf(R.string.track_asked, R.string.track_way, R.string.track_done).map { stringResource(it) }
    val progress by animateFloatAsState(at / 2f, tween(600), label = "track")
    Box(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = labels[at] }) {
        Box(
            Modifier.fillMaxWidth().height(34.dp).drawBehind {
                val left = size.width / 6f; val right = size.width * 5f / 6f; val y = size.height / 2f
                val h = 4.dp.toPx()
                drawRoundRect(St.Track, Offset(left, y - h / 2), Size(right - left, h), CornerRadius(h / 2))
                drawRoundRect(St.Green, Offset(left, y - h / 2), Size((right - left) * progress, h), CornerRadius(h / 2))
            },
        )
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, label ->
                val done = i < at || at == 2
                val current = i == at && at != 2
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(if (done) St.Green else if (current) St.Gold else St.Paper)
                            .border(3.dp, if (done) St.Green else if (current) St.GoldDeep else St.Line, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (done) Icon(StIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp, fontWeight = if (current) FontWeight.ExtraBold else FontWeight.SemiBold),
                        color = if (i <= at) St.Ink else St.Ink2,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerCard(o: CustomerOrder) {
    val delivered = o.status == "delivered" || o.deliveryStatus == "delivered"
    val moving = !delivered && (o.status == "shipped" || o.deliveryStatus in setOf("out_for_delivery", "picked_up"))
    val day = parseServerTime(o.createdAt)?.toLocalDate()?.let { d ->
        when (d) {
            LocalDate.now(Ist) -> stringResource(R.string.day_today)
            LocalDate.now(Ist).minusDays(1) -> stringResource(R.string.day_yesterday)
            else -> d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }
    }.orEmpty()
    val (status, ink) = when {
        o.status == "cancelled" -> stringResource(R.string.cust_cancelled) to St.Brick
        delivered && o.riderId != null -> stringResource(R.string.cust_rider_delivered, day) to St.Green
        delivered -> stringResource(R.string.cust_you_delivered, day) to St.Green
        moving && o.riderId != null -> stringResource(R.string.cust_rider_taking) to St.GoldInk
        moving -> stringResource(R.string.cust_you_taking) to St.GoldInk
        else -> stringResource(R.string.cust_waiting) to St.Ink2
    }
    val items = o.items.joinToString(", ") { item ->
        val litres = litresOf(item.containerType, item.name)
        "${item.quantity} × ${shortName(item.name)}${litres?.let { " " + sizeLabel(it) }.orEmpty()}"
    }
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(St.Paper).border(1.5.dp, St.BeigeLine, shape).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(St.GreenTint), contentAlignment = Alignment.Center) {
            Text(o.customerName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), color = St.Green)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(o.customerName, style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold), color = St.Ink)
            if (items.isNotBlank()) Text(items, style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp), color = St.Ink2)
            Text(status, style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = ink)
        }
        Text(rupees(o.total), style = displayStyle(19, FontWeight.ExtraBold), color = St.Ink)
    }
}

// ── Money ────────────────────────────────────────────────────────────────────

/** Shop money (canvas "Money · shop commission"): this week, what's earned, what's in the bank, each week. */
@Composable
fun ShopMoneyScreen(onBack: () -> Unit) {
    val shop = rememberShop()
    val money = shop.money
    val weeks = remember(money) { weeklyTotals(money?.commissions.orEmpty()) }
    var picked by rememberSaveable { mutableIntStateOf(weeks.lastIndex) }
    val locale = Locale.getDefault()
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
        ShopHeader(stringResource(R.string.money_title), stringResource(R.string.money_sub), onBack)
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(St.Green).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.money_this_week), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp), color = St.OnGreenMuted)
            Text(rupees(weeks.lastOrNull()?.amount ?: 0.0), style = displayStyle(60, FontWeight.ExtraBold).copy(lineHeight = 60.sp), color = Color.White)
            val day = money?.payoutDay
            Row(
                Modifier.padding(top = 6.dp).clip(RoundedCornerShape(18.dp)).background(St.GreenDeep).padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(StIcons.Bank, contentDescription = null, tint = St.GoldLight, modifier = Modifier.size(20.dp))
                Text(
                    if (day != null) stringResource(R.string.money_bank_day, DayOfWeek.of(if (day == 0) 7 else day).getDisplayName(TextStyle.FULL, locale))
                    else stringResource(R.string.money_bank_weekly),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MoneyTile(stringResource(R.string.money_earned), rupees(money?.summary?.total ?: 0.0), Modifier.weight(1f))
            MoneyTile(stringResource(R.string.money_in_bank), rupees(money?.commissions.orEmpty().filter { it.paidOut }.sumOf { it.amount }), Modifier.weight(1f))
        }
        val cardShape = RoundedCornerShape(28.dp)
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth().clip(cardShape).background(St.Paper).border(1.5.dp, St.BeigeLine, cardShape)
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val p = weeks.getOrNull(picked)
            val label: (Int) -> String = { i -> if (i == weeks.lastIndex) "" else dayMonth(weeks[i].start, locale) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.money_each_week), style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = St.Ink, modifier = Modifier.weight(1f))
                if (p != null) {
                    Text(
                        (if (picked == weeks.lastIndex) stringResource(R.string.money_this_week) else label(picked)) + " · " + rupees(p.amount),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                        color = St.Green,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            val max = (weeks.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(1.0)
            val nowLabel = stringResource(R.string.money_now)
            Row(Modifier.fillMaxWidth().height(150.dp).drawBehind { drawRect(St.ChartGrid, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx())) }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weeks.forEachIndexed { i, w ->
                    val fraction by animateFloatAsState((w.amount / max).toFloat().coerceIn(0.02f, 1f), tween(500), label = "bar$i")
                    val desc = (if (i == weeks.lastIndex) nowLabel else label(i)) + ": " + rupees(w.amount)
                    Box(
                        Modifier.weight(1f).fillMaxHeight().selectable(selected = i == picked, role = Role.Button) { picked = i }.semantics { contentDescription = desc },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight(fraction).clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)).background(if (i == picked) St.Green else St.ChartBar))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weeks.indices.forEach { i ->
                    Text(
                        if (i == weeks.lastIndex) nowLabel else label(i),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, fontWeight = if (i == picked) FontWeight.ExtraBold else FontWeight.Medium),
                        color = St.Ink2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            val thisWeek = weekOf(LocalDate.now(Ist))
            val owed = money?.commissions.orEmpty().filter { it.status == "pending" && (parseServerTime(it.createdAt)?.toLocalDate()?.let(::weekOf)?.isBefore(thisWeek) ?: true) }.sumOf { it.amount }
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (owed <= 0.0) {
                    Icon(StIcons.Check, contentDescription = null, tint = St.Green, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.money_all_paid), style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = St.Green)
                } else {
                    Icon(StIcons.Clock, contentDescription = null, tint = St.GoldInk, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.money_owed, rupees(owed)), style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = St.GoldInk)
                }
            }
        }
    }
}

@Composable
private fun MoneyTile(label: String, value: String, modifier: Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Column(modifier.clip(shape).background(St.Beige).border(1.5.dp, St.BeigeLine, shape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp), color = St.Ink2)
        Text(value, style = displayStyle(28, FontWeight.ExtraBold), color = St.Ink)
    }
}

// ── People (Tier 1) ──────────────────────────────────────────────────────────

/** People (canvas "People · Tier 1"): each sub-dealer, their stock, a call or WhatsApp away. */
@Composable
fun ShopPeopleScreen(onBack: () -> Unit, onSubDealer: (Long) -> Unit) {
    val context = LocalContext.current
    val shop = rememberShop()
    val subs = shop.subShops
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
        ShopHeader(
            stringResource(R.string.people_title),
            subs?.let { pluralStringResource(R.plurals.people_sub, it.size, it.size) },
            onBack,
        )
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (subs != null && subs.isEmpty()) EmptyLine(stringResource(R.string.people_none))
            subs?.forEach { sub -> SubDealerCard(sub, onOpen = { onSubDealer(sub.dealer.id) }) }
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val code = shop.referralCode
            val shareText = code?.let { stringResource(R.string.share_text, it) }
            val goldShape = RoundedCornerShape(24.dp)
            Column(
                Modifier.weight(1f).clip(goldShape).background(St.GoldGlint).border(2.dp, St.GoldPale, goldShape)
                    .clickable(enabled = shareText != null, role = Role.Button) { shareText?.let(context::shareOnWhatsApp) }.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(StIcons.Share, contentDescription = null, tint = St.GoldInk, modifier = Modifier.size(22.dp))
                    Text(stringResource(R.string.share_code), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = St.Ink)
                }
                Text(code ?: "—", style = displayStyle(24, FontWeight.ExtraBold).copy(letterSpacing = 1.sp), color = St.Ink)
                Text(stringResource(R.string.share_sends), style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = St.GoldInk)
            }
            val lucky = shop.luckyDraw.orEmpty()
            val entered = lucky.count { it.standing == "entered" || it.standing == "winning" }
            val waiting = lucky.filter { it.standing == "not_entered" }
            val beigeShape = RoundedCornerShape(24.dp)
            Column(
                Modifier.weight(1f).clip(beigeShape).background(St.Beige).border(1.5.dp, St.BeigeLine, beigeShape).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(StIcons.Trophy, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
                    Text(stringResource(R.string.lucky_title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = St.Ink)
                }
                if (lucky.isEmpty()) {
                    Text(stringResource(R.string.lucky_none), style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = St.Ink2)
                } else {
                    Text(stringResource(R.string.lucky_in, entered, lucky.size), style = displayStyle(24, FontWeight.ExtraBold), color = St.Ink)
                    Text(
                        when {
                            waiting.isEmpty() -> stringResource(R.string.lucky_all_in)
                            waiting.size == 1 -> stringResource(R.string.lucky_not_ordered, waiting.first().customerName?.trim()?.substringBefore(' ').orEmpty())
                            else -> pluralStringResource(R.plurals.lucky_not_ordered_many, waiting.size, waiting.size)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = St.Ink2,
                    )
                }
            }
        }
    }
}

private val SubShop.firstName: String get() = dealer.name.trim().substringBefore(' ').ifEmpty { dealer.name }

@Composable
private fun SubDealerCard(sub: SubShop, onOpen: () -> Unit) {
    val context = LocalContext.current
    val inventory = sub.inventory
    val finished = inventory?.count { it.stock == Stock.Finished } ?: 0
    val low = inventory?.count { it.stock == Stock.Low } ?: 0
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(St.Paper).border(1.5.dp, St.BeigeLine, shape).padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(50.dp).clip(CircleShape).background(St.Beige), contentAlignment = Alignment.Center) {
                Text(sub.dealer.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink)
            }
            Column(Modifier.weight(1f)) {
                Text(sub.firstName, style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = St.Ink)
                val area = areaOf(sub.dealer.address)
                if (area.isNotBlank()) Text(area, style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp), color = St.Ink2)
            }
            if (inventory != null) {
                val (text, stock) = when {
                    finished > 0 -> pluralStringResource(R.plurals.shop_finished_count, finished, finished) to Stock.Finished
                    low > 0 -> stringResource(R.string.people_low, low) to Stock.Low
                    else -> stringResource(R.string.people_all_good) to Stock.Plenty
                }
                val (bg, ink, line) = chipColors(stock)
                Row(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(bg).border(1.5.dp, line, RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onOpen)
                        .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = ink)
                    Icon(StIcons.ChevronRight, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShopOutlineButton(stringResource(R.string.call), { context.dial(sub.dealer.phone) }, Modifier.weight(1f), icon = StIcons.Phone, height = 52.dp)
            ShopOutlineButton(stringResource(R.string.whatsapp), { context.openWhatsApp(sub.dealer.phone, "Namaskara ${sub.firstName},") }, Modifier.weight(1f), icon = StIcons.WhatsApp, height = 52.dp)
        }
    }
}

/** A sub-dealer's shelf (canvas "A sub-dealer's shelf"), with a call or a WhatsApp reminder. */
@Composable
fun SubDealerScreen(id: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val shop = rememberShop()
    val sub = shop.subShops?.firstOrNull { it.dealer.id == id }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
        ShopHeader(
            sub?.let { stringResource(R.string.sub_shelf, it.firstName) } ?: stringResource(R.string.people_title),
            sub?.dealer?.address?.let(::areaOf),
            onBack,
            backLabel = stringResource(R.string.people_title),
        )
        sub ?: return@Column
        val inventory = sub.inventory
        if (inventory == null) {
            Text(stringResource(R.string.sub_failed), style = MaterialTheme.typography.bodyLarge, color = St.Ink2, modifier = Modifier.padding(20.dp))
            return@Column
        }
        val items = inventory.sortedBy { if (it.needsAttention) it.quantity else Int.MAX_VALUE }
        val packs = items.associateWith { packLabel(it) }
        val counts = items.associateWith { countLabel(it) }
        val descriptions = items.associateWith { describe(it) }
        OilShelf(
            items = items,
            name = { it.label },
            packLabel = { packs.getValue(it) },
            countLabel = { counts.getValue(it) },
            describe = { descriptions.getValue(it) },
            onOpen = null,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp),
        )
        val worst = items.firstOrNull { it.needsAttention }
        val shape = RoundedCornerShape(26.dp)
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().clip(shape).background(St.Paper).border(1.5.dp, St.BeigeLine, shape).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val text = when {
                worst == null -> stringResource(R.string.sub_all_good, sub.firstName)
                worst.stock == Stock.Finished -> stringResource(R.string.sub_has_none, sub.firstName, worst.fullName())
                else -> stringResource(R.string.sub_running_low, sub.firstName, worst.fullName())
            }
            Text(AnnotatedString.fromHtml(text), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 27.sp), color = St.Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShopOutlineButton(stringResource(R.string.call), { context.dial(sub.dealer.phone) }, Modifier.weight(1f), icon = StIcons.Phone)
                if (worst != null) {
                    val remind = stringResource(R.string.remind_text, sub.firstName, worst.fullName())
                    Row(
                        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(28.dp)).background(St.Green).clickable(role = Role.Button) { context.openWhatsApp(sub.dealer.phone, remind) },
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(StIcons.WhatsApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Text(stringResource(R.string.remind), style = MaterialTheme.typography.labelMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = Color.White)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
