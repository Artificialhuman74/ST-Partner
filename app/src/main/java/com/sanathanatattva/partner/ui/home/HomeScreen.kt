package com.sanathanatattva.partner.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.alerts.AlertCheck
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.PartnerHome
import com.sanathanatattva.partner.data.api.Place
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.SlideButton
import com.sanathanatattva.partner.ui.components.DockContentClearance
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.rupees
import com.sanathanatattva.partner.ui.components.delayed
import com.sanathanatattva.partner.ui.components.fill
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.login.dialSupport
import com.sanathanatattva.partner.ui.theme.AnekNarrow
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.delay
import com.sanathanatattva.partner.alerts.RingActivity
import com.sanathanatattva.partner.data.api.OrderCard

enum class HomeTab { Home, Earnings, Profile }

/** Home for traders and approved riders (canvas "Home · offline / online", "a permission got turned off", "Auto-offline"). */
@Composable
fun HomeScreen(
    onSignedOut: () -> Unit,
    onPermissions: () -> Unit,
    onTab: (HomeTab) -> Unit,
    onOpenOrder: (Long) -> Unit,
) {
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel()
    val session by context.appContainer.sessions.session.collectAsState(initial = context.appContainer.sessions.latest)
    var picking by rememberSaveable { mutableStateOf(false) }
    val signOutHaptics = LocalHapticFeedback.current
    LifecycleResumeEffect(Unit) {
        // Session expired: a double buzz as it goes back to login.
        vm.refresh { signOutHaptics.performHapticFeedback(HapticFeedbackType.Reject); onSignedOut() }
        // Came back from "Go online" on the alert checklist: straight to choosing a start place.
        val repo = context.appContainer.partner
        if (repo.wantsToGoOnline) {
            repo.wantsToGoOnline = false
            if (vm.home?.online != true) picking = true
        }
        onPauseOrDispose { }
    }
    // Keep the "near you" dot honest while online.
    LaunchedEffect(vm.home?.online) {
        while (vm.home?.online == true) {
            delay(20_000)
            vm.refreshHere()
        }
    }

    val home = vm.home
    val user = session?.user
    // Feel the switch: a confirm going online, a soft tick going offline, a buzz when it fails.
    val haptics = LocalHapticFeedback.current
    var wasOnline by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(home?.online) {
        val now = home?.online ?: return@LaunchedEffect
        val before = wasOnline
        if (before != null && before != now) haptics.performHapticFeedback(if (now) HapticFeedbackType.Confirm else HapticFeedbackType.ToggleOff)
        wasOnline = now
    }
    LaunchedEffect(vm.failed) { if (vm.failed) haptics.performHapticFeedback(HapticFeedbackType.Reject) }
    val readiness = vm.readiness

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp + DockContentClearance),
            ) {
                Header(
                    name = user?.firstName.orEmpty(),
                    subtitle = when {
                        user == null -> ""
                        user.isRider -> listOfNotNull(stringResource(R.string.home_partner_rider), user.vehiclePlate).joinToString(" · ")
                        else -> stringResource(R.string.home_partner_trader)
                    },
                    onSupport = { context.dialSupport() },
                )
                if (home?.online == true && !readiness.ready) {
                    Spacer(Modifier.height(14.dp))
                    PermissionWarning(readiness.missing.first(), onFix = onPermissions)
                }
                // Work in progress comes first: an order to answer, or one being delivered.
                vm.active?.let { work ->
                    work.offers.forEach { offer ->
                        Spacer(Modifier.height(12.dp))
                        ActiveWorkRow(
                            title = stringResource(R.string.home_offer_waiting),
                            body = offer.card.lines.joinToString(", ") { "${it.quantity} × ${it.size.orEmpty()} ${it.name}".replace("  ", " ") },
                            gold = true,
                        ) { context.startActivity(RingActivity.intent(context, offer.offerId.toString(), offer.kind == "rider", test = false)) }
                    }
                    work.orders.forEach { order ->
                        Spacer(Modifier.height(12.dp))
                        ActiveWorkRow(
                            title = stringResource(R.string.home_order_in_progress, order.orderNumber),
                            body = stringResource(activeLine(order)),
                            gold = false,
                        ) { onOpenOrder(order.orderId) }
                    }
                }
                Spacer(Modifier.height(if (home?.online == true && !readiness.ready) 12.dp else 18.dp))
                AnimatedContent(
                    targetState = home?.online == true,
                    transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
                    label = "online",
                ) { online ->
                    if (online) {
                        OnlineCard(
                            home = home,
                            here = vm.here,
                            compact = !readiness.ready,
                            busy = vm.busy,
                            onGoOffline = vm::goOffline,
                        )
                    } else {
                        // Home not loaded yet: load it first, so the slide never does nothing.
                        val openPicker = {
                            when {
                                !readiness.ready -> onPermissions()
                                vm.home == null -> vm.loadHomeThen { picking = true }
                                else -> { vm.refreshHere(); picking = true }
                            }
                        }
                        OfflineCard(
                            lastFrom = home?.startedFrom?.place ?: home?.places?.firstOrNull { it.isDefault },
                            liveLast = home?.startedFrom?.kind == "live",
                            busy = vm.busy,
                            onGoOnline = openPicker,
                            onChange = openPicker,
                        )
                    }
                }
                if (vm.failed) {
                    Text(stringResource(vm.failedMessage), color = St.Brick, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(12.dp))
                when {
                    home?.online == true && readiness.ready -> KeepNotificationTip()
                    home?.online != true -> AlertsRow(readiness.missing.size, onPermissions)
                }
                Spacer(Modifier.height(12.dp))
                TodayCard(home?.today?.deliveries ?: 0, home?.today?.earned ?: 0.0, gold = home?.online != true, onAll = { onTab(HomeTab.Earnings) })
            }
        }

        if (picking && home != null) {
            StartPlacePicker(
                places = home.places,
                // No fresh fix yet: the last position the phone reported still works for "here".
                here = vm.here ?: home.live?.let { it.lat to it.lng },
                busy = vm.busy,
                onClose = { picking = false },
                onPick = { start ->
                    vm.goOnline(start)
                    picking = false
                },
            )
        }
        val autoOff = home != null && !home.online && home.autoOfflineAt != null && !vm.autoOfflineDismissed
        if (autoOff) {
            AutoOfflineSheet(
                onGoOnline = {
                    vm.autoOfflineDismissed = true
                    when {
                        !readiness.ready -> onPermissions()
                        else -> { vm.refreshHere(); picking = true }
                    }
                },
                onNotNow = { vm.autoOfflineDismissed = true },
            )
        }
    }
}

@Composable
private fun Header(name: String, subtitle: String, onSupport: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(46.dp).border(2.5.dp, St.Green, CircleShape).padding(2.5.dp).clip(CircleShape).background(St.GreenMist),
            contentAlignment = Alignment.Center,
        ) { Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = St.Green) }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.home_greeting, name), style = MaterialTheme.typography.titleLarge, color = St.Ink)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = St.Ink2)
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(St.Sand).clickable(onClick = onSupport),
            contentAlignment = Alignment.Center,
        ) { Icon(StIcons.Headset, contentDescription = stringResource(R.string.call_support), tint = St.Green, modifier = Modifier.size(24.dp)) }
    }
}

private fun labelRes(label: String) = when (label) {
    "shop" -> R.string.label_shop
    "other" -> R.string.label_other
    else -> R.string.label_home
}

@Composable
private fun OfflineCard(lastFrom: Place?, liveLast: Boolean, busy: Boolean, onGoOnline: () -> Unit, onChange: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(St.Paper).border(2.dp, St.LineSoft, shape).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF9A9283)))
            Text(stringResource(R.string.home_offline), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp), color = St.Ink, modifier = Modifier.semantics { heading() })
        }
        Text(stringResource(R.string.home_offline_body), style = MaterialTheme.typography.bodyMedium, color = St.Ink2, modifier = Modifier.padding(start = 22.dp, top = 4.dp))
        Spacer(Modifier.height(18.dp))
        // "ho-knob" nudges 22px; "ho-chev" chevrons pulse in turn.
        SlideButton(
            label = stringResource(if (busy) R.string.home_going_online else R.string.home_go_online),
            onComplete = onGoOnline,
            knobIcon = StIcons.Power,
            height = 80.dp,
            track = St.Sand,
            border = St.Line,
            nudge = 22.dp,
            chevrons = !busy,
            enabled = !busy,
        )
        if (lastFrom != null || liveLast) {
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onChange),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(if (liveLast) StIcons.Locate else StIcons.Home, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
                val where = if (liveLast || lastFrom == null) stringResource(R.string.home_here)
                else stringResource(labelRes(lastFrom.label)) + " · " + lastFrom.address.substringBefore(',')
                Text(
                    text = stringResource(R.string.home_last_started) + " " + where,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    color = St.Ink,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                )
                Text(stringResource(R.string.home_change), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = St.Green)
            }
        }
    }
}

private val Drop = svgPath("M15 1C15 1 3 17 3 26a12 12 0 0 0 24 0C27 17 15 1 15 1Z")
private val Glint = svgPath("M10 21a2.6 5 0 1 0 0.01 0Z")

@Composable
private fun OnlineCard(home: PartnerHome?, here: Pair<Double, Double>?, compact: Boolean, busy: Boolean, onGoOffline: () -> Unit) {
    val reduce = LocalReduceMotion.current
    // "hn-in": the card settles up 10px over 0.8s.
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(800, easing = OilEase)) }
    val shape = RoundedCornerShape(28.dp)
    val place = home?.startedFrom?.place
    Column(
        Modifier.fillMaxWidth()
            .graphicsLayer { alpha = enter.value; translationY = 10.dp.toPx() * (1f - enter.value) }
            .clip(shape).background(St.GreenTint).border(2.dp, St.Green, shape).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LiveDot()
            Text(stringResource(R.string.home_online), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp), color = St.Green, modifier = Modifier.semantics { heading() })
        }
        Text(
            stringResource(R.string.home_waiting_near_you),
            style = MaterialTheme.typography.bodyMedium,
            color = St.Ink3,
            modifier = Modifier.padding(start = 22.dp, top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
        // Where orders come by: the live fix, else the place they started from.
        val center = here ?: home?.live?.let { it.lat to it.lng } ?: place?.let { it.latitude to it.longitude }
        NearYouMap(center, Modifier.padding(top = if (compact) 8.dp else 12.dp).fillMaxWidth().height(if (compact) 110.dp else 140.dp))
        Spacer(Modifier.height(8.dp))
        SlideButton(
            label = stringResource(if (busy) R.string.home_going_offline else R.string.home_go_offline),
            onComplete = onGoOffline,
            enabled = !busy,
            knobIcon = StIcons.Power,
            fromEnd = true,
            height = 80.dp,
            track = St.Cream,
            nudge = 0.dp,
        )
        Row(Modifier.padding(top = 12.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (place == null) StIcons.Locate else StIcons.Home, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
            val where = if (place == null) stringResource(R.string.home_here) else stringResource(labelRes(place.label))
            Text(stringResource(R.string.home_started_from) + " " + where, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp), color = St.Ink)
        }
    }
}

@androidx.annotation.StringRes
private fun activeLine(o: OrderCard): Int = when {
    o.role == "rider" && o.stage == "rider_assigned" -> R.string.active_rider_to_shop
    o.role == "rider" && o.stage == "picked_up" -> R.string.active_rider_to_customer
    o.role == "rider" && o.stage == "returning" -> R.string.active_rider_returning
    o.role == "packer" && !o.packed -> R.string.active_pack_for_rider
    o.role == "packer" && o.stage == "rider_assigned" -> R.string.active_rider_coming
    o.role == "packer" && o.stage == "returning" -> R.string.active_rider_returning_shop
    o.role == "packer" -> R.string.active_rider_delivering
    o.deliveryStatus == "out_for_delivery" -> R.string.active_on_the_way
    else -> R.string.active_pack
}

/** A tappable row for work in progress; gold when an order is still waiting for an answer. */
@Composable
private fun ActiveWorkRow(title: String, body: String, gold: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(20.dp)).background(if (gold) St.Gold else St.GreenTint)
            .border(2.dp, if (gold) St.Gold else St.Green, RoundedCornerShape(20.dp)).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(if (gold) StIcons.Bell else StIcons.Scooter, contentDescription = null, tint = if (gold) St.Ink else St.Green, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold), color = if (gold) St.Ink else St.Green)
            Text(body, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = if (gold) St.Ink else St.Ink3, maxLines = 2)
        }
        Icon(StIcons.ChevronRight, contentDescription = null, tint = if (gold) St.Ink else St.Green, modifier = Modifier.size(22.dp))
    }
}

/** "hn-live": the online dot breathes a 9dp ring every 1.8s. */
@Composable
private fun LiveDot() {
    val clock by rememberLoop(1800, still = 1f)
    Box(
        Modifier.size(12.dp).drawBehind {
            val p = (clock / 0.8f).coerceAtMost(1f)
            drawCircle(St.Green.copy(alpha = 0.5f * (1f - p)), radius = size.minDimension / 2f + 9.dp.toPx() * p)
        }.clip(CircleShape).background(St.Green),
    )
}

/** Gold, can't-miss warning when a setting got switched off while online ("pw-in"). */
@Composable
private fun PermissionWarning(missing: AlertCheck, onFix: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(700, easing = OilEase)) }
    val what = stringResource(
        when (missing) {
            AlertCheck.Notifications -> R.string.perm_notifications
            AlertCheck.Location -> R.string.perm_location
            AlertCheck.Battery -> R.string.perm_battery
            AlertCheck.FullScreen -> R.string.perm_full
            AlertCheck.DoNotDisturb -> R.string.perm_dnd
        },
    )
    Column(
        Modifier.fillMaxWidth()
            .graphicsLayer { alpha = enter.value; translationY = -12.dp.toPx() * (1f - enter.value) }
            .clip(RoundedCornerShape(24.dp)).background(St.Gold).padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(StIcons.Bell, contentDescription = null, tint = St.Ink, modifier = Modifier.size(30.dp))
            Column {
                Text(stringResource(R.string.perm_warning_title), style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink)
                Text(stringResource(R.string.perm_warning_body, what), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = St.Ink)
            }
        }
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(St.Ink).clickable(onClick = onFix),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.perm_warning_cta), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = St.Cream)
            Icon(StIcons.ArrowRight, contentDescription = null, tint = St.Cream, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun KeepNotificationTip() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(St.Gold).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(StIcons.Bell, contentDescription = null, tint = St.Ink, modifier = Modifier.size(22.dp))
        Text(stringResource(R.string.home_keep_notification), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = St.Ink)
    }
}

@Composable
private fun AlertsRow(missing: Int, onOpen: () -> Unit) {
    val ready = missing == 0
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(18.dp))
            .background(if (ready) St.GreenWash else St.Gold).clickable(onClick = onOpen).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(if (ready) StIcons.Bell else StIcons.PhoneRing, contentDescription = null, tint = if (ready) St.Green else St.Ink, modifier = Modifier.size(22.dp))
        Text(
            if (ready) stringResource(R.string.perm_all_on) else pluralStringResource(R.plurals.perm_left, missing, missing),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp, fontWeight = FontWeight(650)),
            color = if (ready) St.Green else St.Ink,
            modifier = Modifier.weight(1f),
        )
        Icon(StIcons.ChevronRight, contentDescription = null, tint = if (ready) St.Green else St.Ink, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TodayCard(deliveries: Int, earned: Double, gold: Boolean, onAll: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(if (gold) St.Paper else St.Beige).border(if (gold) 2.dp else 1.5.dp, if (gold) St.LineSoft else St.BeigeLine, shape).padding(horizontal = 18.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_today), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = St.Ink, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.home_all_earnings),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = St.Green,
                modifier = Modifier.heightIn(min = 40.dp).clickable(onClick = onAll).padding(vertical = 10.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(deliveries.toString(), stringResource(R.string.home_deliveries), St.Ink, Modifier.weight(1f))
            Stat(rupees(earned), stringResource(R.string.home_earned), if (gold) St.GoldInk else St.Green, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.displayMedium.copy(fontFamily = AnekNarrow, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold), color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = St.Ink2)
    }
}

