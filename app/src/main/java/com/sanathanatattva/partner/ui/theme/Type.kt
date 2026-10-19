package com.sanathanatattva.partner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R

// Anek Kannada is one variable font (wght 100–800, wdth 75–125) covering Latin and Kannada.
@OptIn(ExperimentalTextApi::class)
private fun anek(weight: Int, width: Float) = Font(
    resId = R.font.anek_kannada,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width)),
)

private val weights = listOf(400, 450, 500, 600, 650, 700, 800)

/** Body text. */
val Anek = FontFamily(weights.map { anek(it, 100f) })

/** Headlines and big numbers: the canvas sets these at font-stretch ~92%. */
val AnekNarrow = FontFamily(weights.map { anek(it, 92f) })

val SemiBold650 = FontWeight(650)

/**
 * Kannada script has tall vowel signs and conjuncts below the line, so it needs more
 * line height than Latin (the canvas's Kannada twins use ~1.4).
 */
fun stTypography(kannada: Boolean): Typography {
    fun lh(latin: Float, kn: Float): TextUnit = (if (kannada) kn else latin).em
    fun style(size: Int, weight: FontWeight, family: FontFamily = Anek, latinLh: Float, knLh: Float = 1.4f, tracking: Float = 0f) =
        TextStyle(
            fontFamily = family,
            fontWeight = weight,
            fontSize = size.sp,
            lineHeight = lh(latinLh, maxOf(knLh, latinLh)),
            letterSpacing = tracking.em,
        )
    return Typography(
        displayMedium = style(46, FontWeight.Bold, AnekNarrow, latinLh = 1.0f, knLh = 1.3f, tracking = -0.01f),
        headlineLarge = style(40, FontWeight.Bold, AnekNarrow, latinLh = 1.1f, knLh = 1.35f, tracking = -0.01f),
        headlineMedium = style(32, FontWeight.Bold, AnekNarrow, latinLh = 1.15f, knLh = 1.4f, tracking = -0.01f),
        titleLarge = style(22, FontWeight.Bold, AnekNarrow, latinLh = 1.15f),
        titleMedium = style(19, FontWeight.SemiBold, latinLh = 1.3f),
        bodyLarge = style(19, FontWeight.Normal, latinLh = 1.45f, knLh = 1.5f),
        bodyMedium = style(17, FontWeight.Normal, latinLh = 1.4f, knLh = 1.5f),
        bodySmall = style(15, FontWeight.Normal, latinLh = 1.35f, knLh = 1.45f),
        labelLarge = style(22, SemiBold650, latinLh = 1.2f),
        labelMedium = style(17, FontWeight.SemiBold, latinLh = 1.3f),
        labelSmall = style(14, FontWeight.Bold, latinLh = 1.3f),
    )
}
