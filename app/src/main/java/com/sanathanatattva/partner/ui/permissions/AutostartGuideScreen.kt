package com.sanathanatattva.partner.ui.permissions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.alerts.Autostart
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.PrimaryButton
import com.sanathanatattva.partner.ui.components.SecondaryButton
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.box
import com.sanathanatattva.partner.ui.components.dot
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St

private val Highlight = Color(0xFFE2A72E)
private val TrackOff = Color(0xFFC9C2B3)

/** "One more switch on Redmi phones" (canvas "Autostart picture guide"), worded per phone maker. */
@Composable
fun AutostartGuideScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val guide = remember { Autostart.guideFor() }
    if (guide == null) {
        // No steps for this phone: move on, but from an effect, never mid-composition.
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val reduce = LocalReduceMotion.current
    val t by rememberLoop(6600, still = 0.85f)

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).graphicsLayer { translationX = -12.dp.toPx() }) {
                Icon(StIcons.ArrowLeft, contentDescription = stringResource(R.string.signup_back), tint = St.Ink, modifier = Modifier.size(26.dp))
            }
            Text(stringResource(R.string.perm_battery), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = St.Ink, modifier = Modifier.graphicsLayer { translationX = -12.dp.toPx() })
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.guide_title, guide.brand), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp), color = St.Ink, modifier = Modifier.padding(top = 14.dp).semantics { heading() })
            Text(stringResource(R.string.guide_body), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp), color = St.Ink2, modifier = Modifier.padding(top = 6.dp))
            Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                guide.steps.forEachIndexed { i, (title, body) ->
                    // "gd-c1..3": each step glows gold for its third of the 6.6s loop.
                    val lit = if (reduce) 0f else when (i) {
                        0 -> kf(t, 0f to 1f, 0.30f to 1f, 0.36f to 0f)
                        1 -> kf(t, 0.30f to 0f, 0.34f to 1f, 0.63f to 1f, 0.69f to 0f)
                        else -> kf(t, 0.63f to 0f, 0.67f to 1f, 0.97f to 1f, 1f to 0f)
                    }
                    val shape = RoundedCornerShape(20.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(shape)
                            .background(lerp(St.Cream, St.Paper, lit))
                            .border(2.dp, lerp(St.LineSoft, Highlight, lit), shape)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        StepArt(i, t, reduce, Modifier.size(width = 84.dp, height = 64.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(26.dp).clip(CircleShape).background(St.Green), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp), color = St.Cream)
                                }
                                Text(stringResource(title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = St.Ink)
                            }
                            Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = St.Ink2, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(text = stringResource(R.string.guide_open), arrow = false, onClick = { Autostart.open(context, guide) })
            SecondaryButton(text = stringResource(R.string.guide_done), green = true, onClick = {
                Autostart.confirm(context)
                onDone()
            })
        }
    }
}

/** Tiny phone-screen pictures; a tap ripple ("gd-t1..3") on each, and the switch flips in step 3. */
@Composable
private fun StepArt(step: Int, t: Float, reduce: Boolean, modifier: Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(10.dp)).background(St.Sand)) {
        viewBox(84f, 64f) {
            when (step) {
                0 -> { // Settings grid with "Apps"
                    for (r in 0..1) for (c in 0..2) box(10f + c * 24f, 10f + r * 24f, 16f, 16f, 4f, fill = if (r == 1 && c == 1) St.Green else St.SteelDark)
                }
                1 -> { // App list, ST Partner highlighted
                    for (r in 0..2) {
                        box(8f, 8f + r * 17f, 68f, 13f, 4f, fill = if (r == 1) St.GreenTint else St.Paper, stroke = if (r == 1) St.Green else null, strokeWidth = 1.5f)
                        dot(16f, 14.5f + r * 17f, 4f, fill = if (r == 1) St.Gold else St.SteelDark)
                        box(24f, 12.5f + r * 17f, if (r == 1) 36f else 28f, 4f, 2f, fill = St.Ink2.copy(alpha = 0.5f))
                    }
                }
                else -> { // The Autostart switch
                    box(8f, 22f, 36f, 6f, 3f, fill = St.Ink2.copy(alpha = 0.5f))
                    val on = if (reduce) 1f else kf(t, 0.74f to 0f, 0.80f to 1f, 0.97f to 1f, 1f to 0f, easing = OilEase)
                    box(50f, 18f, 26f, 15f, 7.5f, fill = lerp(TrackOff, St.Green, on))
                    dot(57.5f + 12f * on, 25.5f, 6f, fill = Color.White)
                }
            }
            if (!reduce) {
                val (start, cx, cy) = when (step) {
                    0 -> Triple(0.08f, 42f, 42f)
                    1 -> Triple(0.41f, 42f, 31.5f)
                    else -> Triple(0.72f, 63f, 25.5f)
                }
                val p = kf(t, start to 0f, start + 0.04f to 0.5f, start + 0.18f to 1f)
                val a = kf(t, start to 0f, start + 0.04f to 0.9f, start + 0.18f to 0f)
                if (a > 0f) dot(cx, cy, 6f + 10f * p, fill = St.Gold, alpha = a * 0.6f)
            }
        }
    }
}
