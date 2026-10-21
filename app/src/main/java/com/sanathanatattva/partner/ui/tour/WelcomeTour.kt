package com.sanathanatattva.partner.ui.tour

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.alerts.AlertCheck
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.home.HomeTab
import com.sanathanatattva.partner.ui.shop.displayStyle
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// The welcome tour (canvas "ST Delivery · Welcome tour"): after a partner first logs in, a card
// appears from thin air, a spotlight glides from the yellow bar through the alert checklist to
// Online/Offline, the help button and the bottom buttons, and a hand bobs to say "tap here".

enum class TourStep { Welcome, Bar, Checklist, AllSet, Online, Help, Dock, Finish }

/** Which screen is showing, as far as the tour cares. */
enum class TourScreen { Home, Alerts, Other }

@Stable
class TourState {
    var step by mutableStateOf<TourStep?>(null)
        private set
    var dockIndex by mutableIntStateOf(0)
        private set
    /** Bumps whenever a new card should pop in; the last card glides instead. */
    var popKey by mutableIntStateOf(0)
        private set
    /** The alerts were already on, so steps 1–3 were skipped. */
    var skippedAlerts by mutableStateOf(false)
        private set
    /** Where the things the tour points at are, in root coordinates. */
    val targets = mutableStateMapOf<String, Rect>()
    /** What the alert checklist still has to turn on (it reports this while showing). */
    var missing by mutableStateOf<List<AlertCheck>>(AlertCheck.entries.toList())
        private set
    /** What the checklist's spotlight sits on next ("turnon_Battery", "autostart"); null when done. */
    var checklistKey by mutableStateOf<String?>(null)
        private set
    /** The checklist has reported at least once since the tour reached it. */
    var checklistReported by mutableStateOf(false)
        private set
    /** How tall the card is (px), so a page can scroll what it points at clear of it. */
    var cardHeight by mutableIntStateOf(0)
        internal set

    fun reportChecklist(missing: List<AlertCheck>, next: String?) {
        this.missing = missing
        checklistKey = next
        checklistReported = true
    }
    /** Remember the tour was seen (called on finish or skip). */
    var onDone: () -> Unit = {}

    fun start() {
        dockIndex = 0
        skippedAlerts = false
        closing = false
        cardHeight = 0
        go(TourStep.Welcome)
    }

    fun go(next: TourStep, pop: Boolean = true) {
        if (next == TourStep.Checklist && step != TourStep.Checklist) checklistReported = false
        step = next
        if (pop) popKey++
    }

    fun afterWelcome(alertsReady: Boolean) {
        // No yellow bar on screen (already online) means nothing to set up from here.
        val skip = alertsReady || "bar" !in targets
        skippedAlerts = skip
        go(if (skip) TourStep.Online else TourStep.Bar)
    }

    fun toDock() {
        dockIndex = 0
        go(TourStep.Dock)
    }

    fun nextDock(count: Int) {
        if (dockIndex < count - 1) {
            dockIndex++
            popKey++
        } else {
            // "At the end the card shouldn't bounce": it just glides and settles to its size.
            go(TourStep.Finish, pop = false)
        }
    }

    /** Start or Skip: the dark layer fades away, then the tour ends ([stop]). */
    var closing by mutableStateOf(false)
        private set

    fun finish() {
        if (step != null) closing = true
    }

    fun stop() {
        step = null
        closing = false
        onDone()
    }
}

val LocalTour = staticCompositionLocalOf { TourState() }

/** Marks something the tour can spotlight. */
@Composable
fun Modifier.tourTarget(key: String): Modifier {
    val tour = LocalTour.current
    DisposableEffect(tour, key) { onDispose { tour.targets.remove(key) } }
    return this.onGloballyPositioned { coords ->
        val r = coords.boundsInRoot()
        if (tour.targets[key] != r) tour.targets[key] = r
    }
}

/**
 * Like [tourTarget], and while the tour is on [makeRoomOn] the page scrolls it into view with room
 * under it for the card (on smaller phones Home's slide button sits too low for both to fit).
 */
@Composable
fun Modifier.tourTarget(key: String, makeRoomOn: TourStep): Modifier {
    val tour = LocalTour.current
    val density = LocalDensity.current
    val requester = remember { BringIntoViewRequester() }
    var height by remember { mutableIntStateOf(0) }
    val navBar = WindowInsets.navigationBars.getBottom(density)
    val active = tour.step == makeRoomOn
    LaunchedEffect(active, tour.cardHeight, height) {
        if (!active || tour.cardHeight == 0 || height == 0) return@LaunchedEffect
        // Under it: the gap and the card, then the margin above the phone's own buttons.
        val room = tour.cardHeight + navBar + with(density) { 64.dp.toPx() }
        requester.bringIntoView(Rect(0f, -with(density) { 24.dp.toPx() }, 1f, height + room))
    }
    return tourTarget(key).bringIntoViewRequester(requester).onSizeChanged { height = it.height }
}

private val Glide = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
/** Stiffness of the no-bounce spring the spotlight, hand and card glide on (about 0.6s). */
private const val Follow = 120f
private val Scrim = Color(0xBD0F2417) // ink green at 74%
private val Ring = Color(0xFFF0C25A)
private val DropPath by lazy { svgPath("M12 3c3 4 6 7.5 6 11a6 6 0 0 1-12 0c0-3.5 3-7 6-11z") }
private val HandPath by lazy {
    svgPath("M18 30V8.5a4.5 4.5 0 0 1 9 0V25a4 4 0 0 1 8 0v2a4 4 0 0 1 8 0V40c0 9-6.5 16-15.5 16h-3c-5 0-8.5-2.6-11.3-6.6L6 39.6a3.6 3.6 0 0 1 5.6-4.5L18 41z")
}
private val KnucklePath by lazy { svgPath("M27 25v7M35 27v6M18 41v-6") }

private enum class HandMode { None, Up, Down, Swipe }

private class HandPlace {
    var mode = HandMode.None
    var spot = Offset.Zero
}

/** What the card shows. A change without a new pop cross-fades while the card glides to its new size. */
private data class CardFace(val step: TourStep, val dockIndex: Int, val popKey: Int)

/**
 * Draws the tour over the app. [tabs] are the bottom buttons in order (riders have no My Shop).
 * [onBackHome] takes the partner from the checklist back to Home when the alerts are all on.
 */
@Composable
fun TourOverlay(
    tour: TourState,
    screen: TourScreen,
    firstName: String,
    tabs: List<HomeTab>,
    tabLabel: (HomeTab) -> String,
    alertsReady: () -> Boolean,
    onBackHome: () -> Unit,
) {
    val step = tour.step ?: return
    val onScreen = when (step) {
        TourStep.Checklist, TourStep.AllSet -> screen == TourScreen.Alerts
        else -> screen == TourScreen.Home
    }
    if (!onScreen) return
    BackHandler { tour.finish() }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(step) {
        if (step == TourStep.AllSet || step == TourStep.Finish) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    val density = LocalDensity.current
    val reduce = LocalReduceMotion.current
    // "Home dims": the whole tour fades in, and fades away again on Start or Skip. Each part fades
    // itself; one layer over the whole screen made the phone redraw all of it off screen each frame.
    val shown = remember { Animatable(if (reduce) 1f else 0f) }
    val fade = { shown.value }
    LaunchedEffect(tour.closing) {
        if (!tour.closing) return@LaunchedEffect
        shown.animateTo(0f, tween(if (reduce) 0 else 450, easing = Glide))
        tour.stop()
    }
    BoxWithConstraints(Modifier.fillMaxSize().semantics { paneTitle = "Tour" }) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val topInset = WindowInsets.statusBars.getTop(density).toFloat()
        val bottomInset = WindowInsets.navigationBars.getBottom(density).toFloat()
        fun dp(v: Float) = with(density) { v.dp.toPx() }

        // Online points at the slide button: the whole card is too tall to leave room for this one.
        val onlineKey = listOf("slider", "slider_off").firstOrNull { it in tour.targets } ?: "status"
        val pointedAt: Rect? = when (step) {
            TourStep.Bar -> tour.targets["bar"]?.inflate(dp(6f))
            TourStep.Checklist -> tour.checklistKey?.let { tour.targets[it] }?.inflate(dp(6f))
            TourStep.Online -> tour.targets[onlineKey]?.inflate(dp(8f))
            TourStep.Help -> tour.targets["help"]?.inflate(dp(10f))
            TourStep.Dock -> tour.targets["dock_${tour.dockIndex}"]?.inflate(dp(3f))
            else -> null
        }
        // Something off screen (scrolled away, or gone) has nothing to spotlight yet.
        val target = pointedAt?.takeIf { it.width > 4f && it.height > 4f && it.top >= 0f && it.bottom <= h }
        val radius = when (step) {
            TourStep.Help -> (target?.height ?: 0f) / 2f
            TourStep.Online -> if (onlineKey == "status") dp(34f) else (target?.height ?: 0f) / 2f
            TourStep.Checklist -> dp(30f)
            TourStep.Dock -> dp(26f)
            else -> dp(24f)
        }
        // A spring with no bounce keeps gliding smoothly when the page scrolls under it mid-move
        // (a tween started over every frame and stalled). With nothing to point at, the spotlight
        // closes where it is, and it opens where it's needed: flying in from, or off to, the middle
        // of the screen swept a bright patch across the dark layer.
        val hole = remember { Animatable(target ?: Rect(Offset(w / 2f, h / 2f), Size.Zero), Rect.VectorConverter, Rect.VisibilityThreshold) }
        LaunchedEffect(target) {
            val to = target ?: Rect(hole.value.center, Size.Zero)
            if (reduce) { hole.snapTo(to); return@LaunchedEffect }
            if (target != null && hole.value.width < 1f) hole.snapTo(Rect(target.center, Size.Zero))
            hole.animateTo(to, spring(1f, Follow, Rect.VisibilityThreshold))
        }
        val holeRadius = animateFloatAsState(radius, if (reduce) snap() else spring(1f, Follow, 0.5f), label = "radius")
        val pulse = rememberInfiniteTransition(label = "ring")
        val ring = pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "ring")
        val ringOn = animateFloatAsState(if (target != null) 1f else 0f, tween(if (reduce) 0 else 250), label = "ringOn")
        val holePath = remember { Path() }

        // The dark layer with its hole, and the gold ring round the hole (all read while drawing).
        // The hole is a clip: cheap for the phone, where an even-odd path was redrawn every frame.
        Canvas(Modifier.fillMaxSize()) {
            val r = hole.value
            val corner = CornerRadius(holeRadius.value)
            val f = fade()
            if (r.width > 1f) {
                holePath.rewind()
                holePath.addRoundRect(RoundRect(r, corner))
                clipPath(holePath, ClipOp.Difference) { drawRect(Scrim.copy(alpha = Scrim.alpha * f)) }
            } else {
                drawRect(Scrim.copy(alpha = Scrim.alpha * f))
            }
            val on = ringOn.value * f
            if (on > 0f && r.width > 1f) {
                val sw = 3.dp.toPx()
                drawRoundRect(Ring.copy(alpha = on), r.topLeft + Offset(sw / 2, sw / 2), Size(r.width - sw, r.height - sw), corner, style = Stroke(sw))
                if (!reduce) {
                    val p = ring.value
                    val spread = 16.dp.toPx() * p
                    val band = r.inflate(spread / 2f)
                    drawRoundRect(Ring.copy(alpha = 0.95f * (1f - p) * on), band.topLeft, band.size, CornerRadius(holeRadius.value + spread / 2f), style = Stroke(spread.coerceAtLeast(0.01f)))
                }
            }
        }

        // Taps: the yellow bar and the "Turn on" buttons work for real through the hole; everything
        // else waits for the card's button (a tap on the spotlight counts as Next).
        val passThrough = (step == TourStep.Bar || step == TourStep.Checklist) && target != null
        if (passThrough && target != null) {
            Blocker(0f, 0f, w, target.top)
            Blocker(0f, target.bottom, w, h - target.bottom)
            Blocker(0f, target.top, target.left, target.height)
            Blocker(target.right, target.top, w - target.right, target.height)
        } else {
            Blocker(0f, 0f, w, h)
            if (target != null) {
                Box(
                    Modifier.offset { IntOffset(target.left.roundToInt(), target.top.roundToInt()) }
                        .size(with(density) { target.width.toDp() }, with(density) { target.height.toDp() })
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            when (step) {
                                TourStep.Online -> tour.go(TourStep.Help)
                                TourStep.Help -> tour.toDock()
                                TourStep.Dock -> tour.nextDock(tabs.size)
                                else -> Unit
                            }
                        },
                )
            }
        }

        // The hand that says "tap here".
        val slider = tour.targets["slider"]
        val handMode = when {
            target == null -> HandMode.None
            step == TourStep.Dock -> HandMode.Down
            step == TourStep.Online -> if (onlineKey == "slider" && slider != null) HandMode.Swipe else HandMode.None
            else -> HandMode.Up
        }
        // Where the hand last was, so it fades out there instead of vanishing when the card moves on.
        val handAt = remember { HandPlace() }
        if (handMode != HandMode.None && target != null) {
            val handW = dp(48f); val handH = dp(60f)
            handAt.mode = handMode
            handAt.spot = when (handMode) {
                HandMode.Down -> Offset(target.center.x - handW * (25.5f / 48f), target.top - handH - dp(4f))
                HandMode.Swipe -> Offset(slider!!.left + dp(8f), slider.center.y - dp(8f))
                else -> Offset(target.center.x - handW * (22.5f / 48f), target.bottom + dp(4f))
            }
        }
        val handShown = animateFloatAsState(if (handMode != HandMode.None) 1f else 0f, tween(if (reduce) 0 else 250), label = "handShown")
        if (handAt.mode != HandMode.None && (handMode != HandMode.None || handShown.value > 0f)) {
            val at = animateOffsetAsState(handAt.spot, if (reduce) snap() else spring(1f, Follow, Offset.VisibilityThreshold), label = "hand")
            Hand(at, handAt.mode, swipeDistance = (slider?.width ?: 0f) - dp(90f), reduce = reduce, fade = { fade() * handShown.value })
        }

        // The card: measured first, placed, and only then popped in from thin air.
        var cardHeight by remember { mutableIntStateOf(0) }
        val cardH = cardHeight.toFloat()
        val margin = dp(16f)
        val wanted = when {
            target == null -> (h - cardH) / 2f - dp(30f)
            step == TourStep.Dock -> target.top - dp(76f) - cardH
            step == TourStep.Checklist -> if (target.center.y > h * 0.55f) topInset + margin else h - bottomInset - cardH - dp(24f)
            else -> {
                val below = target.bottom + if (step == TourStep.Online) dp(20f) else dp(74f)
                val above = target.top - cardH - dp(20f)
                when {
                    below + cardH <= h - bottomInset - margin -> below
                    above >= topInset + margin -> above
                    // No room either side: sit low, so the top of what it points at still shows.
                    else -> h - bottomInset - cardH - margin
                }
            }
        }.coerceIn(topInset + dp(8f), (h - bottomInset - cardH - dp(8f)).coerceAtLeast(topInset + dp(8f)))
        val top = remember { Animatable(wanted) }
        var placed by remember { mutableStateOf(false) }
        LaunchedEffect(wanted, cardHeight) {
            if (cardHeight == 0) return@LaunchedEffect
            if (!placed || reduce) { top.snapTo(wanted); placed = true } else top.animateTo(wanted, spring(1f, Follow, 0.5f))
        }
        // The first frames of a new tour are slow (its text, fonts and shapes get ready): let them
        // pass while nothing shows, then fade in, so the fade never stutters.
        var ready by remember { mutableStateOf(reduce) }
        LaunchedEffect(placed) {
            if (!placed || ready) return@LaunchedEffect
            repeat(2) { withFrameNanos { } }
            ready = true
            shown.animateTo(1f, tween(450, easing = Glide))
        }
        val pop = remember { Animatable(if (reduce) 1f else 0f) }
        val spark = remember { Animatable(if (reduce) 1f else 0f) }
        LaunchedEffect(tour.popKey, ready) {
            if (reduce || !ready) return@LaunchedEffect
            pop.snapTo(0f); spark.snapTo(0f)
            // The first card waits for the dark layer to begin fading in.
            if (shown.value < 0.5f) kotlinx.coroutines.delay(150)
            kotlinx.coroutines.coroutineScope {
                launch { pop.animateTo(1f, tween(560, easing = LinearEasing)) }
                launch { spark.animateTo(1f, tween(700, easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1f))) }
            }
        }
        Box(
            Modifier.offset { IntOffset(dp(20f).roundToInt(), top.value.roundToInt()) }
                .width(with(density) { (w - dp(40f)).toDp() })
                .onSizeChanged { cardHeight = it.height; tour.cardHeight = it.height }
                .graphicsLayer { alpha = if (placed) 1f else 0f },
        ) {
            // The gold ring that spreads out behind the card as it appears (unseen until it starts).
            Box(
                Modifier.matchParentSize()
                    .graphicsLayer {
                        val p = spark.value
                        alpha = if (p <= 0f || p >= 1f) 0f else 0.9f * (1f - p) * fade()
                        scaleX = 0.3f + 1.05f * p
                        scaleY = scaleX
                    }
                    .border(3.dp, Ring, RoundedCornerShape(36.dp)),
            )
            Column(
                Modifier
                    .graphicsLayer {
                        // Grows from 35% to 106%, settles to 98%, then 100%. (A blur on top made
                        // every frame of the pop too slow for the phone.)
                        val p = pop.value
                        val grow = p.coerceAtMost(0.55f) / 0.55f
                        alpha = grow * fade()
                        val s = when {
                            p < 0.55f -> 0.35f + (1.06f - 0.35f) * EaseInOut.transform(grow)
                            p < 0.78f -> 1.06f + (0.98f - 1.06f) * ((p - 0.55f) / 0.23f)
                            else -> 0.98f + 0.02f * ((p - 0.78f) / 0.22f)
                        }
                        scaleX = s
                        scaleY = s
                    }
                    .shadow(18.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(St.Paper)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                // A new pop swaps the words while the card is invisible. The last card doesn't pop: its
                // words cross-fade and it glides to its new size (a jump in size read as a glitch).
                AnimatedContent(
                    targetState = CardFace(step, tour.dockIndex, tour.popKey),
                    transitionSpec = {
                        if (reduce || targetState.popKey != initialState.popKey) {
                            (EnterTransition.None togetherWith ExitTransition.None).using(SizeTransform { _, _ -> snap() })
                        } else {
                            (fadeIn(tween(280, delayMillis = 140)) togetherWith fadeOut(tween(140)))
                                .using(SizeTransform { _, _ -> spring(1f, Follow, IntSize.VisibilityThreshold) })
                        }
                    },
                    contentAlignment = Alignment.TopCenter,
                    label = "card",
                ) { face ->
                    Column(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TourCard(tour, face.step, face.dockIndex, firstName, tabs, tabLabel, alertsReady, onBackHome)
                    }
                }
            }
        }
    }
}

/** Swallows taps so the app underneath waits for the tour. */
@Composable
private fun Blocker(x: Float, y: Float, width: Float, height: Float) {
    if (width <= 0f || height <= 0f) return
    val density = LocalDensity.current
    Box(
        Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(with(density) { width.toDp() }, with(density) { height.toDp() })
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
    )
}

/** The pointing hand: bobs and ripples ("tap here"), or slides along Go Online ("swipe"). */
@Composable
private fun Hand(at: androidx.compose.runtime.State<Offset>, mode: HandMode, swipeDistance: Float, reduce: Boolean, fade: () -> Float) {
    val loop = rememberInfiniteTransition(label = "hand")
    val bobState = loop.animateFloat(0f, 1f, infiniteRepeatable(tween(550, easing = EaseInOut), RepeatMode.Reverse), label = "bob")
    val rippleState = loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "ripple")
    val swipeState = loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "swipe")
    val density = LocalDensity.current
    Canvas(
        Modifier.offset { IntOffset(at.value.x.roundToInt(), at.value.y.roundToInt()) }.size(48.dp, 60.dp)
            .graphicsLayer {
                when (mode) {
                    HandMode.Swipe -> if (!reduce) {
                        // Fade in on the knob, slide across (to 70%), fade out (by 86%), wait.
                        val t = swipeState.value
                        val move = CubicBezierEasing(0.5f, 0f, 0.3f, 1f).transform((t / 0.7f).coerceIn(0f, 1f))
                        translationX = swipeDistance.coerceAtLeast(0f) * move
                        alpha = when {
                            t < 0.12f -> t / 0.12f
                            t < 0.7f -> 1f
                            t < 0.86f -> 1f - (t - 0.7f) / 0.16f
                            else -> 0f
                        }
                    }
                    else -> {
                        if (!reduce) translationY = -with(density) { 14.dp.toPx() } * bobState.value
                        if (mode == HandMode.Down) rotationZ = 180f
                    }
                }
                alpha *= fade()
            },
    ) {
        val u = size.width / 48f
        if (mode != HandMode.Swipe && !reduce) {
            val ripple = rippleState.value
            val r = 15.dp.toPx() * (0.4f + 1.5f * ripple)
            drawCircle(Ring.copy(alpha = 0.75f * (1f - ripple)), radius = r, center = Offset(22.5f * u, 4f * u), style = Stroke(3.dp.toPx()))
        }
        scale(u, u, pivot = Offset.Zero) {
            drawPath(HandPath, St.Paper)
            drawPath(HandPath, St.Ink, style = Stroke(2.4f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            drawPath(KnucklePath, St.Ink.copy(alpha = 0.55f), style = Stroke(1.8f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
    }
}

@Composable
private fun TourCard(
    tour: TourState,
    step: TourStep,
    dockIndex: Int,
    firstName: String,
    tabs: List<HomeTab>,
    tabLabel: (HomeTab) -> String,
    alertsReady: () -> Boolean,
    onBackHome: () -> Unit,
) {
    val total = if (tour.skippedAlerts) 4 else 5
    val shift = if (tour.skippedAlerts) 1 else 0
    val number = when (step) {
        TourStep.Online -> 2 - shift
        TourStep.Help -> 3 - shift
        TourStep.Dock -> 4 - shift
        else -> 1
    }
    when (step) {
        TourStep.Welcome -> {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Pop(delayMs = 200) {
                    Box(Modifier.size(84.dp).clip(CircleShape).background(St.GoldGlint).border(3.dp, St.GoldPale, CircleShape), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(40.dp)) {
                            val u = size.width / 24f
                            scale(u, u, pivot = Offset.Zero) { drawPath(DropPath, St.Gold); drawPath(DropPath, St.GoldDeep, style = Stroke(1.2f)) }
                        }
                    }
                }
                Text(stringResource(R.string.tour_welcome_kicker), style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.GoldInk)
                Text(stringResource(R.string.tour_welcome_title, firstName), style = displayStyle(30), color = St.Ink, textAlign = TextAlign.Center)
                Text(stringResource(R.string.tour_welcome_body), style = body(), color = St.Ink2, textAlign = TextAlign.Center)
            }
            TourButton(stringResource(R.string.tour_show_me)) { tour.afterWelcome(alertsReady()) }
            Text(
                stringResource(R.string.tour_skip),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = St.Ink2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).clickable { tour.finish() }.padding(top = 12.dp),
            )
        }
        TourStep.Bar -> {
            Steps(number, total) { tour.finish() }
            Text(stringResource(R.string.tour_bar_title), style = displayStyle(26), color = St.Ink)
            Text(stringResource(R.string.tour_bar_body), style = body(), color = St.Ink2)
            Text(
                stringResource(R.string.tour_bar_prompt),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
                color = St.Ink,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(St.GoldGlint).padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
        TourStep.Checklist -> {
            val autostart = tour.checklistKey == "autostart"
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (autostart) R.string.tour_autostart_title else R.string.tour_turn_on_title), style = displayStyle(26), color = St.Ink, modifier = Modifier.weight(1f))
                SkipTour { tour.finish() }
            }
            Text(AnnotatedString.fromHtml(stringResource(if (autostart) R.string.tour_autostart_body else R.string.tour_turn_on_body)), style = body(18), color = St.Ink2)
            val on = 5 - tour.missing.size
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(5) { i ->
                    val done = i < on
                    val color by androidx.compose.animation.animateColorAsState(if (done) St.Green else St.Track, tween(400), label = "seg$i")
                    Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(color))
                }
            }
        }
        TourStep.AllSet -> {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Celebration()
                Text(stringResource(R.string.tour_allset_title), style = displayStyle(30), color = St.Ink, textAlign = TextAlign.Center)
                Text(stringResource(R.string.tour_allset_body), style = body(), color = St.Ink2, textAlign = TextAlign.Center)
            }
            TourButton(stringResource(R.string.tour_back_home)) {
                onBackHome()
                tour.go(TourStep.Online)
            }
        }
        TourStep.Online -> {
            Steps(number, total) { tour.finish() }
            Text(stringResource(R.string.tour_online_title), style = displayStyle(26), color = St.Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.padding(top = 6.dp).size(16.dp).clip(CircleShape).background(St.Green))
                Text(AnnotatedString.fromHtml(stringResource(R.string.tour_online_on)), style = body(18), color = St.Ink)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.padding(top = 6.dp).size(16.dp).border(3.dp, St.FieldBorder, CircleShape))
                Text(AnnotatedString.fromHtml(stringResource(R.string.tour_online_off)), style = body(18), color = St.Ink)
            }
            Text(stringResource(R.string.tour_online_slide), style = body(17), color = St.Ink2)
            TourButton(stringResource(R.string.tour_next)) { tour.go(TourStep.Help) }
        }
        TourStep.Help -> {
            Steps(number, total) { tour.finish() }
            Text(stringResource(R.string.tour_help_title), style = displayStyle(26), color = St.Ink)
            Text(stringResource(R.string.tour_help_body), style = body(), color = St.Ink2)
            TourButton(stringResource(R.string.tour_next)) { tour.toDock() }
        }
        TourStep.Dock -> {
            val tab = tabs.getOrElse(dockIndex) { tabs.last() }
            Steps(number, total) { tour.finish() }
            Text(stringResource(R.string.tour_dock_kicker, tabs.size), style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.GoldInk)
            Text(tabLabel(tab), style = displayStyle(28), color = St.Ink)
            Text(
                stringResource(
                    when (tab) {
                        HomeTab.Home -> R.string.tour_dock_home
                        HomeTab.MyShop -> R.string.tour_dock_shop
                        HomeTab.Earnings -> R.string.tour_dock_earnings
                        HomeTab.Profile -> R.string.tour_dock_profile
                    },
                ),
                style = body(),
                color = St.Ink2,
            )
            TourButton(stringResource(R.string.tour_next)) { tour.nextDock(tabs.size) }
        }
        TourStep.Finish -> {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Celebration()
                Text(stringResource(R.string.tour_finish_title), style = displayStyle(30), color = St.Ink, textAlign = TextAlign.Center)
                Text(stringResource(R.string.tour_finish_body), style = body(), color = St.Ink2, textAlign = TextAlign.Center)
            }
            TourButton(stringResource(R.string.tour_start)) { tour.finish() }
        }
    }
}

@Composable
private fun body(size: Int = 19) = MaterialTheme.typography.bodyLarge.copy(fontSize = size.sp, lineHeight = (size * 1.45f).sp)

@Composable
private fun TourButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(30.dp)).background(St.Green).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 21.sp, fontWeight = FontWeight(650)), color = Color.White) }
}

@Composable
private fun SkipTour(onSkip: () -> Unit) {
    Text(
        stringResource(R.string.tour_skip_tour),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
        color = St.Ink2,
        modifier = Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onSkip).padding(horizontal = 6.dp, vertical = 12.dp),
    )
}

/** "● ● ━ ● ●  Step 2 of 5 · Skip tour". */
@Composable
private fun Steps(number: Int, total: Int, onSkip: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(total) { i ->
                val n = i + 1
                val width by animateFloatAsState(if (n == number) 22f else 8f, tween(300), label = "dot$i")
                Box(Modifier.size(width.dp, 8.dp).clip(RoundedCornerShape(4.dp)).background(if (n <= number) St.Green else St.Line))
            }
            Text(stringResource(R.string.tour_step, number, total), style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = St.Ink2, modifier = Modifier.padding(start = 4.dp))
        }
        SkipTour(onSkip)
    }
}

/** Pops its content in (0 → 125% → 100%) after [delayMs]. */
@Composable
private fun Pop(delayMs: Int = 0, durationMs: Int = 600, content: @Composable () -> Unit) {
    val reduce = LocalReduceMotion.current
    val p = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        kotlinx.coroutines.delay(delayMs.toLong())
        p.animateTo(1f, tween(durationMs, easing = LinearEasing))
    }
    Box(Modifier.graphicsLayer {
        val t = p.value
        val s = if (t < 0.6f) 1.25f * (t / 0.6f) else 1.25f - 0.25f * ((t - 0.6f) / 0.4f)
        scaleX = s
        scaleY = s
        alpha = (t / 0.3f).coerceAtMost(1f)
    }) { content() }
}

/** The big green tick with oil-gold and green dots bursting out of it. */
@Composable
private fun Celebration() {
    val reduce = LocalReduceMotion.current
    val burst = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) burst.animateTo(1f, tween(1020, easing = LinearEasing)) }
    val colors = listOf(St.Gold, St.Green, Ring, St.GreenDone)
    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        if (!reduce) {
            Canvas(Modifier.size(88.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                repeat(10) { i ->
                    val delay = (120 + (i % 3) * 40) / 1020f
                    val t = ((burst.value - delay) / (900f / 1020f)).coerceIn(0f, 1f)
                    if (t <= 0f || t >= 1f) return@repeat
                    val eased = CubicBezierEasing(0f, 0f, 0.58f, 1f).transform(t)
                    val angle = Math.toRadians(i * 36.0 - 90.0)
                    val dist = 92.dp.toPx() * eased
                    val r = 5.dp.toPx() * (0.4f + 0.6f * eased)
                    drawCircle(colors[i % 4].copy(alpha = 1f - t), r, c + Offset((kotlin.math.cos(angle) * dist).toFloat(), (kotlin.math.sin(angle) * dist).toFloat()))
                }
            }
        }
        Pop(durationMs = 520) {
            Box(Modifier.size(88.dp).clip(CircleShape).background(St.Green), contentAlignment = Alignment.Center) {
                Icon(StIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
            }
        }
    }
}
