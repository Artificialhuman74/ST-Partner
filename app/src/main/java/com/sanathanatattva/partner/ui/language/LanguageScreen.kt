package com.sanathanatattva.partner.ui.language

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.ui.components.Emblem
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.PrimaryButton
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.SwayEase
import com.sanathanatattva.partner.ui.components.fill
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.theme.Anek
import com.sanathanatattva.partner.ui.theme.AnekNarrow
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

private enum class Lang(val tag: String) { Kannada("kn"), English("en") }

/** How long the "saved" tick shows before the page fades away. */
private const val SAVED_HOLD_MS = 650L

/** First-run language picker (canvas "Language"). Choosing switches the whole app, voice included. */
@Composable
fun LanguageScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var lang by rememberSaveable {
        val current = AppCompatDelegate.getApplicationLocales().get(0)?.language
        mutableStateOf(if (current == "en") Lang.English else Lang.Kannada)
    }
    val kn = lang == Lang.Kannada
    var saved by remember { mutableStateOf(false) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
    // Most phones have less height than the 844dp design once bars are drawn; tighten so the
    // switch preview, the point of this screen, stays above the button.
    val compact = maxHeight < 780.dp
    Column(Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = if (compact) 12.dp else 24.dp, bottom = 20.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Emblem(if (compact) 40.dp else 48.dp)
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = St.GreenDeep)
            }
            Spacer(Modifier.height(if (compact) 16.dp else 28.dp))
            Text(
                stringResource(R.string.language_title_kn),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = Anek,
                    fontSize = if (compact) 28.sp else 32.sp,
                    lineHeight = 1.4.em,
                    localeList = LocaleList("kn"),
                ),
                color = St.Ink,
            )
            Text(
                stringResource(R.string.language_title_en),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (compact) 23.sp else 26.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.2.em),
                color = St.Ink2,
            )
            Spacer(Modifier.height(if (compact) 16.dp else 22.dp))
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp)) {
                LanguageCard(
                    glyph = "ಕ",
                    name = stringResource(R.string.language_kannada_native),
                    nameKannada = true,
                    other = stringResource(R.string.language_kannada_in_en),
                    selected = kn,
                    compact = compact,
                    onSelect = { lang = Lang.Kannada },
                )
                LanguageCard(
                    glyph = "A",
                    name = stringResource(R.string.language_english_native),
                    nameKannada = false,
                    other = stringResource(R.string.language_english_in_kn),
                    selected = !kn,
                    compact = compact,
                    onSelect = { lang = Lang.English },
                )
            }
            Spacer(Modifier.height(if (compact) 12.dp else 16.dp))
            SwitchPreview(kn, compact)
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton(
            text = stringResource(if (kn) R.string.language_cta_kn else R.string.language_cta_en),
            done = saved,
            onClick = {
                // Optimistic: the app is in the new language at once (MainActivity handles the
                // locale change in place, no restart), the button turns into a tick, and then the
                // page fades away. The "chosen" flag is saved behind it, outside this screen's
                // scope so leaving can't cancel it.
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                saved = true
                val current = AppCompatDelegate.getApplicationLocales().get(0)?.language
                if (current != lang.tag) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang.tag))
                val settings = context.appContainer.settings
                CoroutineScope(Dispatchers.IO).launch { settings.setLanguageChosen() }
            },
        )
        // Let the tick land before the page fades away.
        LaunchedEffect(saved) {
            if (saved) {
                delay(SAVED_HOLD_MS)
                onDone()
            }
        }
    }
    }
}

@Composable
private fun LanguageCard(
    glyph: String,
    name: String,
    nameKannada: Boolean,
    other: String,
    selected: Boolean,
    compact: Boolean,
    onSelect: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(24.dp)
    val border by animateColorAsState(if (selected) St.Green else St.LineSoft, tween(350, easing = OilEase), label = "border")
    val fill by animateColorAsState(if (selected) St.GreenWash else St.Paper, tween(350, easing = OilEase), label = "fill")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 86.dp else 104.dp)
            .clip(shape)
            .background(fill)
            .border(3.dp, border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { if (!selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onSelect() })
            .padding(start = 16.dp, end = 18.dp, top = if (compact) 10.dp else 16.dp, bottom = if (compact) 10.dp else 16.dp),
    ) {
        Box(
            Modifier.size(if (compact) 56.dp else 64.dp).clip(RoundedCornerShape(18.dp)).background(St.Sand),
            contentAlignment = Alignment.Center,
        ) {
            Text(glyph, style = TextStyle(fontFamily = Anek, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 1.em), color = St.Green)
        }
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = TextStyle(fontFamily = Anek, fontWeight = FontWeight.Bold, fontSize = if (compact) 28.sp else 32.sp, lineHeight = 1.3.em),
                color = St.Ink,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(other, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = if (nameKannada) 1.3.em else 1.45.em), color = St.Ink2)
                Icon(StIcons.Speaker, contentDescription = null, tint = St.Ink2, modifier = Modifier.size(18.dp))
            }
        }
        AnimatedContent(
            targetState = selected,
            transitionSpec = { (scaleIn(tween(500, easing = OilEase), initialScale = 0.4f) + fadeIn(tween(300))) togetherWith fadeOut(tween(150)) },
            label = "check",
        ) { on ->
            if (on) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(St.Green), contentAlignment = Alignment.Center) {
                    Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(20.dp))
                }
            } else {
                Box(Modifier.size(36.dp).border(2.5.dp, St.FieldBorder, CircleShape))
            }
        }
    }
}

/**
 * The "whole app changes" demo: a mini phone whose labels pour from the other language into
 * the chosen one behind a gold oil band, then the Settings hint lights up.
 */
@Composable
private fun SwitchPreview(kn: Boolean, compact: Boolean) {
    val clock by rememberLoop(5600, still = 0.5f)
    val t = clock
    val allText = stringResource(if (kn) R.string.language_all_kn else R.string.language_all_en)
    val laterText = stringResource(if (kn) R.string.language_later_kn else R.string.language_later_en)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(St.Sand)
            .padding(if (compact) 12.dp else 16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Box(
            Modifier
                // Same drawing, scaled down (layout size too) on short screens.
                .size(if (compact) 98.dp else 120.dp, if (compact) 161.dp else 196.dp)
                .wrapContentSize(unbounded = true)
                .requiredSize(120.dp, 196.dp)
                .graphicsLayer {
                    val k = if (compact) 0.82f else 1f
                    scaleX = k; scaleY = k
                }
                .clip(RoundedCornerShape(22.dp))
                .background(St.Ink)
                .padding(6.dp)
                .clearAndSetSemantics { },
        ) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(17.dp)).background(St.Cream)) {
                // Old language wipes away top-down while the new one is revealed under the band.
                val wipe = kf(t, 0f to 0f, 0.14f to 0f, 0.40f to 1f, easing = SwayEase)
                val fromAlpha = if (t < 0.91f) 1f else (t - 0.91f) / 0.09f
                val toAlpha = kf(t, 0f to 1f, 0.86f to 1f, 0.96f to 0f)
                MiniScreen(
                    kn = !kn,
                    modifier = Modifier
                        .graphicsLayer { alpha = if (t in 0.40f..0.91f) 0f else fromAlpha }
                        .drawWithContent {
                            val top = if (t <= 0.90f) size.height * wipe else 0f
                            clipRect(top = top) { this@drawWithContent.drawContent() }
                        },
                )
                MiniScreen(
                    kn = kn,
                    modifier = Modifier
                        .graphicsLayer { alpha = if (t < 0.14f) 0f else toAlpha }
                        .drawWithContent { clipRect(bottom = size.height * wipe) { this@drawWithContent.drawContent() } },
                )
                if (t in 0.14f..0.40f) {
                    val bandY = with(LocalDensity.current) { (-26f + 212f * wipe).dp }
                    Canvas(Modifier.offset(y = bandY).size(108.dp, 26.dp)) {
                        viewBox(108f, 26f) { fill(Band, St.Gold) }
                    }
                }
                val ping = kf(t, 0f to 0f, 0.44f to 0f, 0.66f to 1f, easing = OilEase)
                if (t in 0.44f..0.66f) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(26.dp)
                            .graphicsLayer {
                                val s = 0.6f + 1.3f * ping
                                scaleX = s; scaleY = s
                                alpha = if (t < 0.48f) kf(t, 0.44f to 0f, 0.48f to 0.9f) else 0.9f * (1f - ping)
                            }
                            .border(2.5.dp, St.Gold, CircleShape),
                    )
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                allText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 1.4.em),
                color = St.Ink,
            )
            val glow = kf(t, 0f to 0f, 0.44f to 0f, 0.52f to 0.28f, 0.78f to 0.28f, 0.92f to 0f)
            val turn = kf(t, 0f to 0f, 0.44f to 0f, 0.64f to 120f, easing = OilEase)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .offset(x = (-6).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(St.Gold.copy(alpha = glow))
                    .padding(6.dp),
            ) {
                Icon(
                    StIcons.Gear, contentDescription = null, tint = St.Green,
                    modifier = Modifier.padding(top = 1.dp).size(24.dp).graphicsLayer { rotationZ = turn },
                )
                Text(laterText, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp, lineHeight = 1.4.em), color = St.Green)
            }
        }
    }
}

private val Band = svgPath("M0 4Q13.5 0 27 4T54 4T81 4T108 4V18Q94.5 26 81 20T54 20T27 20T0 18Z")

/** Decorative, so its type stays put when the phone's font size is raised. */
@Composable
private fun fixed(size: Float): TextUnit = with(LocalDensity.current) { size.dp.toSp() }

@Composable
private fun MiniScreen(kn: Boolean, modifier: Modifier = Modifier) {
    @Composable
    fun s(@StringRes knRes: Int, @StringRes enRes: Int) = stringResource(if (kn) knRes else enRes)
    val text = TextStyle(fontFamily = Anek, fontWeight = FontWeight.Bold, lineHeight = 1.2.em)
    Column(
        modifier.fillMaxSize().background(St.Cream).padding(horizontal = 9.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(s(R.string.language_preview_home_kn, R.string.language_preview_home_en), style = text.copy(fontSize = fixed(12f)), color = St.Ink, modifier = Modifier.weight(1f))
            Icon(StIcons.Gear, contentDescription = null, tint = St.Ink2, modifier = Modifier.size(13.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().height(26.dp).clip(RoundedCornerShape(13.dp)).background(St.Green).padding(horizontal = 8.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(St.Gold))
            Text(s(R.string.language_preview_online_kn, R.string.language_preview_online_en), style = text.copy(fontSize = fixed(11f)), color = St.Cream, maxLines = 1)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(St.Paper)
                .border(1.5.dp, St.Divider, RoundedCornerShape(10.dp))
                .padding(horizontal = 7.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(s(R.string.language_preview_order_kn, R.string.language_preview_order_en), style = text.copy(fontSize = fixed(10f)), color = St.GoldInk, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(7.dp, 13.dp).clip(RoundedCornerShape(2.dp)).background(St.Gold))
                Text("2 × 5 L", style = text.copy(fontFamily = AnekNarrow, fontSize = fixed(13f)), color = St.Ink)
            }
            Text(s(R.string.language_preview_oil_kn, R.string.language_preview_oil_en), style = text.copy(fontWeight = FontWeight.Normal, fontSize = fixed(10f), lineHeight = 1.3.em), color = St.Ink2)
        }
        Box(
            Modifier.fillMaxWidth().height(26.dp).clip(RoundedCornerShape(8.dp)).background(St.Green),
            contentAlignment = Alignment.Center,
        ) {
            Text(s(R.string.language_preview_accept_kn, R.string.language_preview_accept_en), style = text.copy(fontSize = fixed(11f)), color = St.Cream, maxLines = 1)
        }
    }
}
