package com.sanathanatattva.partner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/**
 * Slide-to-confirm (order accept, go online/offline): a knob dragged across the track, with a
 * green fill stretching behind it and the label turning cream where the fill covers it.
 * Prevents pocket taps; TalkBack users get a normal double-tap action instead.
 * [fromEnd] puts the knob on the right and slides it left (going offline).
 */
@Composable
fun SlideButton(
    label: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    knobIcon: ImageVector = StIcons.ArrowRight,
    fromEnd: Boolean = false,
    height: Dp = 76.dp,
    track: Color = St.GreenTint,
    border: Color = St.Green,
    knob: Color = St.Green,
    textColor: Color = St.Green,
    /** The label's colour where the fill has covered it. */
    filledTextColor: Color = St.Cream,
    /** How far the knob hints across (canvas: 40px on the ring, 22px on Home). 0 turns it off. */
    nudge: Dp = 40.dp,
    /** Three pulsing chevrons after the label (canvas "ho-chev"). */
    chevrons: Boolean = false,
    enabled: Boolean = true,
    /**
     * After a slide, keeps the knob at the far end while what it started is still open (the
     * start-place picker, going online); when that closes it slides home and starts hinting again.
     */
    held: Boolean = false,
) {
    val reduce = LocalReduceMotion.current
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(height / 2)
    val knobSize = height - 8.dp
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(track)
            .border(2.dp, border, shape)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
                if (!enabled) disabled()
                onClick(label) { if (enabled) { haptics.performHapticFeedback(HapticFeedbackType.Confirm); onComplete() }; true }
            },
    ) {
        val density = LocalDensity.current
        val travel = with(density) { (maxWidth - knobSize - 8.dp).toPx() }
        val nudgePx = with(density) { nudge.toPx() }
        // Where the knob is. A plain state set straight from the finger, so the knob and the fill
        // move in the same frame as the drag (a coroutine per move ran a frame behind).
        var pos by remember { mutableFloatStateOf(0f) }
        // 0..1 of the resting hint; only counts while the knob is home.
        val hint = remember { Animatable(0f) }
        var resting by remember { mutableStateOf(true) }
        var done by remember { mutableStateOf(false) }
        // Past the point where letting go confirms; a tick marks crossing it either way.
        var armed by remember { mutableStateOf(false) }
        val holding by rememberUpdatedState(held)
        fun knobAt(): Float = pos + if (resting) hint.value * nudgePx else 0f
        suspend fun slideTo(target: Float, durationMs: Int) {
            animate(pos, target, animationSpec = tween(if (reduce) 0 else durationMs, easing = OilEase)) { v, _ -> pos = v }
        }
        suspend fun slideHome() {
            armed = false
            slideTo(0f, 420)
            resting = true
        }

        // Canvas "rg-knob" / "ho-knob": still for 55% of 2.8s, out by 70%, eased back by 100%.
        // Starts over from still whenever the knob comes home, so it never jumps mid-hint.
        LaunchedEffect(resting, enabled, nudgePx, reduce) {
            hint.snapTo(0f)
            if (!resting || !enabled || nudgePx == 0f || reduce) return@LaunchedEffect
            while (true) {
                delay(1540)
                hint.animateTo(1f, tween(420, easing = OilEase))
                hint.animateTo(0f, tween(840, easing = OilEase))
            }
        }
        // A finished slide: the knob runs to the end, waits there while what it opened is open,
        // then slides home. If the screen changes under it, it starts fresh next time.
        LaunchedEffect(done) {
            if (!done) return@LaunchedEffect
            slideTo(travel, 180)
            delay(600)
            snapshotFlow { holding }.first { !it }
            // Home first: clearing [done] restarts this effect, which would cut the slide short.
            slideHome()
            done = false
        }
        // Disabled mid-slide (e.g. busy): let go of it.
        LaunchedEffect(enabled) {
            if (!enabled && !done && pos > 0f) slideHome()
        }

        // The fill: a green capsule from the start edge to the knob, the knob at its leading end.
        fun fillLeft(w: Float): Float = if (fromEnd) w - with(density) { 4.dp.toPx() + knobSize.toPx() } - knobAt() else with(density) { 4.dp.toPx() }
        fun fillRight(w: Float): Float = if (fromEnd) w - with(density) { 4.dp.toPx() } else with(density) { 4.dp.toPx() + knobSize.toPx() } + knobAt()

        val chevronClock by rememberLoop(1600, still = 0.4f)
        @Composable
        fun Label(color: Color, modifier: Modifier) {
            Row(
                modifier.fillMaxWidth().height(height).padding(start = if (fromEnd) 0.dp else knobSize + 8.dp, end = if (fromEnd) knobSize + 8.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp, fontWeight = FontWeight.Bold), color = color, maxLines = 1)
                if (chevrons) {
                    Row {
                        listOf(0, 180, 360).forEach { delayMs ->
                            Icon(
                                if (fromEnd) StIcons.ChevronLeft else StIcons.ChevronRight, contentDescription = null, tint = color,
                                modifier = Modifier.size(16.dp).graphicsLayer {
                                    // "ho-chev": each fades 0.25 → 1 → 0.25 over 1.6s, 0.18s apart.
                                    alpha = if (reduce) 1f else kf(chevronClock.delayed(delayMs, 1600), 0f to 0.25f, 0.4f to 1f, 1f to 0.25f, easing = androidx.compose.animation.core.EaseInOut)
                                },
                            )
                        }
                    }
                } else {
                    Icon(if (fromEnd) StIcons.DoubleChevronLeft else StIcons.DoubleChevronRight, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
        }
        Label(textColor, Modifier)
        Box(
            Modifier.matchParentSize().drawBehind {
                val left = fillLeft(size.width)
                val right = fillRight(size.width)
                val inset = 4.dp.toPx()
                drawRoundRect(knob, Offset(left, inset), Size(right - left, size.height - inset * 2), CornerRadius((size.height - inset * 2) / 2))
            },
        )
        // The same label in cream, showing only over the fill.
        Label(
            filledTextColor,
            Modifier.drawWithContent {
                clipRect(left = fillLeft(size.width), right = fillRight(size.width)) { this@drawWithContent.drawContent() }
            },
        )
        Box(
            Modifier
                .align(if (fromEnd) Alignment.CenterEnd else Alignment.CenterStart)
                .offset { IntOffset(((if (fromEnd) -1 else 1) * knobAt()).roundToInt(), 0) }
                .padding(4.dp)
                .size(knobSize)
                // A soft drawn shadow. The phone's own elevation shadow showed as a small
                // hexagon while the Online card faded out.
                .drawBehind {
                    val r = size.minDimension / 2 + 5.dp.toPx()
                    val c = center + Offset(0f, 2.dp.toPx())
                    drawCircle(Brush.radialGradient(0.72f to Color.Black.copy(alpha = 0.22f), 1f to Color.Transparent, center = c, radius = r), r, c)
                }
                .clip(CircleShape)
                .background(knob)
                .draggable(
                    enabled = enabled && !done,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val next = (pos + if (fromEnd) -delta else delta).coerceIn(0f, travel)
                        val nowArmed = next > travel * 0.8f
                        if (nowArmed != armed) {
                            armed = nowArmed
                            haptics.performHapticFeedback(if (nowArmed) HapticFeedbackType.GestureThresholdActivate else HapticFeedbackType.SegmentFrequentTick)
                        }
                        pos = next
                    },
                    onDragStarted = {
                        // Picked up mid-hint: carry on from where the knob is, no jump back.
                        pos = knobAt()
                        resting = false
                    },
                    onDragStopped = { velocity ->
                        // A quick flick past halfway counts too.
                        val flung = (if (fromEnd) -velocity else velocity) > 1800f && pos > travel * 0.5f
                        if (pos > travel * 0.8f || flung) {
                            // Act the moment they let go; the knob finishes its run alongside.
                            armed = false
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            done = true
                            onComplete()
                        } else {
                            slideHome()
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(knobIcon, contentDescription = null, tint = St.Cream, modifier = Modifier.size(30.dp))
        }
    }
}
