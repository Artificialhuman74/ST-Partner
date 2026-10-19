package com.sanathanatattva.partner.ui.order

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.theme.St

/** Steps inside a stage that are the partner's own (the server only knows "out for delivery"). */
private enum class Leg { Main, AtShop, Code, Photo, Fail, Sent }

/**
 * One order, whichever side you're on. The server's stage + your role pick the screen; your
 * own taps (arrived, code typed, photo) move you through the steps in between.
 */
@Composable
fun OrderScreen(onBack: () -> Unit) {
    val vm: OrderViewModel = viewModel()
    LifecycleResumeEffect(Unit) {
        vm.start()
        onPauseOrDispose { vm.stop() }
    }
    var leg by rememberSaveable { mutableStateOf(Leg.Main) }
    var otp by rememberSaveable { mutableStateOf("") }
    // The rider sees "Picked up!" before moving on, even though the server has already moved.
    var pickedAck by rememberSaveable { mutableStateOf(false) }
    val o = vm.order

    BackHandler(enabled = leg != Leg.Main) {
        leg = when (leg) {
            Leg.Photo -> Leg.Code
            else -> Leg.Main
        }
    }
    // A stage change from the other side resets local steps that no longer apply.
    LaunchedEffect(o?.stage) {
        if (o?.stage == "picked_up" && o.role == "rider" && leg == Leg.AtShop) pickedAck = true
        if (o?.stage == "returning" && o.role == "packer" && leg == Leg.Code) leg = Leg.Main
    }

    val earned = vm.earned
    val screen: String = when {
        earned != null -> "delivered"
        o == null && !vm.gone -> "loading"
        vm.gone -> "gone"
        else -> "${o!!.role}/${o.stage}/${o.deliveryStatus}/$leg/$pickedAck"
    }
    AnimatedContent(
        targetState = screen,
        transitionSpec = { (fadeIn(tween(420, easing = OilEase)) + slideInHorizontally(tween(520, easing = OilEase)) { it / 12 }) togetherWith fadeOut(tween(200)) },
        label = "order",
        modifier = Modifier.fillMaxSize().background(St.Cream),
    ) { key ->
        val order = vm.order
        when {
            key == "loading" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = St.Green) }
            key == "delivered" -> DeliveredScreen(
                customer = order?.customer?.firstName.orEmpty(), earned = earned ?: 0.0, trader = order?.role != "rider",
                todayDeliveries = vm.todayDeliveries, todayEarned = vm.todayEarned, onDone = onBack,
            )
            // A rider's order disappears once the shop takes it back.
            key == "gone" -> if (order != null && order.role == "rider") ReturnToShopScreen(order, returned = true, onBack = onBack) else BackTomorrowScreen(order, onBack)
            order == null -> Unit
            order.role == "rider" -> RiderFlow(order, vm, leg, { leg = it }, otp, { otp = it }, pickedAck, { pickedAck = false; leg = Leg.Main }, onBack)
            order.role == "packer" -> PackerFlow(order, vm, leg, { leg = it }, onBack)
            else -> SelfFlow(order, vm, leg, { leg = it }, otp, { otp = it }, onBack)
        }
    }
}

@Composable
private fun SelfFlow(
    o: com.sanathanatattva.partner.data.api.OrderCard, vm: OrderViewModel,
    leg: Leg, setLeg: (Leg) -> Unit, otp: String, setOtp: (String) -> Unit, onBack: () -> Unit,
) {
    when {
        o.stage == "retry_wait" || leg == Leg.Sent -> BackTomorrowScreen(o, onBack)
        o.deliveryStatus in setOf("pending", "accepted") || (o.deliveryStatus == "packed" && o.stage != "retry_wait") ->
            PackScreen(o, vm, forRider = false, onBack = onBack, onRiderCard = {})
        o.deliveryStatus == "out_for_delivery" -> Delivering(o, vm, rider = false, leg, setLeg, otp, setOtp, onBack)
        else -> BackTomorrowScreen(o, onBack)
    }
}

@Composable
private fun PackerFlow(o: com.sanathanatattva.partner.data.api.OrderCard, vm: OrderViewModel, leg: Leg, setLeg: (Leg) -> Unit, onBack: () -> Unit) {
    when (o.stage) {
        "rider_search" -> PackScreen(o, vm, forRider = true, onBack = onBack, onRiderCard = {})
        "rider_assigned" -> when {
            !o.packed -> PackScreen(o, vm, forRider = true, onBack = onBack, onRiderCard = { if (o.packed) setLeg(Leg.Main) })
            leg == Leg.Code -> EnterRiderCodeScreen(o, vm, returning = false, onBack = { setLeg(Leg.Main) })
            else -> RiderComingScreen(o, vm, RiderLeg.Coming, onBack, onEnterCode = { setLeg(Leg.Code) })
        }
        "picked_up" -> RiderComingScreen(o, vm, RiderLeg.Delivering, onBack, onEnterCode = {})
        "returning" -> if (leg == Leg.Code) EnterRiderCodeScreen(o, vm, returning = true, onBack = { setLeg(Leg.Main) })
        else RiderComingScreen(o, vm, RiderLeg.Returning, onBack, onEnterCode = { setLeg(Leg.Code) })
        else -> BackTomorrowScreen(o, onBack)
    }
}

@Composable
private fun RiderFlow(
    o: com.sanathanatattva.partner.data.api.OrderCard, vm: OrderViewModel,
    leg: Leg, setLeg: (Leg) -> Unit, otp: String, setOtp: (String) -> Unit,
    pickedAck: Boolean, clearPicked: () -> Unit, onBack: () -> Unit,
) {
    when {
        o.stage == "returning" -> ReturnToShopScreen(o, returned = false, onBack = onBack)
        o.stage == "rider_assigned" && leg == Leg.AtShop -> RiderPickupCodeScreen(o, vm, picked = false, onBack = { setLeg(Leg.Main) }, onGo = {})
        o.stage == "rider_assigned" -> RiderToShopScreen(o, vm, onBack, onArrived = { setLeg(Leg.AtShop) })
        o.stage == "picked_up" && pickedAck -> RiderPickupCodeScreen(o, vm, picked = true, onBack = clearPicked, onGo = clearPicked)
        o.stage == "picked_up" -> Delivering(o, vm, rider = true, leg, setLeg, otp, setOtp, onBack)
        else -> BackTomorrowScreen(o, onBack)
    }
}

/** On the way → customer's code → door photo, or → couldn't deliver. Same steps for shop and rider. */
@Composable
private fun Delivering(
    o: com.sanathanatattva.partner.data.api.OrderCard, vm: OrderViewModel, rider: Boolean,
    leg: Leg, setLeg: (Leg) -> Unit, otp: String, setOtp: (String) -> Unit, onBack: () -> Unit,
) {
    when (leg) {
        Leg.Code -> HandoverCodeScreen(o, vm, rider, onBack = { setLeg(Leg.Main) }, onCorrect = { setOtp(it); setLeg(Leg.Photo) })
        Leg.Photo -> DoorPhotoScreen(o, vm, otp, onBack = { setLeg(Leg.Code) }, onWrongCode = { setLeg(Leg.Code) })
        Leg.Fail -> FailReasonsScreen(o, vm, rider, onBack = { setLeg(Leg.Main) }, onSent = { setLeg(if (rider) Leg.Main else Leg.Sent) })
        else -> ToCustomerScreen(o, vm, rider, onBack, onArrived = { setLeg(Leg.Code) }, onFailed = { setLeg(Leg.Fail) })
    }
}
