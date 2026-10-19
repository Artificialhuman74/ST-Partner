package com.sanathanatattva.partner.ui.permissions

import android.Manifest
import android.content.ActivityNotFoundException
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.alerts.AlertCheck
import com.sanathanatattva.partner.alerts.AlertReadiness
import com.sanathanatattva.partner.alerts.Autostart
import com.sanathanatattva.partner.alerts.OrderChannel
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiProblem
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.line
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.components.around
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.launch
import com.sanathanatattva.partner.alerts.OnlineService
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

private data class PermRow(val check: AlertCheck, val icon: ImageVector, val title: Int, val why: Int)

private val rows = listOf(
    PermRow(AlertCheck.Notifications, StIcons.Bell, R.string.perm_notifications, R.string.perm_notifications_why),
    PermRow(AlertCheck.Location, StIcons.Pin, R.string.perm_location, R.string.perm_location_why),
    PermRow(AlertCheck.Battery, StIcons.Battery, R.string.perm_battery, R.string.perm_battery_why),
    PermRow(AlertCheck.FullScreen, StIcons.PhoneRing, R.string.perm_full, R.string.perm_full_why),
    PermRow(AlertCheck.DoNotDisturb, StIcons.Moon, R.string.perm_dnd, R.string.perm_dnd_why),
)

/** "Set up alerts" (canvas "Permission checklist"): Go online stays locked until all five are on. */
@Composable
fun PermGateScreen(onBack: () -> Unit, onReady: (alreadyOnline: Boolean) -> Unit, onAutostartGuide: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var readiness by remember { mutableStateOf(AlertReadiness.check(context)) }
    var note by remember { mutableStateOf<String?>(null) }
    // Opened from Profile while online, the button just confirms instead of offering to go online again.
    var online by remember { mutableStateOf(context.appContainer.partner.lastHome?.online == true || OnlineService.running.value) }
    LaunchedEffect(Unit) {
        (context.appContainer.partner.home() as? ApiResult.Success)?.let { online = it.value.online }
    }
    fun refresh() {
        readiness = AlertReadiness.check(context)
        OrderChannel.ensure(context)
    }
    // Settings are changed outside the app: re-check every time the screen comes back.
    LifecycleResumeEffect(Unit) { refresh(); onPauseOrDispose { } }
    val haptics = LocalHapticFeedback.current
    var lastLeft by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(readiness.missing.size) {
        val now = readiness.missing.size
        val before = lastLeft
        if (before != null && now < before) haptics.performHapticFeedback(if (now == 0) HapticFeedbackType.Confirm else HapticFeedbackType.ToggleOn)
        lastLeft = now
    }

    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refresh()
        if (granted) scope.launch { context.appContainer.push.register() }
        else context.startActivity(AlertReadiness.settingsIntent(context, AlertCheck.Notifications))
    }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        refresh()
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] != true) {
            context.startActivity(AlertReadiness.settingsIntent(context, AlertCheck.Location))
        }
    }
    fun turnOn(check: AlertCheck) {
        when {
            check == AlertCheck.Notifications && Build.VERSION.SDK_INT >= 33 -> askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
            check == AlertCheck.Location -> askLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            else -> try {
                context.startActivity(AlertReadiness.settingsIntent(context, check))
            } catch (_: ActivityNotFoundException) {
                context.startActivity(AlertReadiness.settingsIntent(context, AlertCheck.Location))
            }
        }
    }

    val left = readiness.missing.size
    val guide = remember { Autostart.guideFor() }
    val testSent = stringResource(R.string.perm_test_sent)
    val testFailed = stringResource(R.string.error_network)

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).graphicsLayer { translationX = -12.dp.toPx() }) {
                Icon(StIcons.ArrowLeft, contentDescription = stringResource(R.string.signup_back), tint = St.Ink, modifier = Modifier.size(26.dp))
            }
            Text(stringResource(R.string.perm_title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = St.Ink, modifier = Modifier.weight(1f).graphicsLayer { translationX = -12.dp.toPx() })
            Text(
                stringResource(R.string.perm_count, 5 - left),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = if (left == 0) St.Green else St.GoldInk,
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                if (left == 0) stringResource(if (online) R.string.perm_ready_online else R.string.perm_ready) else pluralStringResource(R.plurals.perm_left, left, left),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                color = St.Ink,
                modifier = Modifier.padding(top = 14.dp).semantics { heading() },
            )
            Text(stringResource(R.string.perm_body), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp), color = St.Ink2, modifier = Modifier.padding(top = 6.dp))
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    PermCard(
                        row = row,
                        on = row.check in readiness.on,
                        onTurnOn = { turnOn(row.check) },
                        autostart = if (row.check == AlertCheck.Battery && guide != null && !Autostart.confirmed(context)) guide.brand else null,
                        onAutostart = onAutostartGuide,
                    )
                }
            }
            if (left == 0) {
                TextButton(
                    onClick = {
                        scope.launch {
                            // Make sure the server has this phone's current push token before ringing it.
                            context.appContainer.push.register()
                            note = when (val r = context.appContainer.partner.testAlert()) {
                                is ApiResult.Success -> testSent
                                is ApiResult.Failure -> (r.problem as? ApiProblem.Server)?.message ?: testFailed
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp),
                ) {
                    Icon(StIcons.PhoneRing, contentDescription = null, tint = St.Green, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.perm_test), style = MaterialTheme.typography.labelMedium, color = St.Green)
                }
                note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = St.Ink2, modifier = Modifier.padding(start = 12.dp)) }
            }
            Spacer(Modifier.height(16.dp))
        }
        GoButton(ready = left == 0, online = online, onClick = { onReady(online) })
    }
}

@Composable
private fun PermCard(row: PermRow, on: Boolean, onTurnOn: () -> Unit, autostart: String?, onAutostart: () -> Unit) {
    // "pg-card": colours ease over 0.4s when a setting turns on.
    val border by animateColorAsState(if (on) Color(0xFFB9D3B4) else St.LineSoft, tween(400, easing = OilEase), label = "b")
    val bg by animateColorAsState(if (on) Color(0xFFF3F8F1) else St.Paper, tween(400, easing = OilEase), label = "bg")
    val tile by animateColorAsState(if (on) Color(0xFFDCEBD7) else St.Sand, tween(400, easing = OilEase), label = "t")
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(bg).border(2.dp, border, shape).padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(tile), contentAlignment = Alignment.Center) {
                Icon(row.icon, contentDescription = null, tint = if (on) St.Green else St.GoldInk, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(row.title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = St.Ink)
                Text(stringResource(row.why), style = MaterialTheme.typography.bodySmall, color = St.Ink2)
            }
            AnimatedContent(
                targetState = on,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "state",
            ) { isOn ->
                if (isOn) {
                    Row(Modifier.heightIn(min = 48.dp).padding(start = 8.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tick()
                        Text(stringResource(R.string.perm_on), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Green)
                    }
                } else {
                    Button(
                        onClick = onTurnOn,
                        colors = ButtonDefaults.buttonColors(containerColor = St.Green, contentColor = St.Cream),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(48.dp),
                    ) { Text(stringResource(R.string.perm_turn_on), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold)) }
                }
            }
        }
        if (autostart != null) {
            Row(
                Modifier.padding(start = 58.dp).fillMaxWidth().heightIn(min = 40.dp).clip(RoundedCornerShape(12.dp)).background(St.Gold).clickable(onClick = onAutostart).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(StIcons.Phone2, contentDescription = null, tint = St.Ink, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.perm_autostart, autostart), style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = St.Ink, modifier = Modifier.weight(1f))
                Icon(StIcons.ChevronRight, contentDescription = null, tint = St.Ink, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** "pg-tick": pops in from 0.3x, turned -30°, over 0.55s. */
@Composable
private fun Tick() {
    val reduce = LocalReduceMotion.current
    val p = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(550, easing = OilEase)) }
    Box(
        Modifier.size(30.dp).graphicsLayer {
            alpha = p.value
            scaleX = 0.3f + 0.7f * p.value
            scaleY = scaleX
            rotationZ = -30f * (1f - p.value)
        }.clip(CircleShape).background(St.Green),
        contentAlignment = Alignment.Center,
    ) { Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(16.dp)) }
}

private val LockBody = svgPath("M7 10.5h10a3 3 0 0 1 3 3v4.5a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3v-4.5a3 3 0 0 1 3-3z")
private val Shackle = svgPath("M8 10.5V7.5a4 4 0 0 1 8 0v3")

/** Locked (sand) until ready; then it turns green, the padlock opens ("pg-open") and it glows ("pg-glow"). */
@Composable
private fun GoButton(ready: Boolean, online: Boolean, onClick: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val bg by animateColorAsState(if (ready) St.Green else Color(0xFFE3DAC8), tween(500, easing = OilEase), label = "go")
    val ink by animateColorAsState(if (ready) St.Cream else Color(0xFF5F5A4C), tween(500, easing = OilEase), label = "ink")
    val open = remember { Animatable(0f) }
    LaunchedEffect(ready) { if (!ready) open.snapTo(0f) }
    val glow by rememberLoop(2200, still = 1f)
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .drawBehind {
                if (ready && !reduce) {
                    val p = OilEase.transform((glow / 0.7f).coerceAtMost(1f))
                    val spread = 14.dp.toPx() * p
                    drawRoundRect(
                        St.Green.copy(alpha = 0.45f * (1f - p)),
                        topLeft = Offset(-spread, -spread),
                        size = Size(size.width + spread * 2, size.height + spread * 2),
                        cornerRadius = CornerRadius(20.dp.toPx() + spread),
                    )
                }
            }
            .clip(shape)
            .background(bg)
            .clickable(enabled = ready, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // The shackle swings open ("pg-open"), then the lock gives way to the power icon:
            // a ready button should say "go", not "lock".
            val handOff = remember { Animatable(if (ready && reduce) 1f else 0f) }
            LaunchedEffect(ready) {
                if (ready) { open.animateTo(1f, tween(700, delayMillis = 150, easing = OilEase)); handOff.animateTo(1f, tween(320, easing = OilEase)) }
                else handOff.snapTo(0f)
            }
            Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                if (handOff.value < 1f) {
                    Canvas(Modifier.size(24.dp).graphicsLayer { alpha = 1f - handOff.value; scaleX = 1f - 0.3f * handOff.value; scaleY = scaleX }) {
                        viewBox(24f, 24f) {
                            line(LockBody, ink, 2.4f)
                            // CSS transform-origin 85% 100% of the shackle's box (x 8–16, y 3.5–10.5).
                            around(14.8f, 10.5f, rotate = -28f * open.value) {
                                translate(0f, -3f * open.value) { line(Shackle, ink, 2.4f) }
                            }
                        }
                    }
                }
                if (handOff.value > 0f) {
                    Icon(
                        StIcons.Power, contentDescription = null, tint = ink,
                        modifier = Modifier.size(26.dp).graphicsLayer { alpha = handOff.value; scaleX = 0.6f + 0.4f * handOff.value; scaleY = scaleX },
                    )
                }
            }
            Text(
                stringResource(if (!ready) R.string.perm_locked else if (online) R.string.perm_done_online else R.string.perm_go),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 21.sp),
                color = ink,
            )
        }
    }
}
