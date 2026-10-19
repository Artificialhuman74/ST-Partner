package com.sanathanatattva.partner.ui.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
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
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.ui.components.BackBar
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.box
import com.sanathanatattva.partner.ui.components.dot
import com.sanathanatattva.partner.ui.components.line
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.rupees
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.launch

private const val HOLD_MS = 2000

/** "Also take rider jobs" (canvas "RiderJobsOptIn"): plain explanation, then a 2-second press-and-hold. A tap does nothing. */
@Composable
fun RiderJobsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = context.appContainer.partner
    val scope = rememberCoroutineScope()
    var on by remember { mutableStateOf<Boolean?>(null) }
    var fee by remember { mutableStateOf(45.0) }
    LaunchedEffect(Unit) {
        (repo.home() as? ApiResult.Success)?.value?.let { on = it.takesRiderJobs; fee = it.riderFee }
    }
    fun set(value: Boolean) = scope.launch {
        if (repo.setRiderJobs(value) is ApiResult.Success) on = value
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 22.dp)) {
        BackBar(stringResource(R.string.jobs_title), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            RouteArt(Modifier.padding(top = 6.dp).fillMaxWidth().height(96.dp))
            Text(
                stringResource(R.string.jobs_heading),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                color = St.Ink,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp).semantics { heading() },
            )
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Point(StIcons.Bell, html(stringResource(R.string.jobs_point_ring)))
                Point(StIcons.Rupee, html(stringResource(R.string.jobs_point_earn, rupees(fee))))
                Point(StIcons.Shop, html(stringResource(R.string.jobs_point_first)))
                Point(StIcons.Toggle, AnnotatedString(stringResource(R.string.jobs_point_off)))
            }
            Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
            when (on) {
                null -> Unit
                false -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HoldButton(onDone = { set(true) })
                    Text(stringResource(R.string.jobs_hold_hint), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = St.Ink2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                        Text(stringResource(R.string.jobs_not_now), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.Green)
                    }
                }
                true -> DoneCard(onTurnOff = { set(false) }, onDone = onBack)
            }
        }
    }
}

@Composable
private fun Point(icon: ImageVector, text: AnnotatedString) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(St.Beige).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = St.Green, modifier = Modifier.padding(top = 1.dp).size(24.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp), color = St.Ink)
    }
}

private fun html(s: String) = AnnotatedString.fromHtml(s)

/** Press-and-hold: green fills left to right over 2 s; letting go early drains it. */
@Composable
private fun HoldButton(onDone: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val fill = remember { Animatable(0f) }
    val label = stringResource(if (fill.value > 0f) R.string.jobs_keep_holding else R.string.jobs_hold)
    val turnOn = stringResource(R.string.jobs_hold)
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.fillMaxWidth().height(68.dp).clip(shape).background(St.GreenTint).border(2.dp, St.Green, shape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val run = scope.launch {
                        fill.animateTo(1f, tween(((1f - fill.value) * HOLD_MS).toInt(), easing = LinearEasing))
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDone()
                    }
                    tryAwaitRelease()
                    if (fill.value < 1f) {
                        run.cancel()
                        scope.launch { fill.animateTo(0f, tween(200)) }
                    }
                })
            }
            // TalkBack can't press-and-hold reliably; give it a named action instead (still a deliberate step).
            .semantics(mergeDescendants = true) {
                customActions = listOf(CustomAccessibilityAction(turnOn) { onDone(); true })
            },
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fill.value).background(St.Green))
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val ink = if (fill.value > 0.55f) St.Cream else St.Green
            Icon(StIcons.Hand, contentDescription = null, tint = ink, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), color = ink)
        }
    }
}

/** "rj-done": the confirmation settles in from 96%. */
@Composable
private fun DoneCard(onTurnOff: () -> Unit, onDone: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val p = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(600, easing = OilEase)) }
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.graphicsLayer { alpha = p.value; scaleX = 0.96f + 0.04f * p.value; scaleY = scaleX }
            .fillMaxWidth().clip(shape).background(St.GreenTint).border(2.dp, St.Green, shape).padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(StIcons.CircleCheck, contentDescription = null, tint = St.Green, modifier = Modifier.size(26.dp))
            Text(stringResource(R.string.jobs_on), style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), color = St.Green)
        }
        Text(stringResource(R.string.jobs_on_body), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = St.Ink3)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(St.Cream)
                    .border(2.dp, St.Green, RoundedCornerShape(16.dp)).pointerInput(Unit) { detectTapGestures { onTurnOff() } },
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.jobs_turn_off), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Green) }
            Box(
                Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(St.Green)
                    .pointerInput(Unit) { detectTapGestures { onDone() } },
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.jobs_done), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Cream) }
        }
    }
}

/** Shop → rider → shop, the dashed path marching along ("rj-path"). */
@Composable
private fun RouteArt(modifier: Modifier) {
    val reduce = LocalReduceMotion.current
    val t by rememberLoop(1600, still = 0f)
    val shopPath = svgPath("M8 16l2-7h20l2 7M9 18v14h22V18")
    val scooter = svgPath("M9 47a7 7 0 1 0 14 0a7 7 0 1 0-14 0 M43 47a7 7 0 1 0 14 0a7 7 0 1 0-14 0 M8 41Q8 31 18 31H30L34 39H42 M44 17L50 40 M40 16H48")
    val route = svgPath("M62 46 C 110 10 190 10 238 46")
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(St.Beige), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(width = 300.dp, height = 80.dp)) {
            viewBox(300f, 80f) {
                drawPath(
                    route, St.Green,
                    style = Stroke(3f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f), if (reduce) 0f else -24f * t)),
                )
                translate(22f, 26f) {
                    box(0f, 0f, 40f, 40f, r = 10f, fill = St.Green)
                    line(shopPath, St.Cream, 2.4f)
                }
                translate(130f, 6f) {
                    dot(20f, 20f, 20f, fill = St.Gold)
                    translate(6f, 6f) { scale(0.44f, 0.44f, Offset.Zero) { line(scooter, St.Ink, 5f) } }
                }
                translate(238f, 26f) {
                    box(0f, 0f, 40f, 40f, r = 10f, fill = St.Cream, stroke = St.Green, strokeWidth = 2.5f)
                    line(shopPath, St.Green, 2.4f)
                }
            }
        }
    }
}
