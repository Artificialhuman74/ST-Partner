package com.sanathanatattva.partner.ui.signup

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.rememberLoop
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
    LaunchedEffect(current) { grow.animateTo(1f, tween(1000, easing = OilEase)) } // canvas "su-now"
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
/**
 * A document photo slot (canvas "SignupLicence"). Empty: a dashed tile whose camera bobs
 * ("dl-cam"). After a photo, the canvas's sequence plays once: the card swings in
 * ("dl-card"), a gold line scans it ("dl-scan") while the bar fills ("dl-fill"), then
 * "Clear" ("dl-ok"), a tick badge ("dl-badge") and a green edge ("dl-edge").
 */
@Composable
fun PhotoTile(
    label: String,
    path: String?,
    onTake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumb = rememberThumbnail(path)
    val sharp by produceState<Double?>(null, path) { value = path?.let { sharpness(File(it)) } }
    val shape = RoundedCornerShape(22.dp)
    val reduce = LocalReduceMotion.current
    if (path == null) {
        val bob by rememberLoop(1600)
        Row(
            modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(St.Cream)
                .drawBehind {
                    drawRoundRect(
                        TileDash, cornerRadius = CornerRadius(20.dp.toPx()),
                        style = Stroke(2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f))),
                    )
                }
                .clickable(onClick = onTake)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        ) {
            Box(
                Modifier.size(56.dp)
                    .graphicsLayer { translationY = if (reduce) 0f else -4.dp.toPx() * kf(bob, 0f to 0f, 0.5f to 1f, 1f to 0f, easing = EaseInOut) }
                    .clip(RoundedCornerShape(18.dp)).background(St.Sand),
                contentAlignment = Alignment.Center,
            ) { Icon(StIcons.Camera, contentDescription = null, tint = St.Green, modifier = Modifier.size(28.dp)) }
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = St.Ink)
                Text(stringResource(R.string.signup_tap_photo), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp), color = St.Ink2)
            }
        }
        return
    }

    // Plays for a photo taken now; a photo that was already there (draft, back from another step) shows at rest.
    val firstPath = rememberSaveable { path }
    val clock = remember(path) { Animatable(if (reduce || path == firstPath) SCAN_END else SCAN_START) }
    LaunchedEffect(path) {
        if (clock.value < SCAN_END) clock.animateTo(SCAN_END, tween((SCAN_END - SCAN_START).toInt(), easing = LinearEasing))
    }
    val blurry = sharp != null && sharp!! < BLURRY_BELOW
    val t = clock.value
    val done = sharp != null
    // Canvas timeline (ms of its 6.4 s loop), from the moment the card swings in.
    val card = kf(t, 1280f to 0f, 2176f to 1f, easing = OilEase)
    val col = kf(t, 1536f to 0f, 2304f to 1f, easing = OilEase)
    val scanY = kf(t, 2304f to 0f, 3968f to 1f, easing = ScanEase)
    val scanA = kf(t, 2304f to 0f, 2432f to 1f, 3968f to 1f, 4096f to 0f)
    val fill = kf(t, 2304f to 0f, 4352f to 1f, easing = FillEase)
    val upA = if (done) kf(t, 4224f to 1f, 4480f to 0f) else 1f
    val ok = if (done) kf(t, 4352f to 0f, 4736f to 1f, easing = OilEase) else 0f
    val badge = if (done && !blurry) kf(t, 4352f to 0f, 4864f to 1f, easing = OilEase) else 0f
    val edge = if (done) kf(t, 4352f to 0f, 4736f to 1f, easing = EaseOut) else 0f
    val edgeColor = if (blurry) St.Gold else St.Green

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .clip(shape)
            .background(St.Paper)
            .border(2.5.dp, TileEdge, shape)
            .drawBehind {
                if (edge > 0f) drawRoundRect(edgeColor, alpha = edge, cornerRadius = CornerRadius(22.dp.toPx()), style = Stroke(2.5.dp.toPx()))
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(width = 150.dp, height = 95.dp).graphicsLayer {
                alpha = card
                translationY = 34.dp.toPx() * (1f - card)
                rotationZ = -7f * (1f - card)
            },
        ) {
            Box(Modifier.fillMaxSize().shadow(8.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)).background(St.Sand)) {
                if (thumb != null) Image(thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                if (scanA > 0f) {
                    Box(
                        Modifier.fillMaxWidth().height(4.dp)
                            .graphicsLayer { translationY = 91.dp.toPx() * scanY; alpha = scanA }
                            .shadow(10.dp, RoundedCornerShape(2.dp), ambientColor = St.Gold, spotColor = St.Gold)
                            .background(St.Gold),
                    )
                }
            }
            if (badge > 0f) {
                Box(
                    Modifier.align(Alignment.TopEnd).graphicsLayer {
                        translationX = 8.dp.toPx(); translationY = -8.dp.toPx()
                        alpha = badge; scaleX = 0.3f + 0.7f * badge; scaleY = scaleX
                    }.size(30.dp).clip(CircleShape).background(St.Paper).padding(3.dp).clip(CircleShape).background(St.Green),
                    contentAlignment = Alignment.Center,
                ) { Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(16.dp)) }
            }
        }
        Column(
            Modifier.weight(1f).graphicsLayer { alpha = col; translationX = 10.dp.toPx() * (1f - col) },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = St.Ink, maxLines = 2)
            Box(Modifier.fillMaxWidth().heightIn(min = 46.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
                if (upA > 0f) {
                    Column(Modifier.graphicsLayer { alpha = upA }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.signup_checking), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = St.GoldInk)
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(TileTrack)) {
                            Box(Modifier.fillMaxSize().graphicsLayer { scaleX = fill; transformOrigin = TransformOrigin(0f, 0.5f) }.clip(RoundedCornerShape(4.dp)).background(St.Gold))
                        }
                    }
                }
                if (ok > 0f) {
                    Text(
                        stringResource(if (blurry) R.string.signup_maybe_blurry else R.string.signup_clear),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                        color = if (blurry) St.GoldInk else St.Green,
                        modifier = Modifier.graphicsLayer { alpha = ok; translationY = 6.dp.toPx() * (1f - ok) },
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

private const val SCAN_START = 1280f
private const val SCAN_END = 4864f
private val ScanEase = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
private val FillEase = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val TileDash = androidx.compose.ui.graphics.Color(0xFF8A7B63)
private val TileEdge = androidx.compose.ui.graphics.Color(0xFFE0D3BB)
private val TileTrack = androidx.compose.ui.graphics.Color(0xFFF1E4C8)

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
