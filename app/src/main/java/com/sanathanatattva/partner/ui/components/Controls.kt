package com.sanathanatattva.partner.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlin.math.PI
import kotlin.math.cos

private val ButtonShape = RoundedCornerShape(20.dp)

/** The one big next action on a screen (64dp, deep green). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    arrow: Boolean = true,
    loading: Boolean = false,
    enabled: Boolean = true,
    /** Saved: the label gives way to a tick that pops in (pair it with a confirm haptic). */
    done: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading && !done,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = St.Green,
            contentColor = St.Cream,
            disabledContainerColor = if (loading || done) St.Green else St.Track,
            disabledContentColor = if (loading || done) St.Cream else St.Ink2,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier.fillMaxWidth().height(64.dp),
    ) {
        if (done) {
            val reduce = LocalReduceMotion.current
            val pop = remember { Animatable(if (reduce) 1f else 0f) }
            LaunchedEffect(Unit) { pop.animateTo(1f, tween(420, easing = OilEase)) }
            Icon(
                StIcons.Check,
                contentDescription = stringResource(R.string.saved),
                modifier = Modifier.size(30.dp).graphicsLayer { scaleX = 0.4f + 0.6f * pop.value; scaleY = scaleX; alpha = pop.value },
            )
        } else if (loading) {
            CircularProgressIndicator(color = St.Cream, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
            if (arrow) {
                Spacer(Modifier.width(12.dp))
                Icon(StIcons.ArrowRight, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** Quieter full-width action on cream (60dp, beige outline). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    green: Boolean = false,
    loading: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        shape = ButtonShape,
        border = BorderStroke(2.dp, if (green) St.Green else St.Line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = St.Paper, contentColor = if (green) St.Green else St.Ink),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier.fillMaxWidth().height(60.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(color = St.Green, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
        } else if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        // Kannada labels run longer; shrink a little rather than wrap inside a 60dp button.
        Text(
            text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (green) FontWeight.Bold else FontWeight.SemiBold),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 19.sp),
        )
    }
}

@Composable
fun Emblem(size: Dp, modifier: Modifier = Modifier, describe: Boolean = false) {
    Image(
        painter = painterResource(R.drawable.emblem),
        contentDescription = if (describe) stringResource(R.string.emblem_description) else null,
        modifier = modifier.size(size).clip(CircleShape),
    )
}

/** "Voice on" pill with the speaking-bars motif. */
@Composable
fun VoiceToggle(on: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, St.Line, RoundedCornerShape(24.dp))
            .background(St.Paper)
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .padding(start = 14.dp, end = 18.dp),
    ) {
        VoiceBars(Modifier.size(40.dp, 22.dp), active = on)
        Text(
            stringResource(if (on) R.string.intro_voice_on else R.string.intro_voice_off),
            style = MaterialTheme.typography.labelMedium,
            color = St.Ink,
        )
    }
}

/** Speaker with four bars that rise and fall while a voice is (or can be) speaking. */
@Composable
fun VoiceBars(modifier: Modifier = Modifier, active: Boolean = true) {
    val clock by rememberLoop(1100, still = 0.25f)
    Canvas(modifier) {
        viewBox(40f, 22f) {
            fill(SpeakerShape, St.Green)
            listOf(16f to 0, 22f to -350, 28f to -700, 34f to -200).forEach { (x, delay) ->
                val t = clock.delayed(delay, 1100)
                val h = if (active) 0.35f + 0.65f * (0.5f - 0.5f * cos(t * 2f * PI.toFloat())) else 0.35f
                box(x, 11f - 8f * h, 3f, 16f * h, r = 1.5f, fill = if (active) St.Gold else St.Line)
            }
        }
    }
}

private val SpeakerShape = svgPath("M2 8h4l5-4v14l-5-4H2z")

/** Intro progress: done = mint, current = long green pill, upcoming = sand. */
@Composable
fun StepDots(count: Int, current: Int, modifier: Modifier = Modifier, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 28.dp else 10.dp, tween(450, easing = OilEase), label = "dotW")
            val color by animateColorAsState(
                when {
                    i == current -> St.Green
                    i < current -> St.GreenDone
                    else -> St.Line
                },
                tween(450), label = "dotC",
            )
            Box(Modifier.size(width, 10.dp).clip(RoundedCornerShape(5.dp)).background(color))
        }
    }
}

/** Big labelled field from the Login design: 60dp box, 2dp outline, green focus halo. */
@Composable
fun StTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    error: String? = null,
    /** Short note under the field (e.g. "As written in your bank passbook"). */
    helper: String? = null,
    /** Fixed text before the input, e.g. "+91". */
    prefix: String? = null,
    /** Shows the green "looks good" tick at the end. */
    valid: Boolean = false,
    readOnly: Boolean = false,
    /** Small graphic before the input, e.g. the blue IND strip of a number plate. */
    leadingBadge: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    labelEnd: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    contentType: ContentType? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        error != null -> St.Brick
        focused -> St.Green
        else -> St.FieldBorder
    }
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 30.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = St.Ink, modifier = Modifier.weight(1f))
            labelEnd?.invoke()
        }
        Spacer(Modifier.height(if (labelEnd == null) 8.dp else 4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            readOnly = readOnly,
            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal, fontSize = 20.sp, color = St.Ink),
            cursorBrush = SolidColor(St.Green),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    if (error != null) error(error)
                    if (contentType != null) this.contentType = contentType
                },
            decorationBox = { inner ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .then(if (focused && error == null) Modifier.focusHalo() else Modifier)
                        .background(St.Paper, shape)
                        .border(if (focused) 2.5.dp else 2.dp, borderColor, shape)
                        .padding(start = 16.dp, end = if (trailing != null) 4.dp else 16.dp),
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = if (focused) St.Green else St.Ink2, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    if (leadingBadge != null) {
                        leadingBadge()
                        Spacer(Modifier.width(12.dp))
                    }
                    if (prefix != null) {
                        Text(prefix, style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp), color = St.Ink2)
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.width(2.dp).height(32.dp).background(St.Divider))
                        Spacer(Modifier.width(12.dp))
                    }
                    Box(Modifier.weight(1f)) { inner() }
                    if (valid && error == null) {
                        Icon(
                            StIcons.Check, contentDescription = stringResource(R.string.signup_looks_good), tint = St.Green,
                            modifier = Modifier.padding(start = 8.dp).size(22.dp),
                        )
                    }
                    trailing?.invoke()
                }
            },
        )
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = St.Brick,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            )
        } else if (helper != null) {
            Text(
                helper,
                style = MaterialTheme.typography.bodySmall,
                color = St.Ink2,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            )
        }
    }
}

private fun Modifier.focusHalo(): Modifier = drawBehind {
    val spread = 4.dp.toPx()
    drawRoundRect(
        color = St.Green.copy(alpha = 0.16f),
        topLeft = Offset(-spread, -spread),
        size = Size(size.width + spread * 2, size.height + spread * 2),
        cornerRadius = CornerRadius(16.dp.toPx() + spread),
    )
}

/** Soft lift used under the emblem and sheets. */
fun Modifier.softShadow(elevation: Dp, shape: Shape, color: Color = St.GreenDeep): Modifier =
    shadow(elevation, shape, clip = false, ambientColor = color.copy(alpha = 0.4f), spotColor = color.copy(alpha = 0.5f))

/** The canvas's dashed outline ("border: 2.5px dashed"), for "add" slots. */
fun Modifier.dashedBorder(width: Dp, color: Color, radius: Dp, dash: Dp = 8.dp, gap: Dp = 6.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(w / 2, w / 2),
        size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx() - w / 2),
        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))),
    )
}

/** Inner-page header (canvas: 48dp back circle + 18sp title, pulled 8dp left). */
@Composable
fun BackBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().offset(x = (-8).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(StIcons.ArrowLeft, contentDescription = stringResource(R.string.signup_back), tint = St.Ink, modifier = Modifier.size(26.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = St.Ink, modifier = Modifier.weight(1f))
        if (trailing != null) {
            trailing()
            Spacer(Modifier.width(8.dp))
        }
    }
}
