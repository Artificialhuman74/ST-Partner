package com.sanathanatattva.partner.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.ui.theme.Anek
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import kotlin.math.floor
import kotlin.math.min

// Helpers for the canvas illustrations: they are drawn in the SVG's own units, so the
// coordinates below match the design files one to one.

/** The canvas's signature ease, cubic-bezier(.22,1,.36,1): quick start, long viscous settle. */
val OilEase: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
val FallEase: Easing = CubicBezierEasing(0.55f, 0f, 0.85f, 0.4f)
val SwayEase: Easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)

fun svgPath(d: String): Path = PathParser().parsePathString(d).toPath()

/** Draws [block] in a [w]×[h] SVG viewBox scaled to fit (contain) and centred. */
inline fun DrawScope.viewBox(w: Float, h: Float, block: DrawScope.() -> Unit) {
    val s = min(size.width / w, size.height / h)
    withTransform({
        translate((size.width - w * s) / 2f, (size.height - h * s) / 2f)
        scale(s, s, Offset.Zero)
    }, block)
}

/** Rotates or scales around an SVG-unit pivot. */
inline fun DrawScope.around(px: Float, py: Float, rotate: Float = 0f, sx: Float = 1f, sy: Float = sx, block: DrawScope.() -> Unit) {
    withTransform({
        if (rotate != 0f) rotate(rotate, Offset(px, py))
        if (sx != 1f || sy != 1f) scale(sx, sy, Offset(px, py))
    }, block)
}

fun DrawScope.fill(path: Path, color: Color, alpha: Float = 1f) = drawPath(path, color, alpha)

fun DrawScope.line(path: Path, color: Color, width: Float, alpha: Float = 1f, cap: StrokeCap = StrokeCap.Round) =
    drawPath(path, color, alpha, Stroke(width, cap = cap, join = StrokeJoin.Round))

fun DrawScope.box(
    x: Float, y: Float, w: Float, h: Float, r: Float = 0f,
    fill: Color? = null, stroke: Color? = null, strokeWidth: Float = 2f, alpha: Float = 1f,
) {
    val radius = CornerRadius(r, r)
    if (fill != null) drawRoundRect(fill, Offset(x, y), Size(w, h), radius, alpha = alpha)
    if (stroke != null) drawRoundRect(stroke, Offset(x, y), Size(w, h), radius, Stroke(strokeWidth, join = StrokeJoin.Round), alpha)
}

fun DrawScope.dot(
    cx: Float, cy: Float, r: Float,
    fill: Color? = null, stroke: Color? = null, strokeWidth: Float = 2f, alpha: Float = 1f,
) {
    if (fill != null) drawCircle(fill, r, Offset(cx, cy), alpha)
    if (stroke != null) drawCircle(stroke, r, Offset(cx, cy), alpha, Stroke(strokeWidth))
}

/** SVG `<text text-anchor="middle">`: [y] is the baseline, size is in viewBox units. */
fun DrawScope.label(
    measurer: TextMeasurer, text: String, x: Float, y: Float, size: Float,
    color: Color, weight: FontWeight = FontWeight.Bold, tracking: Float = 0f, alpha: Float = 1f,
) {
    val style = TextStyle(
        fontFamily = Anek,
        fontWeight = weight,
        fontSize = (size / (density * fontScale)).sp,
        letterSpacing = (tracking / size).em,
        color = color,
    )
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(x - layout.size.width / 2f, y - layout.firstBaseline), alpha = alpha)
}

/**
 * Piecewise keyframes like CSS: `kf(t, 0f to 0f, 0.4f to 1f)` holds outside the range.
 * [easing] applies inside each segment.
 */
fun kf(t: Float, vararg stops: Pair<Float, Float>, easing: Easing = LinearEasing): Float {
    if (t <= stops.first().first) return stops.first().second
    for (i in 1 until stops.size) {
        val (t1, v1) = stops[i]
        if (t <= t1) {
            val (t0, v0) = stops[i - 1]
            val f = if (t1 == t0) 1f else easing.transform((t - t0) / (t1 - t0))
            return v0 + (v1 - v0) * f
        }
    }
    return stops.last().second
}

/** 0→1 progress of [t] across [start]..[end], eased. */
fun seg(t: Float, start: Float, end: Float, easing: Easing = OilEase): Float =
    easing.transform(((t - start) / (end - start)).coerceIn(0f, 1f))

/**
 * A looping 0→1 clock for an illustration with a CSS-style [periodMs].
 * With "Remove animations" on it stops at [still] (a pleasant resting frame).
 */
@Composable
fun rememberLoop(periodMs: Int, still: Float = 0f): State<Float> {
    if (LocalReduceMotion.current) return remember(still) { mutableFloatStateOf(still) }
    val transition = rememberInfiniteTransition(label = "loop")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "loop$periodMs",
    )
}

/** Shifts a loop clock by a CSS animation-delay (negative delays start mid-cycle). */
fun Float.delayed(delayMs: Int, periodMs: Int): Float {
    val shifted = this - delayMs.toFloat() / periodMs
    return shifted - floor(shifted)
}
