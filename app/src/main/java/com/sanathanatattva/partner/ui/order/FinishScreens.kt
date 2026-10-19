package com.sanathanatattva.partner.ui.order

import com.sanathanatattva.partner.ui.components.ShutterButton
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.data.api.OrderCard
import com.sanathanatattva.partner.ui.components.BackBar
import com.sanathanatattva.partner.ui.components.CameraCapture
import com.sanathanatattva.partner.ui.components.CameraGuide
import com.sanathanatattva.partner.ui.components.CameraTrigger
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.rupees
import com.sanathanatattva.partner.ui.components.seg
import com.sanathanatattva.partner.ui.login.text
import com.sanathanatattva.partner.ui.signup.rememberThumbnail
import com.sanathanatattva.partner.ui.theme.AnekNarrow
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/** Page frame shared by every order screen: header, scrolling body, the big action pinned below. */
@Composable
fun OrderPage(
    title: String,
    onBack: () -> Unit,
    chip: (@Composable () -> Unit)? = null,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(St.Cream).windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)) {
        BackBar(title, onBack, trailing = chip)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), content = bottom)
    }
}

/* ── The customer's code ───────────────────────────────────────────────── */

/** Canvas "HandoverCode": 6 numbers from the customer's ST app or email. */
@Composable
fun HandoverCodeScreen(o: OrderCard, vm: OrderViewModel, rider: Boolean, onBack: () -> Unit, onCorrect: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    var ok by remember { mutableStateOf(false) }
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
    // Check as soon as all 6 are in, like the canvas's instant "Code is correct".
    LaunchedEffect(code) {
        ok = false
        if (code.length == 6) vm.checkCode(code) { right ->
            if (right) { ok = true; haptics.performHapticFeedback(HapticFeedbackType.Confirm) } else wrong++
        }
    }
    OrderPage(
        title = stringResource(R.string.order_title, o.orderNumber),
        onBack = onBack,
        bottom = {
            OrderAction(stringResource(R.string.code_next_photo), onClick = { onCorrect(code) }, enabled = ok, leading = StIcons.Camera, loading = vm.busy)
        },
    ) {
        if (rider) Steps(listOf(stringResource(R.string.step_at_shop), stringResource(R.string.step_picked_up), stringResource(R.string.step_hand_over)), current = 2, tickDone = false)
        else Steps(listOf(stringResource(R.string.step_packed), stringResource(R.string.step_on_way), stringResource(R.string.step_hand_over)), current = 2, tickDone = false)
        OrderHeading(stringResource(R.string.code_ask, o.customer.firstName))
        OrderBody(stringResource(R.string.code_ask_body))
        CodeBoxes(code, 6, Modifier.padding(top = 4.dp).graphicsLayer { translationX = shake.value.dp.toPx() }, height = 64.dp, gap = 8.dp, font = 30)
        Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).semantics { liveRegion = LiveRegionMode.Polite }, contentAlignment = Alignment.Center) {
            when {
                ok -> PickedUpStatus(stringResource(R.string.code_correct))
                vm.error != null && code.length < 6 -> Text(text(vm.error!!), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = St.Brick)
                vm.resent -> Text(stringResource(R.string.code_resent), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = St.Green)
                else -> Row(
                    Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { vm.resendCode() }.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(StIcons.Refresh, contentDescription = null, tint = St.Green, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.code_resend), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.Green)
                }
            }
        }
        NumPad(
            onDigit = { if (code.length < 6) { code += it; vm.error = null } },
            onClear = { code = "" },
            onDelete = { code = code.dropLast(1) },
            keyHeight = 62.dp,
        )
    }
}

/* ── Photo at the door ─────────────────────────────────────────────────── */

/** Canvas "DoorPhoto": the cans at the door as proof, then Done · delivered. */
@Composable
fun DoorPhotoScreen(o: OrderCard, vm: OrderViewModel, otp: String, onBack: () -> Unit, onWrongCode: () -> Unit) {
    val context = LocalContext.current
    val reduce = LocalReduceMotion.current
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    val trigger = remember { CameraTrigger() }
    val haptics = LocalHapticFeedback.current
    OrderPage(
        title = stringResource(R.string.order_title, o.orderNumber),
        onBack = onBack,
        chip = { GreenChip(stringResource(R.string.code_ok_chip), tick = true) },
        bottom = {
            if (photo == null) {
                ShutterButton(trigger, stringResource(R.string.door_take))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlineAction(stringResource(R.string.door_retake), StIcons.Camera, Modifier.weight(1f).height(64.dp)) { photo = null }
                    Box(Modifier.weight(2f)) {
                        OrderAction(
                            stringResource(R.string.door_done), leading = StIcons.Check, loading = vm.busy,
                            onClick = { photo?.let { vm.deliver(otp, File(it), onWrongCode) } },
                        )
                    }
                }
            }
        },
    ) {
        OrderHeading(stringResource(R.string.door_title))
        OrderBody(stringResource(R.string.door_body, o.customer.firstName))
        Box(Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xFF2A2F2B))) {
            val shot = photo
            if (shot == null) {
                CameraCapture(
                    front = false,
                    guide = CameraGuide.Corners,
                    hint = stringResource(R.string.door_frame_hint),
                    newFile = { File(File(context.cacheDir, "doors").apply { mkdirs() }, "${UUID.randomUUID()}.jpg") },
                    onCaptured = { photo = it.path },
                    modifier = Modifier.fillMaxSize(),
                    trigger = trigger,
                )
            } else {
                TakenPhoto(shot)
            }
        }
        vm.error?.let { Text(text(it), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = St.Brick) }
    }
}

/** "dp-flash" white flash, "dp-shot" settle from 104%, then "dp-saved" drops in. */
@Composable
private fun TakenPhoto(path: String) {
    val reduce = LocalReduceMotion.current
    val image = rememberThumbnail(path, 1200)
    val flash = remember { Animatable(if (reduce) 0f else 0.95f) }
    val settle = remember { Animatable(if (reduce) 1f else 0f) }
    val saved = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(path) {
        if (reduce) return@LaunchedEffect
        coroutineScope {
            launch { flash.animateTo(0f, tween(700)) }
            launch { settle.animateTo(1f, tween(600, easing = OilEase)) }
            launch { delay(350); saved.animateTo(1f, tween(500, easing = OilEase)) }
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (image != null) {
            Image(image, contentDescription = stringResource(R.string.door_photo_desc), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.04f - 0.04f * settle.value; scaleY = scaleX })
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = flash.value }.background(Color.White))
        Row(
            Modifier.padding(12.dp).graphicsLayer { alpha = saved.value; translationY = -8.dp.toPx() * (1f - saved.value) }
                .height(38.dp).clip(RoundedCornerShape(19.dp)).background(St.Green).padding(start = 10.dp, end = 14.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.door_saved), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold), color = St.Cream)
        }
    }
}

/* ── Couldn't deliver ──────────────────────────────────────────────────── */

private data class Reason(val wire: String, val label: Int, val icon: ImageVector)

private val reasons = listOf(
    Reason("not_home", R.string.fail_not_home, StIcons.NotHome),
    Reason("phone_off", R.string.fail_phone_off, StIcons.PhoneOff),
    Reason("wrong_address", R.string.fail_wrong_address, StIcons.PinUnsure),
    Reason("refused", R.string.fail_refused, StIcons.Ban),
    Reason("other", R.string.fail_other, StIcons.Dots),
)

/** Canvas "FailReasons": try calling first, then pick what happened. */
@Composable
fun FailReasonsScreen(o: OrderCard, vm: OrderViewModel, rider: Boolean, onBack: () -> Unit, onSent: () -> Unit) {
    val context = LocalContext.current
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    OrderPage(
        title = stringResource(R.string.order_title, o.orderNumber),
        onBack = onBack,
        bottom = {
            OrderAction(
                stringResource(if (picked == null) R.string.fail_pick else if (rider) R.string.fail_return else R.string.fail_keep),
                onClick = { picked?.let { vm.fail(it, null, onSent) } },
                enabled = picked != null,
                leading = StIcons.ArrowLeft,
                loading = vm.busy,
            )
        },
    ) {
        OrderHeading(stringResource(R.string.fail_title))
        OrderBody(stringResource(if (rider) R.string.fail_body_rider else R.string.fail_body_shop))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(18.dp)).background(St.GreenTint)
                .clickable(role = Role.Button) { vm.callCustomer { dial(context, it) } }.padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(StIcons.Phone, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
            Text(stringResource(R.string.fail_call_first, o.customer.firstName), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Green, modifier = Modifier.weight(1f))
            Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(St.Green).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.fail_call), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.Cream)
            }
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            reasons.forEach { r -> ReasonOption(r, picked == r.wire) { picked = r.wire } }
        }
        vm.error?.let { Text(text(it), style = MaterialTheme.typography.bodyMedium, color = St.Brick) }
    }
}

@Composable
private fun ReasonOption(r: Reason, on: Boolean, onPick: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val border by animateColorAsState(if (on) St.Green else St.BeigeLine, tween(300, easing = OilEase), label = "b")
    val bg by animateColorAsState(if (on) St.GreenWash else St.Beige, tween(300, easing = OilEase), label = "bg")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(18.dp)).background(bg).border(2.5.dp, border, RoundedCornerShape(18.dp))
            .selectable(selected = on, role = Role.RadioButton, onClick = onPick).padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Color(0xFFDCEBD7) else St.Cream), contentAlignment = Alignment.Center) {
            Icon(r.icon, contentDescription = null, tint = St.Green, modifier = Modifier.size(24.dp))
        }
        Text(stringResource(r.label), style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold), color = St.Ink, modifier = Modifier.weight(1f))
        Box(Modifier.size(26.dp).clip(CircleShape).border(2.5.dp, if (on) St.Green else St.FieldBorder, CircleShape), contentAlignment = Alignment.Center) {
            if (on) {
                val p = remember { Animatable(if (reduce) 1f else 0f) }
                LaunchedEffect(Unit) { p.animateTo(1f, tween(400, easing = OilEase)) }
                Box(Modifier.size(12.dp).graphicsLayer { scaleX = p.value; scaleY = p.value }.clip(CircleShape).background(St.Green))
            }
        }
    }
}

/* ── Delivered! ────────────────────────────────────────────────────────── */

private val FloodEase = CubicBezierEasing(0.3f, 0.1f, 0.2f, 1f)
private val SplashEase = CubicBezierEasing(0.33f, 0f, 0.4f, 1f)
private val RayEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val OdoEase = CubicBezierEasing(0.3f, 0f, 0.15f, 1f)

/**
 * Canvas "Delivered": gold oil floods up from the bottom with a wave and splashes, the badge
 * pops with a ring and rays, the tick draws itself, and the earnings roll up like an odometer.
 * Plays once (the canvas loops it), then holds.
 */
@Composable
fun DeliveredScreen(customer: String, earned: Double, trader: Boolean, todayDeliveries: Int?, todayEarned: Double?, onDone: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val haptics = LocalHapticFeedback.current
    val clock = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        // 0..0.9 of the canvas's 6.4 s loop: everything lands, nothing fades away.
        clock.animateTo(0.9f, tween(5760, easing = LinearEasing))
    }
    val t = if (reduce) 0.9f else clock.value
    LaunchedEffect(t >= 0.3f) { if (t >= 0.3f && !reduce) haptics.performHapticFeedback(HapticFeedbackType.Confirm) }
    val wave by rememberLoop(2400)

    Box(Modifier.fillMaxSize().background(St.Cream)) {
        // "dv-flood" + "dv-wave" + "dv-splash"
        val flood = seg(t, 0f, 0.28f, FloodEase)
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(306.dp).graphicsLayer { translationY = size.height * (1f - flood) }) {
            Box(Modifier.fillMaxSize().background(St.Gold))
            Canvas(Modifier.fillMaxWidth().height(22.dp).offset(y = (-20).dp)) {
                val w = 48.75.dp.toPx()
                val shift = if (reduce) 0f else -size.width * wave
                val path = Path().apply {
                    moveTo(shift, 12.dp.toPx())
                    var x = shift
                    while (x < size.width * 2) {
                        quadraticTo(x + w / 2, if (((x - shift) / w).toInt() % 2 == 0) 2.dp.toPx() else 22.dp.toPx(), x + w, 12.dp.toPx())
                        x += w
                    }
                    lineTo(x, size.height); lineTo(shift, size.height); close()
                }
                drawPath(path, St.Gold)
            }
            listOf(70f to 0f, 150f to 0.12f, 236f to 0.22f, 318f to 0.12f).forEachIndexed { i, (left, delayFrac) ->
                val d = delayFrac / 6.4f
                val p = t - d
                val a = kf(p, 0.24f to 0f, 0.27f to 1f, 0.42f to 0f)
                val y = kf(p, 0.24f to 0f, 0.33f to -54f, 0.42f to 4f, easing = SplashEase)
                Box(
                    Modifier.offset(x = left.dp, y = (-9).dp).size(width = if (i % 2 == 0) 9.dp else 7.dp, height = if (i % 2 == 0) 12.dp else 10.dp)
                        .graphicsLayer { alpha = a; translationY = y.dp.toPx() }.clip(DropShape).background(St.Gold),
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                // "dv-ring"
                val ringA = kf(t, 0.28f to 0f, 0.31f to 0.9f, 0.6f to 0f, easing = OilEase)
                val ringS = kf(t, 0.28f to 0.9f, 0.31f to 0.95f, 0.6f to 1.7f, easing = OilEase)
                Box(Modifier.size(150.dp).graphicsLayer { alpha = ringA; scaleX = ringS; scaleY = ringS }.border(4.dp, St.Gold, CircleShape))
                // "dv-ray" ×8
                Canvas(Modifier.size(150.dp)) {
                    val p = seg(t, 0.30f, 0.56f, LinearEasing)
                    val travel = if (p < 3f / 26f) RayEase.transform(p / (3f / 26f)) * 82f else 82f + 40f * RayEase.transform((p - 3f / 26f) / (23f / 26f))
                    val alpha = if (t < 0.30f || t > 0.56f) 0f else if (p < 3f / 26f) p / (3f / 26f) else 1f - (p - 3f / 26f) / (23f / 26f)
                    val s = if (p < 3f / 26f) 0.6f + 0.4f * (p / (3f / 26f)) else 1f - 0.4f * ((p - 3f / 26f) / (23f / 26f))
                    for (k in 0 until 8) {
                        rotate(45f * k, center) {
                            val gold = k % 2 == 0
                            val w = (if (gold) 10.dp else 8.dp).toPx() * s
                            val h = (if (gold) 14.dp else 12.dp).toPx() * s
                            drawOval(if (gold) St.Gold else St.Green, topLeft = Offset(center.x - w / 2, center.y - travel.dp.toPx() - h / 2), size = Size(w, h), alpha = alpha.coerceIn(0f, 1f))
                        }
                    }
                }
                // "dv-badge" + "dv-check"
                val badge = seg(t, 0.22f, 0.32f)
                Box(
                    Modifier.size(128.dp).graphicsLayer { alpha = badge; scaleX = 0.3f + 0.7f * badge; scaleY = scaleX }
                        .shadow(18.dp, CircleShape, ambientColor = St.GreenDeep, spotColor = St.GreenDeep)
                        .clip(CircleShape).background(St.Green).border(6.dp, St.Gold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val draw = seg(t, 0.30f, 0.44f)
                    Canvas(Modifier.size(70.dp)) {
                        val sx = size.width / 70f
                        val tick = Path().apply { moveTo(17f * sx, 36f * sx); lineTo(29f * sx, 48f * sx); lineTo(54f * sx, 21f * sx) }
                        val m = PathMeasure().apply { setPath(tick, false) }
                        val part = Path()
                        m.getSegment(0f, m.length * draw, part, true)
                        drawPath(part, St.Cream, style = Stroke(9f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
            val text1 = seg(t, 0.34f, 0.46f)
            val text2 = seg(t, 0.38f, 0.50f)
            Text(
                stringResource(R.string.delivered_title),
                style = MaterialTheme.typography.displayMedium.copy(fontFamily = AnekNarrow, fontSize = 46.sp, lineHeight = 46.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.015).em),
                color = St.Ink,
                modifier = Modifier.padding(top = 20.dp).graphicsLayer { alpha = text1; translationY = 16.dp.toPx() * (1f - text1) }.semantics { heading() },
            )
            Text(
                stringResource(R.string.delivered_body, customer),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp),
                color = St.Ink3,
                modifier = Modifier.padding(top = 10.dp, start = 24.dp, end = 24.dp).graphicsLayer { alpha = text2; translationY = 12.dp.toPx() * (1f - text2) },
            )
            val earn = seg(t, 0.40f, 0.50f)
            Column(
                Modifier.padding(top = 22.dp).graphicsLayer { alpha = earn; scaleX = 0.9f + 0.1f * earn; scaleY = scaleX }
                    .clip(RoundedCornerShape(24.dp)).background(St.GreenTint).border(2.dp, St.Green, RoundedCornerShape(24.dp))
                    .padding(start = 26.dp, end = 26.dp, top = 10.dp, bottom = 12.dp)
                    .semantics(mergeDescendants = true) { },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(if (trader) R.string.delivered_commission else R.string.delivered_earned), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = St.Ink3)
                Odometer(earned, seg(t, 0.44f, 0.64f, OdoEase))
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val today = seg(t, 0.50f, 0.60f)
            if (todayDeliveries != null && todayEarned != null) {
                Row(
                    Modifier.fillMaxWidth().graphicsLayer { alpha = today; translationY = 10.dp.toPx() * (1f - today) }
                        .clip(RoundedCornerShape(20.dp)).background(St.Cream).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.home_today), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = St.Ink2)
                        Text(pluralStringResource(R.plurals.earnings_deliveries, todayDeliveries, todayDeliveries), style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink)
                    }
                    Text(rupees(todayEarned), style = MaterialTheme.typography.headlineMedium.copy(fontFamily = AnekNarrow, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold), color = St.GoldInk)
                }
            }
            OrderAction(stringResource(R.string.order_back_to_orders), onClick = onDone, trailing = StIcons.ArrowRight)
        }
    }
}

private val DropShape = RoundedCornerShape(percent = 50)

/** "+₹45" where each digit rolls up from 0 to its value ("od-col"). */
@Composable
private fun Odometer(amount: Double, p: Float) {
    val whole = amount.toLong().toString()
    val desc = rupees(amount)
    Row(Modifier.height(72.dp).semantics { contentDescription = desc }, verticalAlignment = Alignment.Top) {
        val style = MaterialTheme.typography.displayLarge.copy(fontFamily = AnekNarrow, fontSize = 64.sp, lineHeight = 72.sp, fontWeight = FontWeight.ExtraBold)
        Text("+₹", style = style, color = St.Green)
        whole.forEach { ch ->
            val d = ch.digitToInt()
            Box(Modifier.height(72.dp).clip(RoundedCornerShape(0.dp))) {
                Column(Modifier.graphicsLayer { translationY = -72.dp.toPx() * d * p }) {
                    for (n in 0..9) Text(n.toString(), style = style, color = St.Green, modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}
