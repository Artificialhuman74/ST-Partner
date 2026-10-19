package com.sanathanatattva.partner.ui.order

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.data.api.OrderCard
import com.sanathanatattva.partner.data.api.OrderLine
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.box
import com.sanathanatattva.partner.ui.components.dashedBorder
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.line
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.rupees
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.login.text
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.delay

/* ── Pack (shop delivering it, or packing for a rider) ─────────────────── */

/** Canvas "OrderCard" (shop delivers) and "PackForRider": tick each line as it goes in the bag. */
@Composable
fun PackScreen(o: OrderCard, vm: OrderViewModel, forRider: Boolean, onBack: () -> Unit, onRiderCard: () -> Unit) {
    val ticked = rememberSaveable(o.orderId) { mutableStateListOf<Int>().also { if (o.packed) it.addAll(o.lines.indices) } }
    val left = o.lines.size - ticked.size
    val ready = left == 0
    OrderPage(
        title = stringResource(R.string.order_title, o.orderNumber),
        onBack = onBack,
        chip = { if (forRider) BeigeChip(stringResource(R.string.order_rider_delivers)) else GreenChip(stringResource(R.string.order_commission, rupees(o.earn))) },
        bottom = {
            val label = when {
                o.packed && forRider -> stringResource(R.string.pack_waiting_rider)
                ready && forRider -> stringResource(R.string.pack_ready_pickup)
                ready -> stringResource(R.string.pack_start)
                left == 1 -> stringResource(R.string.pack_tick_one)
                else -> stringResource(R.string.pack_tick_each)
            }
            OrderAction(
                label,
                // A retried order is already packed: the shop just sets off again.
                onClick = { if (o.packed && !forRider) vm.startDelivery() else vm.packed(startAfter = !forRider) },
                enabled = ready && (!o.packed || !forRider),
                trailing = if (ready && !forRider) StIcons.ArrowRight else null,
                loading = vm.busy,
                done = vm.doneFlash,
            )
        },
    ) {
        if (forRider) {
            RiderStatusBanner(o, vm, onRiderCard)
        } else {
            Steps(listOf(stringResource(R.string.step_pack), stringResource(R.string.step_on_way), stringResource(R.string.step_hand_over)), current = 0, goldNow = true, tickDone = false)
        }
        OrderHeading(stringResource(R.string.pack_title), Modifier.padding(top = 4.dp))
        val list: @Composable () -> Unit = {
            o.lines.forEachIndexed { i, l ->
                PackLine(l, done = i in ticked, art = !forRider, onToggle = { if (i in ticked) ticked.remove(i) else ticked.add(i) })
            }
        }
        if (forRider) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(St.Beige).dashedBorder(2.5.dp, St.Green, 26.dp).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) { list() }
            o.shareAmount?.let { share ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(St.Beige).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.pack_share), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = St.Ink3, modifier = Modifier.weight(1f))
                    Text(rupees(share), style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold), color = St.Green)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { list() }
            o.customer.address?.let { address ->
                val context = LocalContext.current
                AddressCard(
                    name = o.customer.firstName,
                    address = address,
                    callLabel = null, onCall = null,
                    mapsLabel = stringResource(R.string.order_open_maps),
                    onMaps = { if (o.customer.lat != null && o.customer.lng != null) openNavigation(context, o.customer.lat, o.customer.lng) },
                )
            }
            GreenNote(stringResource(R.string.order_paid_online))
        }
        vm.error?.let { Text(text(it), color = St.Brick, style = MaterialTheme.typography.bodyMedium) }
    }
}

/** One line to tick: checkbox, (can), qty + name, and the loud REFILL / NEW banner. */
@Composable
private fun PackLine(l: OrderLine, done: Boolean, art: Boolean, onToggle: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val border by animateColorAsState(if (done) Color(0xFF1A6B2E) else St.LineSoft, tween(350, easing = OilEase), label = "b")
    val bg by animateColorAsState(if (done) Color(0xFFF3F8F1) else if (art) St.Paper else St.Cream, tween(350, easing = OilEase), label = "bg")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(if (art) 22.dp else 18.dp)).background(bg)
            .border(if (art) 2.5.dp else 2.dp, border, RoundedCornerShape(if (art) 22.dp else 18.dp))
            .toggleable(value = done, role = Role.Checkbox) { haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff); onToggle() }
            .padding(if (art) 12.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (art) 10.dp else 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TickBox(done)
            if (art) PlainCan(Modifier.size(width = 44.dp, height = 46.dp))
            Column(Modifier.weight(1f)) {
                Text(l.qtyText(), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 27.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink)
                Text(l.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight(650)), color = St.Ink)
            }
        }
        CanBanner(l.lineType, forRider = false, wiggle = art)
    }
}

/** "oc-tick": the tick spins in from 30% when a line is packed. */
@Composable
private fun TickBox(done: Boolean) {
    val reduce = LocalReduceMotion.current
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (done) Color(0xFF1A6B2E) else St.Paper)
            .border(3.dp, if (done) Color(0xFF1A6B2E) else St.FieldBorder, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            val p = remember { Animatable(if (reduce) 1f else 0f) }
            LaunchedEffect(Unit) { p.animateTo(1f, tween(500, easing = OilEase)) }
            Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(20.dp).graphicsLayer { scaleX = 0.3f + 0.7f * p.value; scaleY = scaleX; rotationZ = -25f * (1f - p.value); alpha = p.value })
        }
    }
}

private val CanHandle = svgPath("M19 13q9-10 18 0")

/** The canvas's plain steel can with a green label (pack checklist). */
@Composable
private fun PlainCan(modifier: Modifier) {
    Canvas(modifier) {
        viewBox(76f, 78f) {
            translate(12f, 10f) {
                scale(0.9f, 0.9f, Offset.Zero) {
                    line(CanHandle, St.Ink, 2.6f)
                    box(8f, 12f, 40f, 50f, 7f, fill = St.Steel, stroke = St.Ink, strokeWidth = 2.2f)
                    box(13f, 22f, 4f, 34f, 2f, fill = Color.White, alpha = 0.8f)
                    box(8f, 12f, 40f, 8f, 4f, fill = St.SteelDark, stroke = St.Ink, strokeWidth = 2.2f)
                    box(18f, 28f, 28f, 22f, 3f, fill = Color(0xFF1A6B2E))
                }
            }
        }
    }
}

/**
 * REFILL ONLY (solid gold) / PACK A NEW CAN (solid green), never missable (handoff §10).
 * [wiggle]: "cn-ico", the can icon gives a little shake every 3.6 s.
 */
@Composable
fun CanBanner(lineType: String, forRider: Boolean, wiggle: Boolean) {
    if (lineType != "refill" && lineType != "new") return
    val refill = lineType == "refill"
    val reduce = LocalReduceMotion.current
    val t by rememberLoop(3600)
    val (title, body) = when {
        refill && forRider -> R.string.stamp_refill to R.string.stamp_refill_rider
        refill -> R.string.stamp_refill to R.string.stamp_refill_trader
        forRider -> R.string.stamp_new_rider to R.string.stamp_new_body
        else -> R.string.stamp_new_trader to R.string.stamp_new_body
    }
    val ink = if (refill) St.Ink else St.Cream
    Row(
        Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(14.dp)).background(if (refill) St.Gold else St.Green).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Starts after 1 s, then shakes in the first 14% of each 3.6 s.
        val p = if (!wiggle || reduce) 0f else ((t * 3600f - 1000f).let { if (it < 0) it + 3600f else it } / 3600f)
        val rot = if (!wiggle || reduce) 0f else kf(p, 0f to 0f, 0.03f to -12f, 0.07f to 10f, 0.11f to -4f, 0.14f to 0f, 1f to 0f)
        val scale = if (!wiggle || reduce) 1f else kf(p, 0f to 1f, 0.03f to 1.08f, 0.07f to 1.08f, 0.11f to 1f, 1f to 1f)
        Icon(
            if (refill) StIcons.CanNoNew else StIcons.CanNew, contentDescription = null, tint = ink,
            modifier = Modifier.size(30.dp).graphicsLayer { rotationZ = rot; scaleX = scale; scaleY = scale; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.6f) },
        )
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp), color = ink)
            Text(stringResource(body), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = if (refill) ink else St.OnGreenMuted)
        }
    }
}

/** "Finding a rider near you…" (ripples) or "Ravi is coming · 1.1 km" once someone accepts. */
@Composable
private fun RiderStatusBanner(o: OrderCard, vm: OrderViewModel, onRiderCard: () -> Unit) {
    val rider = o.rider
    AnimatedContent(rider != null, transitionSpec = { fadeIn(tween(600, easing = OilEase)) togetherWith fadeOut(tween(200)) }, label = "rider") { found ->
        val shape = RoundedCornerShape(20.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 76.dp).clip(shape).background(St.GreenTint)
                .then(if (found) Modifier.clickable(onClick = onRiderCard) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (found && rider != null) {
                Avatar(vm.riderPhoto, 48.dp, ring = 2.5.dp, description = stringResource(R.string.order_rider_photo, rider.firstName))
                Column(Modifier.weight(1f)) {
                    val km = riderKm(o)
                    Text(
                        if (km != null) stringResource(R.string.rider_coming_km, rider.firstName, km) else stringResource(R.string.rider_coming, rider.firstName),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold), color = St.Green,
                    )
                    Text(listOfNotNull(vehicleName(rider.vehicleType), rider.vehiclePlate?.let(::spacedPlate)).joinToString(" · "), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = St.Ink3)
                }
                Icon(StIcons.ChevronRight, contentDescription = null, tint = Color(0xFF1A6B2E), modifier = Modifier.size(22.dp))
            } else {
                SearchRipples()
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.rider_finding), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold), color = St.Green)
                    Text(stringResource(R.string.rider_finding_body), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = St.Ink3)
                }
            }
        }
    }
}

/** "pr-rip" ×2: rings spreading from a search dot, 1.2 s apart. */
@Composable
private fun SearchRipples() {
    val reduce = LocalReduceMotion.current
    val t by rememberLoop(2400)
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(48.dp)) {
            if (!reduce) listOf(t, (t + 0.5f) % 1f).forEach { p ->
                val e = OilEase.transform(p)
                drawCircle(Color(0xFF1A6B2E).copy(alpha = 0.8f * (1f - e)), radius = size.minDimension / 2 * (0.4f + 1.1f * e), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx()))
            }
        }
        Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF1A6B2E)), contentAlignment = Alignment.Center) {
            Icon(StIcons.Search, contentDescription = null, tint = St.Cream, modifier = Modifier.size(14.dp))
        }
    }
}

fun riderKm(o: OrderCard): String? {
    val live = o.rider?.live ?: return null
    val lat = o.shop?.lat ?: return null
    val lng = o.shop?.lng ?: return null
    val d = FloatArray(1)
    android.location.Location.distanceBetween(live.lat, live.lng, lat, lng, d)
    return "%.1f".format(d[0] / 1000f)
}

@Composable
fun vehicleName(type: String?): String? = when (type) {
    "walk" -> stringResource(R.string.signup_vehicle_walk)
    "cycle" -> stringResource(R.string.signup_vehicle_cycle)
    "scooter" -> stringResource(R.string.signup_vehicle_scooter)
    "bike" -> stringResource(R.string.signup_vehicle_bike)
    else -> null
}

/* ── Rider is coming / delivering / bringing it back ───────────────────── */

enum class RiderLeg { Coming, Delivering, Returning }

/** Canvas "RiderComing": live map of the rider heading to the shop, who they are, what they take. */
@Composable
fun RiderComingScreen(o: OrderCard, vm: OrderViewModel, leg: RiderLeg, onBack: () -> Unit, onEnterCode: () -> Unit) {
    val context = LocalContext.current
    val rider = o.rider ?: return
    OrderPage(
        title = stringResource(R.string.order_title, o.orderNumber),
        onBack = onBack,
        chip = { if (leg == RiderLeg.Returning) BeigeChip(stringResource(R.string.order_returning)) else GreenChip(stringResource(R.string.order_packed), tick = true) },
        bottom = {
            if (leg != RiderLeg.Delivering) {
                OrderAction(stringResource(R.string.rider_here_code, rider.firstName), onClick = onEnterCode, leading = StIcons.Keypad)
            } else {
                OrderAction(stringResource(R.string.order_back_to_orders), onClick = onBack, trailing = StIcons.ArrowRight)
            }
        },
    ) {
        OrderHeading(
            stringResource(
                when (leg) {
                    RiderLeg.Coming -> R.string.rider_coming_title
                    RiderLeg.Delivering -> R.string.rider_delivering_title
                    RiderLeg.Returning -> R.string.rider_returning_title
                },
                rider.firstName,
            ),
        )
        OrderBody(
            when (leg) {
                RiderLeg.Coming -> stringResource(R.string.rider_coming_body)
                RiderLeg.Delivering -> stringResource(R.string.rider_delivering_body)
                RiderLeg.Returning -> stringResource(R.string.rider_returning_body)
            },
            Modifier.padding(top = 0.dp),
        )
        val shop = o.shop
        if (shop?.lat != null && shop.lng != null) {
            val target = if (leg == RiderLeg.Delivering && o.customer.lat != null && o.customer.lng != null) o.customer.lat to o.customer.lng else shop.lat to shop.lng
            val km = riderKm(o)
            RouteMap(
                to = target,
                toPin = if (leg == RiderLeg.Delivering) Pin.Customer else Pin.Shop,
                mover = rider.live?.let { it.lat to it.lng },
                moverPin = Pin.Rider,
                label = if (leg != RiderLeg.Delivering && km != null) stringResource(R.string.km_away, km) else null,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
        }
        BeigePanel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Avatar(vm.riderPhoto, 72.dp, description = stringResource(R.string.order_rider_photo, rider.firstName))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(rider.name, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = St.Ink)
                    rider.vehiclePlate?.let { PlateBadge(it) }
                    vehicleName(rider.vehicleType)?.let { Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = St.Ink2) }
                }
            }
            rider.phone?.let { phone ->
                OutlineAction(stringResource(R.string.call_person_number, rider.firstName, phone), StIcons.Phone, Modifier.fillMaxWidth()) { dial(context, phone) }
            }
        }
        LinesSummary(o.lines)
    }
}

/* ── Typing the rider's code ───────────────────────────────────────────── */

/** Canvas "EnterRiderCode": look at the face and plate, then type the 4 numbers the rider shows. */
@Composable
fun EnterRiderCodeScreen(o: OrderCard, vm: OrderViewModel, returning: Boolean, onBack: () -> Unit) {
    val rider = o.rider ?: return
    var code by rememberSaveable { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current
    val shake = remember { Animatable(0f) }
    var wrong by remember { mutableStateOf(0) }
    LaunchedEffect(wrong) {
        if (wrong == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.Reject)
        for (x in listOf(-10f, 10f, -6f, 6f, 0f)) shake.animateTo(x, tween(60))
        delay(200)
        code = ""
    }
    OrderPage(
        title = stringResource(R.string.order_title, o.orderNumber),
        onBack = onBack,
        bottom = {
            OrderAction(
                stringResource(if (returning) R.string.code_took_back else R.string.code_handed_over, rider.firstName),
                onClick = { if (returning) vm.returned(code) { wrong++ } else vm.handover(code) { wrong++ } },
                enabled = code.length == 4,
                leading = StIcons.Check,
                loading = vm.busy,
                done = vm.doneFlash,
            )
        },
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(St.Beige).border(1.5.dp, St.BeigeLine, RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Avatar(vm.riderPhoto, 64.dp, description = stringResource(R.string.order_rider_photo, rider.firstName))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.code_is_this, rider.firstName), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink)
                rider.vehiclePlate?.let { PlateBadge(it, big = false) }
                Text(stringResource(R.string.code_check_face), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = St.Ink2)
            }
        }
        OrderHeading(stringResource(R.string.code_type, rider.firstName), Modifier.padding(top = 4.dp))
        OrderBody(stringResource(R.string.code_type_body))
        CodeBoxes(code, 4, Modifier.padding(horizontal = 24.dp).graphicsLayer { translationX = shake.value.dp.toPx() })
        Box(Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics { liveRegion = LiveRegionMode.Polite }, contentAlignment = Alignment.Center) {
            when {
                vm.doneFlash -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(StIcons.CircleCheck, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
                    Text(stringResource(if (returning) R.string.code_ok_back else R.string.code_ok_hand, rider.firstName), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = St.Green)
                }
                vm.error != null -> Text(text(vm.error!!), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = St.Brick)
            }
        }
        NumPad(
            onDigit = { if (code.length < 4) { code += it; vm.error = null } },
            onClear = { code = "" },
            onDelete = { code = code.dropLast(1) },
        )
    }
}

/* ── Simple outcomes ───────────────────────────────────────────────────── */

/** After a failed delivery: the oil is back on the shelf and goes out again tomorrow. */
@Composable
fun BackTomorrowScreen(o: OrderCard?, onBack: () -> Unit) {
    OrderPage(
        title = o?.let { stringResource(R.string.order_title, it.orderNumber) } ?: "",
        onBack = onBack,
        bottom = { OrderAction(stringResource(R.string.order_back_to_orders), onClick = onBack, trailing = StIcons.ArrowRight) },
    ) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.align(Alignment.CenterHorizontally).size(88.dp).clip(CircleShape).background(St.GreenTint), contentAlignment = Alignment.Center) {
            Icon(StIcons.Shop, contentDescription = null, tint = St.Green, modifier = Modifier.size(44.dp))
        }
        OrderHeading(stringResource(R.string.tomorrow_title), Modifier.padding(top = 8.dp))
        OrderBody(stringResource(R.string.tomorrow_body))
        o?.let { LinesSummary(it.lines, title = stringResource(R.string.tomorrow_keep)) }
    }
}
