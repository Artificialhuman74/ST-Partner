package com.sanathanatattva.partner.ui.signup

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import android.graphics.Matrix
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.data.signup.BLURRY_BELOW
import com.sanathanatattva.partner.data.signup.sharpness
import com.sanathanatattva.partner.ui.components.CameraCapture
import com.sanathanatattva.partner.ui.components.CameraGuide
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.PrimaryButton
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Every signup step: back + "Join as a partner" + "Step n of N", the segmented
 * progress bar (done green, current gold, rest sand), title, body, scrolling content,
 * and one big action pinned at the bottom.
 */
@Composable
fun SignupFrame(
    stepNumber: Int,
    stepCount: Int,
    title: String,
    body: String?,
    onBack: () -> Unit,
    action: String,
    onAction: () -> Unit,
    actionLoading: Boolean = false,
    actionEnabled: Boolean = true,
    actionIcon: ImageVector? = null,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
    ) {
        Row(Modifier.padding(start = 0.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp).size(48.dp).graphicsLayer { translationX = -12.dp.toPx() }) {
                Icon(StIcons.ArrowLeft, contentDescription = stringResource(R.string.signup_back), tint = St.Ink, modifier = Modifier.size(26.dp))
            }
            Text(
                stringResource(R.string.signup_title),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                color = St.Ink,
                modifier = Modifier.weight(1f).graphicsLayer { translationX = -12.dp.toPx() },
            )
            if (stepCount > 0) {
                Text(stringResource(R.string.signup_step, stepNumber, stepCount), style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp), color = St.Ink2)
            }
        }
        // Fix-mode screens (sent back by an admin) pass stepCount = 0: no counter, no bar.
        if (stepCount > 0) StepBar(stepNumber, stepCount, Modifier.padding(top = 6.dp))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = St.Ink,
                modifier = Modifier.padding(top = 20.dp).semantics { heading() },
            )
            if (body != null) {
                Text(body, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp), color = St.Ink2, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(20.dp))
            content()
            Spacer(Modifier.height(16.dp))
        }
        footer?.invoke()
        PrimaryButton(
            text = action,
            onClick = onAction,
            loading = actionLoading,
            enabled = actionEnabled,
            arrow = actionIcon == null,
        )
    }
}

@Composable
private fun StepBar(current: Int, total: Int, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val grow = remember(current) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(current) { grow.animateTo(1f, tween(900, easing = OilEase)) }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(total) { i ->
            val n = i + 1
            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(St.Track)) {
                if (n <= current) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (n == current) {
                                    scaleX = grow.value
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                }
                            }
                            .background(if (n == current) St.Gold else St.Green),
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = St.Green, modifier = Modifier.size(24.dp))
        Text(text, style = MaterialTheme.typography.titleLarge, color = St.Ink, modifier = Modifier.semantics { heading() })
    }
}

@Composable
fun InlineNote(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(St.Sand).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = St.Ink3)
    }
}

@Composable
fun FieldError(text: String?) {
    if (text != null) Text(text, style = MaterialTheme.typography.bodySmall, color = St.Brick, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
}

/** Decodes a downsized, upright thumbnail of a photo taken in the app. */
@Composable
fun rememberThumbnail(path: String?, maxSide: Int = 720): ImageBitmap? {
    val state = produceState<ImageBitmap?>(null, path) {
        value = if (path == null) null else withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
                val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
                val degrees = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                val upright = if (degrees == 0f) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
                upright.asImageBitmap()
            }.getOrNull()
        }
    }
    return state.value
}

/** A document photo slot: empty (tap to shoot) or taken (thumbnail, clarity note, Retake). */
@Composable
fun PhotoTile(
    label: String,
    path: String?,
    onTake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumb = rememberThumbnail(path)
    val sharp by produceState<Double?>(null, path) { value = path?.let { sharpness(File(it)) } }
    val shape = RoundedCornerShape(20.dp)
    if (path == null) {
        Column(
            modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .clip(shape)
                .background(St.Paper)
                .drawBehind {
                    drawRoundRect(
                        St.Line, cornerRadius = CornerRadius(20.dp.toPx()),
                        style = Stroke(2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f))),
                    )
                }
                .clickable(onClick = onTake)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Icon(StIcons.Camera, contentDescription = null, tint = St.Green, modifier = Modifier.size(30.dp))
            Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = St.Ink)
            Text(stringResource(R.string.signup_tap_photo), style = MaterialTheme.typography.bodySmall, color = St.Ink2)
        }
    } else {
        Row(
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(St.Paper)
                .border(2.dp, St.LineSoft, shape)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(width = 120.dp, height = 78.dp).clip(RoundedCornerShape(10.dp)).background(St.Sand)) {
                if (thumb != null) Image(thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            // Label, clarity note and Retake share one column so none of them gets squeezed.
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = St.Ink, maxLines = 1)
                val blurry = sharp != null && sharp!! < BLURRY_BELOW
                if (sharp != null) {
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(if (blurry) St.Gold else St.GreenWash).padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (!blurry) Icon(StIcons.Check, contentDescription = null, tint = St.Green, modifier = Modifier.size(14.dp))
                        Text(
                            stringResource(if (blurry) R.string.signup_maybe_blurry else R.string.signup_clear),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (blurry) St.Ink else St.Green,
                            maxLines = 1,
                        )
                    }
                }
                Row(
                    Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onTake)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(StIcons.Retake, contentDescription = null, tint = St.Green, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.signup_retake), style = MaterialTheme.typography.labelMedium, color = St.Green)
                }
            }
        }
    }
}

/** Full-screen document camera shown over a step. */
@Composable
fun DocumentCamera(
    hint: String,
    newFile: () -> File,
    onCaptured: (File) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    Box(
        Modifier
            .fillMaxSize()
            .background(St.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
    ) {
        CameraCapture(
            front = false,
            guide = CameraGuide.Card,
            hint = hint,
            newFile = newFile,
            onCaptured = onCaptured,
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(48.dp).clip(RoundedCornerShape(24.dp)).background(St.Ink.copy(alpha = 0.6f))) {
            Icon(StIcons.ArrowLeft, contentDescription = stringResource(R.string.signup_back), tint = St.Cream)
        }
    }
}
