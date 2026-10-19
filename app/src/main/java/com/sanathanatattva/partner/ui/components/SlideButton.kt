package com.sanathanatattva.partner.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Slide-to-confirm (order accept, go online/offline): a knob dragged across the track.
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
    /** How far the knob hints across (canvas: 40px on the ring, 22px on Home). 0 turns it off. */
    nudge: Dp = 40.dp,
    /** Three pulsing chevrons after the label (canvas "ho-chev"). */
    chevrons: Boolean = false,
    enabled: Boolean = true,
) {
    val scope = rememberCoroutineScope()
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
                onClick(label) { if (enabled) { haptics.performHapticFeedback(HapticFeedbackType.Confirm); onComplete() }; true }
            },
    ) {
        val density = LocalDensity.current
        val travel = with(density) { (maxWidth - knobSize - 8.dp).toPx() }
        val offset = remember { Animatable(0f) }
        var done by remember { mutableStateOf(false) }
        var dragging by remember { mutableStateOf(false) }
        // Past the point where letting go confirms; a tick marks crossing it either way.
        var armed by remember { mutableStateOf(false) }
        // Canvas "rg-knob" / "ho-knob": still for 55% of 2.8s, out by 70%, eased back by 100%.
        val clock by rememberLoop(2800)
        val hint = if (nudge.value > 0 && !reduce && !dragging && !done) {
            with(density) { nudge.toPx() } * kf(clock, 0f to 0f, 0.55f to 0f, 0.70f to 1f, 1f to 0f, easing = OilEase)
        } else 0f
        val chevronClock by rememberLoop(1600, still = 0.4f)
        Row(
            Modifier.fillMaxWidth().height(height).padding(start = if (fromEnd) 0.dp else knobSize + 8.dp, end = if (fromEnd) knobSize + 8.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp, fontWeight = FontWeight.Bold), color = textColor, maxLines = 1)
            if (chevrons) {
                Row {
                    listOf(0, 180, 360).forEach { delayMs ->
                        // "ho-chev": each fades 0.25 → 1 → 0.25 over 1.6s, 0.18s apart.
                        val a = if (reduce) 1f else kf(chevronClock.delayed(delayMs, 1600), 0f to 0.25f, 0.4f to 1f, 1f to 0.25f, easing = androidx.compose.animation.core.EaseInOut)
                        Icon(
                            if (fromEnd) StIcons.ChevronLeft else StIcons.ChevronRight, contentDescription = null, tint = textColor,
                            modifier = Modifier.size(16.dp).graphicsLayer { alpha = a },
                        )
                    }
                }
            } else {
                Icon(if (fromEnd) StIcons.DoubleChevronLeft else StIcons.DoubleChevronRight, contentDescription = null, tint = textColor, modifier = Modifier.size(22.dp))
            }
        }
        Box(
            Modifier
                .align(if (fromEnd) Alignment.CenterEnd else Alignment.CenterStart)
                .offset { IntOffset(((if (fromEnd) -1 else 1) * (offset.value + hint)).roundToInt(), 0) }
                .padding(4.dp)
                .size(knobSize)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(knob)
                .draggable(
                    enabled = enabled && !done,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val next = (offset.value + if (fromEnd) -delta else delta).coerceIn(0f, travel)
                        val nowArmed = next > travel * 0.8f
                        if (nowArmed != armed) {
                            armed = nowArmed
                            haptics.performHapticFeedback(if (nowArmed) HapticFeedbackType.GestureThresholdActivate else HapticFeedbackType.SegmentFrequentTick)
                        }
                        scope.launch { offset.snapTo(next) }
                    },
                    onDragStarted = { dragging = true },
                    onDragStopped = {
                        dragging = false
                        if (offset.value > travel * 0.8f) {
                            done = true
                            armed = false
                            // Act the moment they let go; the knob finishes its run alongside.
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onComplete()
                            offset.animateTo(travel, tween(180))
                            delay(600)
                            done = false
                            offset.snapTo(0f)
                        } else {
                            armed = false
                            offset.animateTo(0f, tween(320, easing = OilEase))
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(knobIcon, contentDescription = null, tint = St.Cream, modifier = Modifier.size(30.dp))
        }
    }
}
