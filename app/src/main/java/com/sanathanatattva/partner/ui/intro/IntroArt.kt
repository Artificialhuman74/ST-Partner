package com.sanathanatattva.partner.ui.intro

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.around
import com.sanathanatattva.partner.ui.components.box
import com.sanathanatattva.partner.ui.components.delayed
import com.sanathanatattva.partner.ui.components.dot
import com.sanathanatattva.partner.ui.components.fill
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.label
import com.sanathanatattva.partner.ui.components.line
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.theme.St
import kotlin.math.PI
import kotlin.math.cos

// The three intro illustrations, drawn in the canvas SVGs' 350×380 units.

private const val W = 350f
private const val H = 380f
private val Rock = CubicBezierEasing(0.36f, 0f, 0.64f, 1f)
private val Drop = CubicBezierEasing(0.55f, 0f, 0.6f, 1f)

/** Speaking bars: scaleY .35 ↔ 1 on a 1.1s ease-in-out cycle. */
private fun voiceLevel(clock: Float, delayMs: Int): Float {
    val t = clock.delayed(delayMs, 1100)
    return 0.35f + 0.65f * (0.5f - 0.5f * cos(t * 2f * PI.toFloat()))
}

private fun DrawScope.backdrop() = dot(175f, 205f, 150f, fill = St.SandDeep)

// ── 1 · Your phone rings ────────────────────────────────────────────────

private object P1 {
    val arcs = listOf(
        "M98 172 Q86 197 98 222" to 0, "M84 158 Q66 197 84 236" to 180, "M70 144 Q46 197 70 250" to 360,
        "M252 172 Q264 197 252 222" to 0, "M266 158 Q284 197 266 236" to 180, "M280 144 Q304 197 280 250" to 360,
    ).map { (d, delay) -> svgPath(d) to delay }
    val canL = svgPath("M152 140 q0-7 7-7 h4 q7 0 7 7 v34 q0 6-6 6 h-6 q-6 0-6-6z")
    val canR = svgPath("M180 140 q0-7 7-7 h4 q7 0 7 7 v34 q0 6-6 6 h-6 q-6 0-6-6z")
    val minus = svgPath("M139 276h14")
    val tick = svgPath("M197 276.5l5 5 9-10")
    val bubble = svgPath("M206 18 h96 a24 24 0 0 1 0 48 h-74 l-16 12 4-14 a24 24 0 0 1 -10 -46z")
    val speaker = svgPath("M216 36h5l6-5v18l-6-5h-5z")
}

@Composable
fun RingingPhoneArt(modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val clock by rememberLoop(2600, still = 0.6f)
    val pulse by rememberLoop(1600, still = 1f)
    val voice by rememberLoop(1100, still = 0.25f)
    val newOrder = stringResource(R.string.art_new_order)
    val oil = stringResource(R.string.art_groundnut_oil)
    val description = stringResource(R.string.intro1_art)
    Canvas(modifier.semantics { contentDescription = description }) {
        viewBox(W, H) {
            backdrop()
            P1.arcs.forEach { (path, delay) ->
                val t = clock.delayed(delay, 2600)
                val a = kf(t, 0f to 0f, 0.08f to 1f, 0.44f to 0f, easing = EaseOut)
                if (a > 0f) line(path, St.Gold, 4.5f, alpha = a)
            }
            val tilt = kf(
                clock, 0f to 0f, 0.05f to -4f, 0.10f to 4f, 0.15f to -3f, 0.20f to 3f, 0.26f to -1f, 0.31f to 0f,
                easing = Rock,
            )
            around(175f, 247f, rotate = tilt) {
                box(110f, 72f, 130f, 250f, 26f, fill = St.Ink)
                box(118f, 82f, 114f, 230f, 19f, fill = St.Cream)
                box(143f, 96f, 64f, 18f, 9f, fill = St.Green)
                label(measurer, newOrder, 175f, 109f, 10f, St.Cream, tracking = 0.6f)
                box(155f, 124f, 12f, 9f, 2f, fill = St.Green, stroke = St.Ink)
                fill(P1.canL, St.Gold); line(P1.canL, St.Ink, 2f)
                box(183f, 124f, 12f, 9f, 2f, fill = St.Green, stroke = St.Ink)
                fill(P1.canR, St.Gold); line(P1.canR, St.Ink, 2f)
                box(152f, 148f, 18f, 12f, fill = St.Cream, alpha = 0.92f)
                box(180f, 148f, 18f, 12f, fill = St.Cream, alpha = 0.92f)
                label(measurer, "2 × 5 L", 175f, 208f, 19f, St.Ink)
                label(measurer, oil, 175f, 226f, 12f, St.Ink2, weight = FontWeight.Normal)
                dot(146f, 276f, 17f, fill = St.Brick)
                line(P1.minus, St.Cream, 3f)
                val p = OilEase.transform(pulse)
                around(204f, 276f, sx = 1f + 0.9f * p) { dot(204f, 276f, 17f, fill = St.Green, alpha = 0.65f * (1f - p)) }
                dot(204f, 276f, 17f, fill = St.Green)
                line(P1.tick, St.Cream, 3f)
            }
            val pop = kf(clock, 0f to 0f, 0.12f to 1f, easing = OilEase)
            val fade = kf(clock, 0.88f to 1f, 1f to 0f)
            around(200f, 78f, sx = 0.7f + 0.3f * pop) {
                val a = pop * fade
                fill(P1.bubble, St.Paper, alpha = a)
                line(P1.bubble, St.Line, 2f, alpha = a)
                fill(P1.speaker, St.Green, alpha = a)
                listOf(238f to 0, 248f to -350, 258f to -700, 268f to -200, 278f to 0, 288f to -700).forEach { (x, d) ->
                    val h = 24f * voiceLevel(voice, d)
                    box(x, 42f - h / 2f, 4f, h, 2f, fill = St.Gold, alpha = a)
                }
            }
        }
    }
}

// ── 2 · Pack it, or pick it up ──────────────────────────────────────────

private object P2 {
    val scallops = listOf(
        "M24 60h31v12a15.5 15.5 0 0 1-31 0z" to true, "M55 60h31v12a15.5 15.5 0 0 1-31 0z" to false,
        "M86 60h31v12a15.5 15.5 0 0 1-31 0z" to true, "M117 60h31v12a15.5 15.5 0 0 1-31 0z" to false,
        "M148 60h31v12a15.5 15.5 0 0 1-31 0z" to true, "M179 60h33v12a16.5 16.5 0 0 1-33 0z" to false,
    ).map { (d, green) -> svgPath(d) to green }
    val shelf = svgPath("M48 138h140")
    val can = svgPath("M101 112q0-6 6-6h4q6 0 6 6v30q0 5-5 5h-6q-5 0-5-5z")
    val handle = svgPath("M100 182c0-14 8-20 23-20s23 6 23 20")
    val bag = svgPath("M86 186h74l-6 66H92z")
    val bagTop = svgPath("M86 186h74l-3 12H89z")
    val tick = svgPath("M145.5 200.5l4.5 4.5 8-9")
    val trail = svgPath("M240 236 C 214 252 196 246 176 224")
    val arrow = svgPath("M170 232l5-10 9 6")
    val person = svgPath("M276 159a9 7 0 0 1 18 0")
}

@Composable
fun PackPickupArt(modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val clock by rememberLoop(3600, still = 0.7f)
    val march by rememberLoop(1400)
    val pickup = stringResource(R.string.art_pickup_code)
    val showShop = stringResource(R.string.art_show_shop)
    val description = stringResource(R.string.intro2_art)
    Canvas(modifier.semantics { contentDescription = description }) {
        viewBox(W, H) {
            backdrop()
            box(34f, 86f, 168f, 184f, fill = St.Paper, stroke = St.Ink)
            box(24f, 40f, 188f, 20f, 4f, fill = St.Green, stroke = St.Ink)
            P2.scallops.forEach { (p, green) -> fill(p, if (green) St.Green else St.Cream); line(p, St.Ink, 2f) }
            line(P2.shelf, St.Ink, 2f)
            box(56f, 112f, 12f, 26f, 4f, fill = St.Gold, stroke = St.Ink)
            box(74f, 116f, 12f, 22f, 4f, fill = St.Gold, stroke = St.Ink)
            box(160f, 112f, 12f, 26f, 4f, fill = St.Gold, stroke = St.Ink)
            box(20f, 252f, 200f, 34f, 6f, fill = St.Wood, stroke = St.Ink)
            // Two cans drop into the bag, one after the other.
            listOf(0f to 0, 28f to 450).forEach { (dx, delay) ->
                val t = clock.delayed(delay, 3600)
                val y = kf(t, 0f to -14f, 0.10f to 0f, 0.38f to 64f, 0.44f to 72f, easing = Drop)
                val a = kf(t, 0f to 0f, 0.10f to 1f, 0.38f to 1f, 0.44f to 0f)
                if (a > 0f) translate(dx, y) {
                    box(104f, 98f, 10f, 8f, 2f, fill = St.Green, stroke = St.Ink, alpha = a)
                    fill(P2.can, St.Gold, alpha = a); line(P2.can, St.Ink, 2f, alpha = a)
                }
            }
            line(P2.handle, St.Ink, 3f)
            fill(P2.bag, St.Kraft); line(P2.bag, St.Ink, 2f)
            fill(P2.bagTop, St.KraftDark); line(P2.bagTop, St.Ink, 2f)
            val tick = kf(clock, 0.52f to 0f, 0.62f to 1f, easing = OilEase)
            val tickFade = kf(clock, 0.92f to 1f, 1f to 0f)
            if (tick > 0f) around(152f, 200f, sx = 0.3f + 0.7f * tick) {
                val a = tick * tickFade
                dot(152f, 200f, 15f, fill = St.Green, stroke = St.Cream, strokeWidth = 3f, alpha = a)
                line(P2.tick, St.Cream, 3f, alpha = a)
            }
            drawPath(
                P2.trail, St.Green,
                style = Stroke(3f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), -28f * march)),
            )
            line(P2.arrow, St.Green, 3f)
            translate(0f, kf(clock, 0f to 0f, 0.5f to -6f, 1f to 0f, easing = EaseInOut)) {
                box(238f, 112f, 94f, 184f, 20f, fill = St.Ink)
                box(245f, 121f, 80f, 166f, 14f, fill = St.Cream)
                dot(285f, 150f, 14f, fill = St.GreenMist)
                dot(285f, 146f, 5f, fill = St.Green)
                fill(P2.person, St.Green)
                label(measurer, pickup, 285f, 186f, 9f, St.Ink2, tracking = 0.8f)
                label(measurer, "4817", 285f, 216f, 27f, St.Green, weight = FontWeight.ExtraBold, tracking = 3f)
                box(257f, 240f, 56f, 26f, 13f, fill = St.Sand)
                label(measurer, showShop, 285f, 257f, 11f, St.Ink)
            }
        }
    }
}

// ── 3 · Code and photo ──────────────────────────────────────────────────

private object P3 {
    val handle = svgPath("M150 284c0-10 6-15 17-15s17 5 17 15")
    val bag = svgPath("M140 286h54l-4 40h-46z")
    val bagTop = svgPath("M140 286h54l-2 9h-50z")
    val corners = listOf("M126 274v-14h14", "M194 260h14v14", "M208 324v14h-14", "M140 338h-14v-14").map(::svgPath)
    val tick = svgPath("M256 215l11 11 22-24")
}

@Composable
fun CodePhotoArt(modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val clock by rememberLoop(5000, still = 0.8f)
    val delivered = stringResource(R.string.art_delivered)
    val description = stringResource(R.string.intro3_art)
    Canvas(modifier.semantics { contentDescription = description }) {
        viewBox(W, H) {
            val t = clock
            val out = kf(t, 0.92f to 1f, 0.97f to 0f)
            backdrop()
            box(40f, 112f, 132f, 214f, fill = St.Paper, stroke = St.Ink)
            box(52f, 124f, 108f, 202f, fill = St.Green, stroke = St.Ink)
            box(80f, 140f, 52f, 24f, 4f, fill = St.Cream, stroke = St.Ink)
            box(64f, 182f, 84f, 54f, 6f, stroke = St.GreenDeep)
            box(64f, 248f, 84f, 54f, 6f, stroke = St.GreenDeep)
            dot(146f, 242f, 5f, fill = St.Gold, stroke = St.Ink)
            box(26f, 324f, 160f, 12f, 4f, fill = St.Wood, stroke = St.Ink)
            label(measurer, "24", 106f, 158f, 16f, St.Ink, weight = FontWeight.ExtraBold)
            line(P3.handle, St.Ink, 2.6f)
            fill(P3.bag, St.Kraft); line(P3.bag, St.Ink, 2f)
            fill(P3.bagTop, St.KraftDark); line(P3.bagTop, St.Ink, 2f)

            val frame = kf(t, 0.34f to 0f, 0.44f to 1f, easing = OilEase)
            if (frame > 0f) around(167f, 299f, sx = 1.2f - 0.2f * frame) {
                P3.corners.forEach { line(it, St.Gold, 4f, alpha = frame * out) }
            }
            val flash = kf(t, 0.48f to 0f, 0.50f to 0.9f, 0.60f to 0f, easing = EaseOut)
            if (flash > 0f) box(126f, 260f, 82f, 78f, 8f, fill = Color.White, alpha = flash)

            box(196f, 38f, 140f, 62f, 16f, fill = St.Paper, stroke = St.Line)
            val ok = kf(t, 0.28f to 0f, 0.32f to 1f, easing = EaseOut) * out
            if (ok > 0f) box(196f, 38f, 140f, 62f, 16f, fill = St.GreenWash, stroke = St.Green, strokeWidth = 3f, alpha = ok)
            val slots = listOf(206f, 226f, 246f, 270f, 290f, 310f)
            slots.forEach { x -> box(x, 54f, 16f, 30f, 5f, fill = St.Sand) }
            "492716".forEachIndexed { i, digit ->
                val start = 0.03f + 0.04f * i
                val a = kf(t, start to 0f, start + 0.03f to 1f, easing = OilEase) * out
                if (a > 0f) label(measurer, digit.toString(), slots[i] + 8f, 76f, 20f, St.Ink, weight = FontWeight.ExtraBold, alpha = a)
            }

            val badge = kf(t, 0.56f to 0f, 0.66f to 1f, easing = OilEase)
            if (badge > 0f) around(272f, 230f, sx = 0.4f + 0.6f * badge) {
                val a = badge * out
                dot(272f, 214f, 38f, fill = St.Green, alpha = a)
                dot(272f, 214f, 46f, stroke = St.Gold, strokeWidth = 3f, alpha = a)
                line(P3.tick, St.Cream, 6f, alpha = a)
                label(measurer, delivered, 272f, 292f, 18f, St.Green, weight = FontWeight.ExtraBold, alpha = a)
            }
        }
    }
}
