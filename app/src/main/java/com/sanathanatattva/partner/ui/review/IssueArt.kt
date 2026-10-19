package com.sanathanatattva.partner.ui.review

import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sanathanatattva.partner.data.signup.ReviewItem
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.delayed
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St

/**
 * Canvas "Rejected": each flagged item shows its own problem fixing itself on a 4.4 s loop
 * (a blurry licence sharpening into its frame, a helmet coming off, a short name growing to
 * the passbook's, the PAN sliding into frame, the pin moving into the area), while a red "!"
 * gives way to a green tick ("rj-warn" / "rj-ok"). Cards are staggered like the canvas
 * ("rj-d1" … "rj-d4").
 */
@Composable
fun IssueArt(item: String, index: Int, fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val loop by rememberLoop(LOOP)
    // Negative CSS delays start a card part-way through its loop.
    val stagger = intArrayOf(0, 1100, 2200, 3300, 550)[index % 5]
    val t = if (reduce) 0f else loop.delayed(-stagger, LOOP)
    Box(modifier.size(72.dp)) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(St.Sand)) {
            when (item) {
                ReviewItem.LICENCE_FRONT, ReviewItem.LICENCE_BACK -> LicenceArt(t, reduce)
                ReviewItem.SELFIE -> Canvas(Modifier.fillMaxSize()) { viewBox(76f, 76f) { selfie(t, reduce) } }
                ReviewItem.BANK -> Canvas(Modifier.fillMaxSize()) { viewBox(76f, 76f) { bank(t, reduce) } }
                ReviewItem.PAN -> Canvas(Modifier.fillMaxSize()) { viewBox(76f, 76f) { pan(t, reduce) } }
                ReviewItem.BASE -> Canvas(Modifier.fillMaxSize()) { viewBox(76f, 76f) { address(t, reduce) } }
                else -> Icon(fallbackIcon, contentDescription = null, tint = St.Brick, modifier = Modifier.align(Alignment.Center).size(30.dp))
            }
        }
        // "rj-warn": the red "!" shrinks away 40–50%, back by 96%. "rj-ok": the tick pops 48–58%, gone by 96%.
        val warn = if (reduce) 1f else kf(t, 0.40f to 1f, 0.50f to 0f, 0.86f to 0f, 0.96f to 1f, easing = OilEase)
        val ok = if (reduce) 0f else kf(t, 0.48f to 0f, 0.58f to 1f, 0.86f to 1f, 0.96f to 0f, easing = OilEase)
        Badge(warn, 0.5f, St.Brick, warnMark = true)
        Badge(ok, 0.4f, St.Green, warnMark = false)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Badge(p: Float, scaleFrom: Float, color: Color, warnMark: Boolean) {
    if (p <= 0f) return
    Box(
        Modifier.align(Alignment.TopEnd)
            .graphicsLayer { translationX = 7.dp.toPx(); translationY = -7.dp.toPx(); alpha = p; scaleX = scaleFrom + (1f - scaleFrom) * p; scaleY = scaleX }
            .size(26.dp).clip(CircleShape).background(St.Paper).padding(2.5.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (warnMark) {
            Canvas(Modifier.size(12.dp)) {
                viewBox(24f, 24f) {
                    drawLine(St.Cream, Offset(12f, 4f), Offset(12f, 14f), strokeWidth = 4f, cap = StrokeCap.Round)
                    drawCircle(St.Cream, 2.4f, Offset(12f, 20f))
                }
            }
        } else {
            Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(13.dp))
        }
    }
}

/** "rj-blur" + "rj-frame": the photo sharpens 24–46% while gold corners close in on it 18–34%. */
@Composable
private fun LicenceArt(t: Float, reduce: Boolean) {
    val blur = if (reduce) 2.2f else kf(t, 0.24f to 2.2f, 0.46f to 0f, 0.86f to 0f, 1f to 2.2f, easing = Wave)
    Box(Modifier.fillMaxSize()) {
        val blurMod = if (Build.VERSION.SDK_INT >= 31) Modifier.blur((blur * 72f / 76f).dp) else Modifier
        Canvas(Modifier.fillMaxSize().then(blurMod)) {
            viewBox(76f, 76f) {
                drawRoundRect(Color(0xFFE7EFDF), Offset(12f, 22f), Size(52f, 33f), androidx.compose.ui.geometry.CornerRadius(5f))
                drawRoundRect(Color(0xFFC9D6C1), Offset(12f, 22f), Size(52f, 33f), androidx.compose.ui.geometry.CornerRadius(5f), style = Stroke(1.5f))
                drawRoundRect(Color(0xFF8B9A86), Offset(17f, 27f), Size(26f, 3.5f), androidx.compose.ui.geometry.CornerRadius(1.75f))
                drawRoundRect(Color(0xFFB4BFAE), Offset(17f, 34f), Size(38f, 3f), androidx.compose.ui.geometry.CornerRadius(1.5f))
                floatArrayOf(17f, 20f, 22.5f, 26f, 28.5f, 31.5f, 34f, 37f, 39.5f).forEachIndexed { i, x ->
                    drawRect(Color(0xFF4A5548), Offset(x, 41f), Size(if (i % 3 == 2) 2f else if (i % 2 == 0) 1.6f else 1f, 9f))
                }
                drawRoundRect(Color(0xFFC9D6C1), Offset(46f, 40f), Size(13f, 10f), androidx.compose.ui.geometry.CornerRadius(2f))
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            viewBox(76f, 76f) {
                val f = if (reduce) 0f else kf(t, 0.18f to 0f, 0.34f to 1f, 0.86f to 1f, 0.96f to 0f, easing = OilEase)
                if (f > 0f) {
                    val s = 1.2f - 0.2f * f
                    scale(s, s, pivot = Offset(38f, 38.5f)) { corners(St.Gold, f) }
                }
            }
        }
    }
}

/** "rj-helmet": the helmet lifts away 22–46% and drops back by 96%. */
private fun DrawScope.selfie(t: Float, reduce: Boolean) {
    drawCircle(Color(0xFFCFE0C9), 25f, Offset(38f, 42f))
    clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(38f, 42f), 25f)) }) {
        drawCircle(FigureInk, 9.5f, Offset(38f, 38f))
        drawPath(SelfieBody, FigureInk)
    }
    val off = if (reduce) 0f else kf(t, 0.22f to 0f, 0.46f to 1f, 0.84f to 1f, 0.96f to 0f, easing = OilEase)
    val a = 1f - off
    if (a > 0f) translate(0f, -24f * off) {
        drawPath(Helmet, Color(0xFF34463A), alpha = a)
        drawLine(St.Gold, Offset(28f, 40.5f), Offset(48f, 40.5f), strokeWidth = 3.2f, cap = StrokeCap.Round, alpha = a)
    }
}

/** "rj-short" → "rj-long": the red short name gives way to the full green one, 22–48%. */
private fun DrawScope.bank(t: Float, reduce: Boolean) {
    drawRoundRect(St.Paper, Offset(12f, 13f), Size(52f, 21f), androidx.compose.ui.geometry.CornerRadius(5f))
    drawRoundRect(Color(0xFFD8C8AB), Offset(12f, 13f), Size(52f, 21f), androidx.compose.ui.geometry.CornerRadius(5f), style = Stroke(1.5f))
    val short = if (reduce) 1f else kf(t, 0.24f to 1f, 0.30f to 0f, 0.90f to 0f, 1f to 1f, easing = EaseOut)
    if (short > 0f) drawRoundRect(St.Brick, Offset(18f, 20.5f), Size(16f, 6f), androidx.compose.ui.geometry.CornerRadius(3f), alpha = short)
    if (!reduce) {
        val longA = kf(t, 0.22f to 0f, 0.28f to 1f, 0.86f to 1f, 0.96f to 0f, easing = OilEase)
        val longW = kf(t, 0.28f to 0.45f, 0.48f to 1f, 0.86f to 1f, 0.96f to 0.45f, easing = OilEase)
        if (longA > 0f) drawRoundRect(St.Green, Offset(18f, 20.5f), Size(36f * longW, 6f), androidx.compose.ui.geometry.CornerRadius(3f), alpha = longA)
    }
    drawRoundRect(St.Green, Offset(12f, 40f), Size(52f, 26f), androidx.compose.ui.geometry.CornerRadius(4f))
    drawRect(St.GreenDeep, Offset(15f, 40f), Size(3f, 26f))
    drawRoundRect(St.Cream, Offset(22f, 49f), Size(32f, 6f), androidx.compose.ui.geometry.CornerRadius(3f))
}

/** "rj-pan" + "rj-fit": the card slides 20 units into frame 20–46%, the corners turn green 44–52%. */
private fun DrawScope.pan(t: Float, reduce: Boolean) {
    val x = if (reduce) 20f else 20f * kf(t, 0.20f to 1f, 0.46f to 0f, 0.86f to 0f, 1f to 1f, easing = OilEase)
    translate(x, 0f) {
        drawRoundRect(Color(0xFFDFE7EF), Offset(14f, 24f), Size(48f, 30f), androidx.compose.ui.geometry.CornerRadius(4f))
        drawRoundRect(Color(0xFF9DB3C9), Offset(14f, 24f), Size(48f, 30f), androidx.compose.ui.geometry.CornerRadius(4f), style = Stroke(1.5f))
        drawPath(PanStripe, Color(0xFF9DB3C9))
        drawRoundRect(Color(0xFF7D8FA3), Offset(18f, 35f), Size(20f, 3f), androidx.compose.ui.geometry.CornerRadius(1.5f))
        drawRoundRect(Color(0xFF5F7286), Offset(18f, 42f), Size(24f, 3.5f), androidx.compose.ui.geometry.CornerRadius(1.75f))
        drawRoundRect(Color(0xFFB6C4D2), Offset(47f, 34f), Size(11f, 14f), androidx.compose.ui.geometry.CornerRadius(1.5f))
    }
    corners(St.Gold, 1f)
    val fit = if (reduce) 0f else kf(t, 0.44f to 0f, 0.52f to 1f, 0.86f to 1f, 0.96f to 0f, easing = EaseOut)
    if (fit > 0f) corners(St.Green, fit)
}

/** "rj-pin" + "rj-zone": the pin arcs back into the dashed area 18–46%, which deepens 44–54%. */
private fun DrawScope.address(t: Float, reduce: Boolean) {
    drawRect(Color(0xFFEFE6D3), Offset.Zero, Size(76f, 76f))
    drawLine(Road, Offset(0f, 52f), Offset(76f, 52f), strokeWidth = 6f)
    drawLine(Road, Offset(48f, 0f), Offset(48f, 76f), strokeWidth = 6f)
    drawLine(Road, Offset(0f, 20f), Offset(76f, 28f), strokeWidth = 4f)
    val zone = if (reduce) 0.08f else kf(t, 0.44f to 0.08f, 0.54f to 0.22f, 0.86f to 0.22f, 1f to 0.08f, easing = EaseOut)
    drawCircle(St.Green, 18f, Offset(30f, 44f), alpha = zone)
    drawCircle(St.Green, 18f, Offset(30f, 44f), style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))))
    // Away (26, −14) → mid (13, −26) → home, then back at the end of the loop.
    val (dx, dy) = when {
        reduce || t <= 0.18f -> 26f to -14f
        t <= 0.32f -> { val p = PinOut.transform((t - 0.18f) / 0.14f); (26f - 13f * p) to (-14f - 12f * p) }
        t <= 0.46f -> { val p = PinIn.transform((t - 0.32f) / 0.14f); (13f - 13f * p) to (-26f + 26f * p) }
        t <= 0.86f -> 0f to 0f
        else -> { val p = (t - 0.86f) / 0.14f; (26f * p) to (-14f * p) }
    }
    translate(dx, dy) {
        drawOval(St.Ink, Offset(25f, 43f), Size(10f, 4f), alpha = 0.25f)
        drawPath(AreaPin, St.Green)
        drawPath(AreaPin, St.GreenDeep, style = Stroke(1.5f))
        drawCircle(St.Cream, 3.4f, Offset(30f, 30f))
    }
}

/** The four L-shaped frame corners shared by the licence and PAN pictures. */
private fun DrawScope.corners(color: Color, alpha: Float) {
    val stroke = Stroke(2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    FrameCorners.forEach { drawPath(it, color, alpha = alpha, style = stroke) }
}

private const val LOOP = 4400
private val Wave = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
private val PinOut = CubicBezierEasing(0.3f, 0f, 0.6f, 1f)
private val PinIn = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val FigureInk = Color(0xFF5F7D5A)
private val Road = Color(0xFFFFFDF8)
private val SelfieBody = svgPath("M21 68c1.5-11 8.5-16 17-16s15.5 5 17 16z")
private val Helmet = svgPath("M25 40a13 14 0 0 1 26 0v4H25z")
private val PanStripe = svgPath("M14 28a4 4 0 0 1 4-4h40a4 4 0 0 1 4 4v2H14z")
private val AreaPin = svgPath("M30 45s-9-8.5-9-15a9 9 0 0 1 18 0c0 6.5-9 15-9 15z")
private val FrameCorners = listOf(
    svgPath("M8 24v-6h6"), svgPath("M62 18h6v6"), svgPath("M68 53v6h-6"), svgPath("M14 59H8v-6"),
)
