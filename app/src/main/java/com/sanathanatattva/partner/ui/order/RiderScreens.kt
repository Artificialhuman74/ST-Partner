package com.sanathanatattva.partner.ui.order

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.OrderCard
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.SlideButton
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.rupees
import com.sanathanatattva.partner.ui.login.dialSupport
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St

/** Canvas "RiderToShop": head to the shop that packed it. */
@Composable
fun RiderToShopScreen(o: OrderCard, vm: OrderViewModel, onBack: () -> Unit, onArrived: () -> Unit) {
    val context = LocalContext.current
    val shop = o.shop
    OrderPage(
        title = stringResource(R.string.order_title, o.orderNumber),
        onBack = onBack,
        chip = { GreenChip(stringResource(R.string.order_you_earn, rupees(o.earn))) },
        bottom = {
            OrderAction(stringResource(R.string.rider_at_shop), onClick = onArrived, leading = StIcons.Arrived)
            TextAction(stringResource(R.string.order_need_help)) { context.dialSupport() }
        },
    ) {
        Steps(riderSteps(), current = 0)
        OrderHeading(stringResource(R.string.rider_go_to, shop?.name.orEmpty()))
        if (shop?.lat != null && shop.lng != null) {
            RouteMap(
                to = shop.lat to shop.lng, toPin = Pin.Shop, mover = vm.me, moverPin = Pin.Me,
                label = o.toShopKm?.let { stringResource(R.string.km, "%.1f".format(it)) },
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            AddressCard(
                name = shop.name, address = shop.address.orEmpty(),
                callLabel = shop.phone?.let { stringResource(R.string.call_shop) }, onCall = shop.phone?.let { p -> { dial(context, p) } },
                mapsLabel = stringResource(R.string.google_maps), onMaps = { openNavigation(context, shop.lat, shop.lng) },
            )
        }
        LinesSummary(o.lines, title = stringResource(R.string.rider_collect))
    }
}

@Composable
private fun riderSteps() = listOf(stringResource(R.string.step_to_shop), stringResource(R.string.step_pick_up), stringResource(R.string.step_deliver))

/**
 * Canvas "RiderPickupCode": the rider shows 4 numbers; the shop types them. Screen brightness
 * goes to full so the code reads easily across a counter.
 */
@Composable
fun RiderPickupCodeScreen(o: OrderCard, vm: OrderViewModel, picked: Boolean, onBack: () -> Unit, onGo: () -> Unit) {
    val context = LocalContext.current
    FullBrightness()
    val session by context.appContainer.sessions.session.collectAsState(initial = context.appContainer.sessions.latest)
    val me = session?.user
    OrderPage(
        title = stringResource(R.string.order_title, o.orderNumber),
        onBack = onBack,
        chip = {
            Row(
                Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(St.Beige).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(StIcons.Sun, contentDescription = null, tint = St.Ink, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.pickup_brightness), style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = St.Ink)
            }
        },
        bottom = {
            if (picked) {
                AnimatedVisibility(true, enter = fadeIn(tween(600)) + slideInVertically(tween(600, easing = OilEase)) { it / 6 }) {
                    OrderAction(stringResource(R.string.rider_go_customer), onClick = onGo, trailing = StIcons.ArrowRight)
                }
            } else {
                o.shop?.phone?.let { phone ->
                    OutlineAction(stringResource(R.string.call_the_shop), StIcons.Phone, Modifier.fillMaxWidth().height(56.dp)) { dial(context, phone) }
                }
            }
        },
    ) {
        Steps(listOf(stringResource(R.string.step_at_shop), stringResource(R.string.step_pick_up), stringResource(R.string.step_deliver)), current = 1, tickDone = false)
        OrderHeading(stringResource(if (picked) R.string.pickup_done_title else R.string.pickup_show_title))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(St.Beige).border(2.dp, St.Green, RoundedCornerShape(26.dp)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShowCode(o.pickupCode ?: "····", faded = picked, label = stringResource(R.string.pickup_code_label))
            Box(Modifier.heightIn(min = 28.dp).semantics { liveRegion = LiveRegionMode.Polite }, contentAlignment = Alignment.Center) {
                if (picked) PickedUpStatus(stringResource(R.string.pickup_confirmed))
                else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.pickup_waiting_shop, o.shop?.name.orEmpty()), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.Green)
                    WaitingDots()
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(St.Beige).border(1.5.dp, St.BeigeLine, RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(null, 44.dp, ring = 2.5.dp)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.pickup_shop_sees), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = St.Ink2)
                Text(listOfNotNull(me?.firstName, me?.vehiclePlate?.let(::spacedPlate)).joinToString(" · "), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Ink)
            }
        }
        LinesSummary(o.lines, title = stringResource(R.string.pickup_check))
    }
}

/** "pc-in" + "pc-pop": the confirmation slides up, its tick pops. */
@Composable
fun PickedUpStatus(text: String) {
    val reduce = LocalReduceMotion.current
    val inn = remember { Animatable(if (reduce) 1f else 0f) }
    val pop = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        inn.animateTo(1f, tween(600, easing = OilEase))
    }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(550, easing = OilEase)) }
    Row(
        Modifier.graphicsLayer { alpha = inn.value; translationY = 10.dp.toPx() * (1f - inn.value) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(26.dp).graphicsLayer { scaleX = 0.4f + 0.6f * pop.value; scaleY = scaleX; alpha = pop.value }.clip(CircleShape).background(St.Green), contentAlignment = Alignment.Center) {
            Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(15.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = St.Green)
    }
}

/** Screen at full brightness while a code is being shown; restored on leave. */
@Composable
fun FullBrightness() {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(activity) {
        val window = activity.window
        val before = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        onDispose { window.attributes = window.attributes.apply { screenBrightness = before } }
    }
}

/**
 * Canvas "OnTheWay" (shop delivering) and "RiderToCustomer": the road to the customer, their
 * address, a Call button that never shows the number, and what to hand over.
 */
@Composable
fun ToCustomerScreen(o: OrderCard, vm: OrderViewModel, rider: Boolean, onBack: () -> Unit, onArrived: () -> Unit, onFailed: () -> Unit) {
    val context = LocalContext.current
    val c = o.customer
    OrderPage(
        title = stringResource(R.string.order_title, o.orderNumber),
        onBack = onBack,
        chip = { GreenChip(stringResource(if (rider) R.string.order_you_earn else R.string.order_commission, rupees(o.earn))) },
        bottom = {
            // A slide, not a tap, so a pocket press can't jump to the hand-over.
            SlideButton(label = stringResource(R.string.order_complete), onComplete = onArrived, chevrons = true, nudge = 22.dp)
            TextAction(stringResource(R.string.order_couldnt), onClick = onFailed)
        },
    ) {
        if (rider) Steps(listOf(stringResource(R.string.step_at_shop), stringResource(R.string.step_picked_up), stringResource(R.string.step_deliver)), current = 2, tickDone = false)
        else Steps(listOf(stringResource(R.string.step_packed), stringResource(R.string.step_on_way), stringResource(R.string.step_hand_over)), current = 1)
        OrderHeading(stringResource(R.string.order_take_to, c.firstName))
        if (c.lat != null && c.lng != null) {
            val km = (if (rider) o.shopToCustomerKm else o.distanceKm)
            RouteMap(
                to = c.lat to c.lng, toPin = Pin.Customer, mover = vm.me, moverPin = if (rider) Pin.Rider else Pin.Me,
                from = if (rider) o.shop?.let { s -> if (s.lat != null && s.lng != null) s.lat to s.lng else null } else null,
                label = km?.let { stringResource(R.string.km, "%.1f".format(it)) },
                modifier = Modifier.fillMaxWidth().height(if (rider) 190.dp else 200.dp),
            )
        }
        AddressCard(
            name = c.firstName, address = c.address.orEmpty(),
            callLabel = stringResource(R.string.call_person, c.firstName), onCall = { vm.callCustomer { dial(context, it) } },
            mapsLabel = stringResource(R.string.google_maps),
            onMaps = { if (c.lat != null && c.lng != null) openNavigation(context, c.lat, c.lng) },
        )
        LinesSummary(o.lines)
    }
}

/** Canvas "ReturnToShop": couldn't deliver, so the oil goes back with a code the shop types. */
@Composable
fun ReturnToShopScreen(o: OrderCard, returned: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val shop = o.shop
    OrderPage(
        title = stringResource(R.string.order_title, o.orderNumber),
        onBack = onBack,
        chip = { BeigeChip(stringResource(R.string.order_returning)) },
        bottom = {
            if (returned) {
                OrderAction(stringResource(R.string.order_back_to_orders), onClick = onBack, trailing = StIcons.ArrowRight)
            } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                shop?.phone?.let { phone -> OutlineAction(stringResource(R.string.call_shop), StIcons.Phone, Modifier.weight(1f).height(60.dp)) { dial(context, phone) } }
                if (shop?.lat != null && shop.lng != null) {
                    Box(Modifier.weight(1f)) {
                        OrderAction(stringResource(R.string.return_maps), onClick = { openNavigation(context, shop.lat, shop.lng) }, leading = StIcons.Map)
                    }
                }
            }
        },
    ) {
        OrderHeading(if (returned) stringResource(R.string.return_done_title) else stringResource(R.string.return_title, shop?.name.orEmpty()))
        GreenNote(stringResource(R.string.return_tomorrow, o.customer.firstName), icon = StIcons.Clock)
        if (!returned) FullBrightness()
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(St.Beige).border(2.dp, St.Green, RoundedCornerShape(26.dp)).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.return_show_code), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.Ink3)
            ShowCode(o.returnCode ?: "····", faded = returned, tile = 62.dp, height = 76.dp, font = 46, label = stringResource(R.string.return_code_label))
            Box(Modifier.heightIn(min = 26.dp).semantics { liveRegion = LiveRegionMode.Polite }, contentAlignment = Alignment.Center) {
                if (returned) PickedUpStatus(stringResource(R.string.return_got_back))
                else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.return_waiting, shop?.name.orEmpty()), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = St.Green)
                    WaitingDots()
                }
            }
        }
        LinesSummary(o.lines, title = stringResource(R.string.return_give_back)) {
            Box(Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(St.Cream).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.return_cans, o.load.cans), style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink)
            }
        }
    }
}
