package com.sanathanatattva.partner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Screens under the dock put a sheet up by calling [hideDock] while it shows; the dock
 * slides away so the sheet's buttons are never covered.
 */
class DockState {
    var hiddenBy by mutableStateOf(0)
        private set
    fun hide() { hiddenBy++ }
    fun show() { hiddenBy = (hiddenBy - 1).coerceAtLeast(0) }
}

val LocalDockState = staticCompositionLocalOf { DockState() }

/** Hides the dock for as long as the calling composable is on screen. */
@Composable
fun HideDock() {
    val dock = LocalDockState.current
    DisposableEffect(dock) {
        dock.hide()
        onDispose { dock.show() }
    }
}

/** One destination in the [LiquidDock]. */
data class DockItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/**
 * Space the dock occupies above the system navigation bar (its 64dp body plus the
 * 8dp margins). Screens whose content scrolls beneath the floating dock add this to
 * their bottom padding so nothing is trapped underneath it.
 */
val DockContentClearance = 88.dp

/**
 * The dock's widest span. Four tabs sit comfortably under one thumb on a phone; on a
 * tablet or in landscape the dock stays this size, centred, instead of stretching
 * its tabs a hand's width apart. Bars that float with it (the shop's cart bar) match.
 */
val DockMaxWidth = 480.dp

/**
 * Ported from the ST consumer app (SanathanaTattva `LiquidDock`) so both apps share one dock;
 * colours come from the partner app's St tokens (light cream ground only).
 *
 * A floating bottom navigation dock. The "liquid" is the motion, not the material:
 * a single brand-green selection pill glides between destinations on a spring and
 * stretches slightly along the direction of travel, so switching tabs reads as one
 * continuous morph rather than four separate on/off states.
 *
 * Legibility is never traded away for the effect — the dock is fully opaque and
 * icons/labels sit at full contrast (deliberately not glassmorphic, per DESIGN.md).
 * Under system reduced-motion the pill snaps instead of gliding.
 *
 * [onItemBounds] reports each destination's slot in root coordinates, so
 * something drawn above the app (a brand moment folding into a tab) can find it.
 */
@Composable
fun LiquidDock(
    items: List<DockItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onItemBounds: (index: Int, bounds: Rect) -> Unit = { _, _ -> },
) {
    if (items.isEmpty()) return
    val reduced = LocalReduceMotion.current
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val scope = rememberCoroutineScope()
    // A springy settle: low damping gives a small elastic overshoot, and a soft
    // stiffness keeps the glide liquid rather than mechanical. Paired with the
    // release-velocity fling below, tab changes read as one physical throw-and-settle.
    val settle = spring<Float>(dampingRatio = 0.6f, stiffness = 340f)
    // The drag gesture below outlives individual selections (its pointerInput only
    // restarts when the track geometry changes), so read these through the latest
    // composition instead of the stale values captured when the gesture began.
    val currentIndex by rememberUpdatedState(safeIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)

    // The pill's continuous position in "slot units" (0..lastIndex). A single value the
    // whole effect hangs off: the finger drives it during a drag, and it springs to the
    // selected slot the rest of the time.
    val pill = remember { Animatable(safeIndex.toFloat()) }
    // Live finger position while a drag is in progress; null when not dragging. It
    // overrides [pill] so the pill sits exactly under the finger with no spring lag.
    var dragPos by remember { mutableStateOf<Float?>(null) }
    // Accumulates pointer samples during a drag so release can read a real flick
    // velocity and hand it to the settle spring.
    val velocityTracker = remember { VelocityTracker() }

    // Glide to the selected slot whenever the selection changes from outside a drag
    // (a tap, or back-stack navigation). Skipped mid-drag so it can't fight the finger.
    LaunchedEffect(safeIndex) {
        if (dragPos == null) {
            if (reduced) pill.snapTo(safeIndex.toFloat())
            else pill.animateTo(safeIndex.toFloat(), settle)
        }
    }

    // Lift the floating dock off the cream ground with a soft neutral drop shadow.
    val dockShape = RoundedCornerShape(28.dp)
    // The system bars are cleared first, so the capped dock centres in the usable
    // width — a landscape side nav bar never pushes it off centre.
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
    Surface(
        modifier = Modifier
            .widthIn(max = DockMaxWidth)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 8.dp, shape = dockShape, clip = false, ambientColor = Color.Black, spotColor = Color.Black),
        shape = dockShape,
        color = St.Paper,
        shadowElevation = 0.dp,
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(6.dp),
        ) {
            val count = items.size
            val trackWidthPx = constraints.maxWidth.toFloat()
            val slotPx = trackWidthPx / count
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            // Small gap between the pill and its slot edges. Kept tight so the pill is
            // wide enough to fully carry the longest label on green.
            val pillInsetPx = with(density) { 3.dp.toPx() }
            val pillWidthDp = with(density) { (slotPx - pillInsetPx * 2f).toDp() }

            // Convert a pointer x (in track px) into a clamped, centred slot position.
            // Mirrored under RTL to match the Row's auto-mirrored child order.
            fun posFor(x: Float): Float {
                val logicalX = if (layoutDirection == LayoutDirection.Rtl) trackWidthPx - x else x
                return (logicalX / slotPx - 0.5f).coerceIn(0f, (count - 1).toFloat())
            }
            // The position everything renders from: the finger when dragging, else the pill.
            val pillPos = dragPos ?: pill.value

            // Fractional distance from the nearest slot → 0 at rest, ~0.5 mid-flight.
            val frac = pillPos - pillPos.roundToInt()
            // Speed of the pill right now, in slot-units/sec — the spring's own
            // velocity as it glides and overshoots (≈0 while a finger holds it).
            val speed = abs(pill.velocity)
            // Elastic deformation: the pill bulges along its direction of travel —
            // partly from how far it sits between slots, partly from raw speed — then
            // thins on the cross axis to conserve volume, so it reads as a gel blob
            // being flung rather than a rectangle being scaled.
            val stretch = (1f + 0.26f * (abs(frac) * 2f) + 0.16f * (speed / 14f).coerceIn(0f, 1f))
                .coerceAtMost(1.5f)

            Box(
                Modifier
                    .offset {
                        // Row auto-mirrors its children under RTL, so item i renders at
                        // visual slot (count-1-i); the pill's own offset math is always
                        // LTR-pixel-space and needs the same mirror to line up.
                        val leftX = slotPx * pillPos + pillInsetPx
                        val x = if (layoutDirection == LayoutDirection.Rtl) {
                            val pillWidthPx = with(density) { pillWidthDp.toPx() }
                            trackWidthPx - leftX - pillWidthPx
                        } else {
                            leftX
                        }
                        IntOffset(x.roundToInt(), 0)
                    }
                    .width(pillWidthDp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = stretch
                        // Volume-preserving squash: as it widens it flattens a little,
                        // the way a droplet does when it's pulled sideways.
                        scaleY = 1f - 0.5f * (stretch - 1f)
                    }
                    .background(
                        color = St.Green,
                        shape = RoundedCornerShape(22.dp),
                    ),
            )

            Row(
                Modifier
                    .fillMaxSize()
                    // Drag the pill directly: it tracks the finger, and on release snaps
                    // to (and selects) whichever destination it settled over. Taps on the
                    // destinations below still work — a tap isn't a drag.
                    .pointerInput(count, slotPx) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                velocityTracker.resetTracking()
                                dragPos = posFor(it.x)
                            },
                            onHorizontalDrag = { change, _ ->
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                dragPos = posFor(change.position.x)
                            },
                            onDragEnd = {
                                val finger = dragPos ?: return@detectHorizontalDragGestures
                                // Release velocity, track-px/s → slot-units/s. Mirrored
                                // under RTL, where sliding right lowers the slot index.
                                val vxPx = velocityTracker.calculateVelocity().x
                                val vSlots =
                                    (if (layoutDirection == LayoutDirection.Rtl) -vxPx else vxPx) / slotPx
                                // A flick throws the pill onward: bias the landing by the
                                // release velocity so a fast swipe commits to the next
                                // destination even when let go early.
                                val landed = (finger + vSlots * 0.10f)
                                    .roundToInt().coerceIn(0, items.lastIndex)
                                if (landed != currentIndex) currentOnSelect(landed)
                                // Hand the finger's position to [pill] BEFORE clearing the
                                // override, so the pill keeps sitting exactly where it was
                                // let go and settles onto the slot from there — no snap back
                                // to the old position while navigation catches up.
                                scope.launch {
                                    pill.snapTo(finger)
                                    dragPos = null
                                    if (reduced) pill.snapTo(landed.toFloat())
                                    // Carry the flick's momentum into the spring: a real
                                    // fling that overshoots and settles, not a fresh glide.
                                    else pill.animateTo(landed.toFloat(), settle, initialVelocity = vSlots)
                                }
                            },
                            onDragCancel = {
                                val finger = dragPos
                                scope.launch {
                                    if (finger != null) pill.snapTo(finger)
                                    dragPos = null
                                    pill.animateTo(currentIndex.toFloat(), settle)
                                }
                            },
                        )
                    },
            ) {
                val haptics = LocalHapticFeedback.current
                items.forEachIndexed { i, item ->
                    // 1 when the pill is centred on this item, fading to 0 as it leaves.
                    val on = (1f - abs(pillPos - i)).coerceIn(0f, 1f)
                    val contentColor = lerp(St.Ink2, St.Cream, on)
                    val interaction = remember { MutableInteractionSource() }
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .onGloballyPositioned { onItemBounds(i, it.boundsInRoot()) }
                            .clip(RoundedCornerShape(22.dp))
                            .selectable(
                                selected = i == safeIndex,
                                interactionSource = interaction,
                                // No ripple: the gliding pill is the only selection
                                // feedback, so a tap no longer flashes a second "ghost"
                                // pill at the target before the real one slides in.
                                indication = null,
                                role = Role.Tab,
                                // The tab already open stays as it is: no navigation, so
                                // its screen never reloads from a repeat tap.
                                onClick = {
                                    if (i != safeIndex) {
                                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        onSelect(i)
                                    }
                                },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (on > 0.5f) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            tint = contentColor,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    val s = 1f + 0.08f * on
                                    scaleX = s
                                    scaleY = s
                                },
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold),
                            color = contentColor,
                            maxLines = 1,
                            softWrap = false,
                            // Tuck the label a hair in from the pill edges and tighten
                            // tracking so the longest word ("Containers") sits fully on
                            // the green instead of bleeding onto the dock behind it.
                            letterSpacing = 0.sp,
                            modifier = Modifier.padding(horizontal = 2.dp),
                        )
                    }
                }
            }
        }
    }
    }
}
