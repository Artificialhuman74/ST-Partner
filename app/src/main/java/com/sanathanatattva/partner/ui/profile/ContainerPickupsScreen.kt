package com.sanathanatattva.partner.ui.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.Pickup
import com.sanathanatattva.partner.ui.components.BackBar
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.StTextField
import com.sanathanatattva.partner.ui.components.box
import com.sanathanatattva.partner.ui.components.dashedBorder
import com.sanathanatattva.partner.ui.components.line
import com.sanathanatattva.partner.ui.components.rupees
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.login.UiText
import com.sanathanatattva.partner.ui.login.text
import com.sanathanatattva.partner.ui.login.toUiText
import com.sanathanatattva.partner.ui.signup.DocumentCamera
import com.sanathanatattva.partner.ui.signup.rememberThumbnail
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private enum class Method(val wire: String, val title: Int, val body: Int) {
    Credit("store_credit", R.string.pickup_credit, R.string.pickup_credit_body),
    Upi("manual_upi", R.string.pickup_upi, R.string.pickup_upi_body),
    Bank("manual_bank", R.string.pickup_bank, R.string.pickup_bank_body),
}

private sealed interface Sheet {
    data class Refund(val pickup: Pickup) : Sheet
    data class Damage(val pickup: Pickup) : Sheet
}

/** Empty can pickups (canvas "ContainerPickups", traders only): refund the deposit or keep it for a damaged can. */
@Composable
fun ContainerPickupsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = context.appContainer.partner
    val scope = rememberCoroutineScope()
    var pickups by remember { mutableStateOf<List<Pickup>?>(null) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    fun reload() = scope.launch { (repo.containerPickups() as? ApiResult.Success)?.let { pickups = it.value.pickups } }
    LaunchedEffect(Unit) { reload() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 22.dp)) {
            BackBar(stringResource(R.string.profile_pickups), onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                val list = pickups
                if (list != null) {
                    Text(
                        if (list.isEmpty()) stringResource(R.string.pickup_none) else pluralStringResource(R.plurals.pickup_title, list.size, list.size),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                        color = St.Ink,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp).semantics { heading() },
                    )
                    Text(
                        stringResource(if (list.isEmpty()) R.string.pickup_none_body else R.string.pickup_body),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = St.Ink2,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        list.forEach { p -> PickupCard(p, onRefund = { sheet = Sheet.Refund(p) }, onDamaged = { sheet = Sheet.Damage(p) }) }
                    }
                }
            }
        }
        when (val s = sheet) {
            is Sheet.Refund -> RefundSheet(s.pickup, onClose = { sheet = null }, onDone = { sheet = null; reload() })
            is Sheet.Damage -> DamageSheet(s.pickup, onClose = { sheet = null }, onDone = { sheet = null; reload() })
            null -> Unit
        }
    }
}

@Composable
private fun PickupCard(p: Pickup, onRefund: () -> Unit, onDamaged: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val area = p.customerAddress?.substringBeforeLast(',')?.trim().orEmpty()
    Column(
        Modifier.fillMaxWidth().clip(shape).background(St.Beige).border(1.5.dp, St.BeigeLine, shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmptyCan(Modifier.size(width = 48.dp, height = 52.dp))
            Column(Modifier.weight(1f)) {
                Text(p.customerName, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = St.Ink)
                if (area.isNotEmpty()) Text(area, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = St.Ink3, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.pickup_deposit), style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = St.Ink2)
                Text(rupees(p.deposit), style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip { Text(stringResource(R.string.pickup_can, litres(p.containerType)), style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = St.Ink) }
            Method.entries.firstOrNull { it.wire == p.destination }?.let { m ->
                Chip {
                    Text(
                        AnnotatedString.fromHtml(stringResource(R.string.pickup_asked_for, stringResource(m.title))),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = St.Ink3,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.weight(1.3f).height(50.dp).clip(RoundedCornerShape(16.dp)).background(St.Green).clickable(role = Role.Button, onClick = onRefund),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.pickup_fine), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.Cream, maxLines = 1)
            }
            Box(
                Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(16.dp)).background(St.Cream)
                    .border(2.dp, St.Danger, RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onDamaged),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.pickup_damaged), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.Danger, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Chip(content: @Composable () -> Unit) {
    Box(Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(St.Cream).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { content() }
}

/** "5L" → "5 L". */
private fun litres(type: String) = type.replace(Regex("(?i)\\s*l$"), " L")

/** The canvas's empty steel can with a dashed label patch. */
@Composable
private fun EmptyCan(modifier: Modifier) {
    val handle = remember { svgPath("M19 12q9-10 18 0") }
    Canvas(modifier) {
        viewBox(56f, 64f) {
            line(handle, St.Ink, 2.5f)
            box(8f, 11f, 40f, 50f, r = 7f, fill = St.Steel, stroke = St.Ink, strokeWidth = 2f)
            box(8f, 11f, 40f, 8f, r = 4f, fill = St.SteelDark, stroke = St.Ink, strokeWidth = 2f)
            drawRoundRect(
                St.FieldBorder, topLeft = Offset(18f, 27f), size = Size(22f, 20f), cornerRadius = CornerRadius(3f),
                style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))),
            )
        }
    }
}

/** "cp-sheet" + "cp-scrim": the sheet rises 80dp as the scrim fades in. */
@Composable
private fun PickupSheet(onClose: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onClose)
    val reduce = LocalReduceMotion.current
    val rise = remember { Animatable(if (reduce) 1f else 0f) }
    val scrim = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        launch { scrim.animateTo(1f, tween(300)) }
        rise.animateTo(1f, tween(500, easing = OilEase))
    }
    val close = stringResource(R.string.pickup_close)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = scrim.value }.background(St.Scrim)
                .clickable(onClick = onClose, indication = null, interactionSource = null).semantics { contentDescription = close },
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .graphicsLayer { translationY = 80.dp.toPx() * (1f - rise.value); alpha = 0.5f + 0.5f * rise.value }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(St.Cream)
                .clickable(enabled = false) {}
                .navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(St.Handle))
            content()
        }
    }
}

@Composable
private fun RefundSheet(p: Pickup, onClose: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val repo = context.appContainer.partner
    val scope = rememberCoroutineScope()
    var method by remember { mutableStateOf(Method.entries.firstOrNull { it.wire == p.destination } ?: Method.Credit) }
    var shot by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { shot = copyToCache(context, uri) }
    }
    PickupSheet(onClose) {
        Text(
            stringResource(R.string.pickup_refund_title, rupees(p.deposit), p.customerName),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
            color = St.Ink,
            modifier = Modifier.padding(top = 6.dp).semantics { heading() },
        )
        Method.entries.firstOrNull { it.wire == p.destination }?.let {
            Text(stringResource(R.string.pickup_they_asked, stringResource(it.title)), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = St.Ink2)
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Method.entries.forEach { m -> MethodOption(m, m == method) { method = m; error = null } }
        }
        if (method == Method.Upi) {
            val thumb = rememberThumbnail(shot?.path, 240)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).dashedBorder(2.5.dp, St.Dash, 16.dp)
                    .clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (thumb != null) {
                    Image(thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                } else {
                    Icon(StIcons.Image, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
                }
                Text(
                    stringResource(if (shot == null) R.string.pickup_add_shot else R.string.pickup_change_shot),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = St.Green,
                )
            }
        }
        error?.let { Text(text(it), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = St.Brick) }
        SheetButton(stringResource(R.string.pickup_refund_done), St.Green, busy) {
            if (method == Method.Upi && shot == null) {
                error = UiText.Res(R.string.pickup_need_shot)
                return@SheetButton
            }
            busy = true
            scope.launch {
                when (val r = repo.resolvePickup(p.id, "refunded", method.wire, null, if (method == Method.Upi) shot else null)) {
                    is ApiResult.Success -> { haptics.performHapticFeedback(HapticFeedbackType.Confirm); onDone() }
                    is ApiResult.Failure -> error = r.problem.toUiText()
                }
                busy = false
            }
        }
    }
}

@Composable
private fun MethodOption(m: Method, on: Boolean, onPick: () -> Unit) {
    val border by animateColorAsState(if (on) St.Green else St.BeigeLine, tween(250), label = "b")
    val bg by animateColorAsState(if (on) St.GreenWash else St.Beige, tween(250), label = "bg")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(18.dp)).background(bg).border(2.5.dp, border, RoundedCornerShape(18.dp))
            .selectable(selected = on, role = Role.RadioButton, onClick = onPick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(m.title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Ink)
            Text(stringResource(m.body), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 19.sp), color = St.Ink2)
        }
        Box(Modifier.size(24.dp).clip(CircleShape).border(2.5.dp, if (on) St.Green else St.FieldBorder, CircleShape), contentAlignment = Alignment.Center) {
            if (on) Box(Modifier.size(11.dp).clip(CircleShape).background(St.Green))
        }
    }
}

@Composable
private fun DamageSheet(p: Pickup, onClose: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val repo = context.appContainer.partner
    val scope = rememberCoroutineScope()
    var photo by remember { mutableStateOf<File?>(null) }
    var shooting by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    if (shooting) {
        DocumentCamera(
            hint = stringResource(R.string.pickup_damage_hint),
            newFile = { File(pickupDir(context), "${UUID.randomUUID()}.jpg") },
            onCaptured = { photo = it; shooting = false; error = null },
            onClose = { shooting = false },
        )
        return
    }
    PickupSheet(onClose) {
        Text(stringResource(R.string.pickup_damage_title), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp), color = St.Ink, modifier = Modifier.padding(top = 6.dp).semantics { heading() })
        Text(
            AnnotatedString.fromHtml(stringResource(R.string.pickup_damage_warning)),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
            color = St.DangerInk,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(St.DangerWash)
                .border(1.5.dp, St.DangerLine, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        )
        val thumb = rememberThumbnail(photo?.path, 720)
        Box(
            Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(18.dp)).dashedBorder(2.5.dp, St.Dash, 18.dp)
                .clickable { shooting = true },
            contentAlignment = Alignment.Center,
        ) {
            if (thumb != null) {
                Image(thumb, contentDescription = stringResource(R.string.pickup_damage_photo), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(StIcons.Camera, contentDescription = null, tint = St.Green, modifier = Modifier.size(32.dp))
                    Text(stringResource(R.string.pickup_take_photo), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.Green)
                }
            }
        }
        StTextField(
            value = note,
            onValueChange = { note = it.take(200) },
            label = stringResource(R.string.pickup_whats_wrong),
            helper = stringResource(R.string.pickup_whats_wrong_hint),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        error?.let { Text(text(it), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = St.Brick) }
        SheetButton(stringResource(R.string.pickup_keep_deposit), St.Danger, busy) {
            if (photo == null) {
                error = UiText.Res(R.string.pickup_need_photo)
                return@SheetButton
            }
            busy = true
            scope.launch {
                when (val r = repo.resolvePickup(p.id, "forfeited", null, note, photo)) {
                    is ApiResult.Success -> { haptics.performHapticFeedback(HapticFeedbackType.Confirm); onDone() }
                    is ApiResult.Failure -> error = r.problem.toUiText()
                }
                busy = false
            }
        }
    }
}

@Composable
private fun SheetButton(label: String, color: Color, busy: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(20.dp)).background(color).clickable(enabled = !busy, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) CircularProgressIndicator(color = St.Cream, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
        else Text(label, style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold), color = St.Cream)
    }
}

private fun pickupDir(context: Context) = File(context.cacheDir, "pickups").apply { mkdirs() }

private suspend fun copyToCache(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
    runCatching {
        val out = File(pickupDir(context), "${UUID.randomUUID()}.img")
        context.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } } ?: return@runCatching null
        out
    }.getOrNull()
}
