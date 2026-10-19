package com.sanathanatattva.partner.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.clickable
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.ui.theme.St
import java.io.File
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

enum class CameraGuide { Oval, Card, Corners }

/** Lets a screen put its own shutter outside the camera box (the door photo does). */
class CameraTrigger {
    internal var fire: (() -> Unit)? = null
    var busy by mutableStateOf(false)
        internal set
    fun shoot() { fire?.invoke() }
}

/**
 * Live camera with a shape to line up against (oval for selfies, ID-card frame for
 * documents) and a big shutter. Asks for the camera permission itself.
 */
@Composable
fun CameraCapture(
    front: Boolean,
    guide: CameraGuide,
    hint: String,
    newFile: () -> File,
    onCaptured: (File) -> Unit,
    modifier: Modifier = Modifier,
    trigger: CameraTrigger? = null,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    Box(modifier.clip(RoundedCornerShape(28.dp)).background(St.Ink)) {
        if (!granted) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(StIcons.Camera, contentDescription = null, tint = St.Cream, modifier = Modifier.size(40.dp))
                Text(stringResource(R.string.signup_camera_needed), color = St.Cream, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                PrimaryButton(text = stringResource(R.string.signup_camera_allow), arrow = false, onClick = { ask.launch(Manifest.permission.CAMERA) })
            }
            return@Box
        }

        val lifecycle = LocalLifecycleOwner.current
        val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
        var busy by remember { mutableStateOf(false) }
        val previewView = remember {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }
        DisposableEffect(front, lifecycle) {
            val future = ProcessCameraProvider.getInstance(context)
            var provider: ProcessCameraProvider? = null
            future.addListener({
                provider = future.get().also { p ->
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                    runCatching {
                        p.unbindAll()
                        p.bindToLifecycle(lifecycle, selector, preview, capture)
                    }
                }
            }, ContextCompat.getMainExecutor(context))
            onDispose { provider?.unbindAll() }
        }

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        GuideOverlay(guide, Modifier.fillMaxSize())
        Text(
            hint,
            color = St.Cream,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                .background(St.Ink.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
        val takePhoto = stringResource(R.string.signup_take_photo)
        val shoot: () -> Unit = {
            if (!busy) {
                busy = true
                trigger?.busy = true
                val file = newFile()
                val options = ImageCapture.OutputFileOptions.Builder(file)
                    .setMetadata(ImageCapture.Metadata().apply { isReversedHorizontal = front })
                    .build()
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                capture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        busy = false
                        trigger?.busy = false
                        onCaptured(file)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        busy = false
                        trigger?.busy = false
                        file.delete()
                    }
                })
            }
        }
        DisposableEffect(trigger) {
            trigger?.fire = shoot
            onDispose { trigger?.fire = null }
        }
        if (trigger == null) Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .size(76.dp)
                .border(4.dp, St.Gold, CircleShape)
                .padding(7.dp)
                .clip(CircleShape)
                .background(St.Cream)
                .semantics { role = Role.Button; contentDescription = takePhoto }
                .clickable(enabled = !busy, onClick = shoot),
            contentAlignment = Alignment.Center,
        ) {
            if (busy) CircularProgressIndicator(color = St.Green, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
            else Icon(StIcons.Camera, contentDescription = null, tint = St.Green, modifier = Modifier.size(30.dp))
        }
    }
}

/**
 * Dims everything outside the guide shape and outlines it. The selfie oval is the canvas's
 * ("SignupSelfie"): cream dashes marching round it ("sf-dash", 2.4 s) and a gold arc
 * sweeping once round every 3.2 s ("sf-sweep").
 */
@Composable
private fun GuideOverlay(guide: CameraGuide, modifier: Modifier) {
    val reduce = LocalReduceMotion.current
    val march by rememberLoop(2400)
    val sweep by rememberLoop(3200)
    Canvas(modifier) {
        val k = size.width / 350f
        val shape = Path()
        when (guide) {
            CameraGuide.Oval -> {
                // rx 88, ry 116, centred 14 units above the middle of a 350 × 340 frame.
                val w = 176f * k
                val h = 232f * k
                shape.addOval(Rect(Offset(center.x - w / 2, center.y - 14f * k - h / 2), Size(w, h)))
            }
            CameraGuide.Corners -> Unit
            CameraGuide.Card -> {
                val w = size.width * 0.86f
                val h = w / 1.586f // ID-1 card ratio (licence, PAN)
                shape.addRoundRect(RoundRect(Rect(Offset(center.x - w / 2, center.y - h / 2), Size(w, h)), CornerRadius(16.dp.toPx())))
            }
        }
        if (guide == CameraGuide.Corners) {
            // The canvas's four cream corner brackets; nothing dimmed, the whole doorway counts.
            val inset = 40.dp.toPx() * size.width / 358.dp.toPx()
            val arm = 30.dp.toPx() * size.width / 358.dp.toPx()
            val l = inset; val t = inset * 1.5f; val r = size.width - inset; val b = size.height - inset * 1.5f
            val stroke = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            listOf(
                Path().apply { moveTo(l, t + arm); lineTo(l, t); lineTo(l + arm, t) },
                Path().apply { moveTo(r - arm, t); lineTo(r, t); lineTo(r, t + arm) },
                Path().apply { moveTo(r, b - arm); lineTo(r, b); lineTo(r - arm, b) },
                Path().apply { moveTo(l + arm, b); lineTo(l, b); lineTo(l, b - arm) },
            ).forEach { drawPath(it, St.Cream, style = stroke) }
            return@Canvas
        }
        clipPath(shape, ClipOp.Difference) { drawRect(Color(0x800F2417)) }
        if (guide == CameraGuide.Oval) {
            val dashPhase = if (reduce) 0f else 38f * k * march
            drawPath(shape, St.Cream, alpha = 0.85f, style = Stroke(3f * k, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f * k, 9f * k), dashPhase)))
            if (!reduce) {
                val perimeter = PathMeasure().apply { setPath(shape, false) }.length
                val phase = perimeter * SweepEase.transform(sweep)
                drawPath(shape, St.Gold, style = Stroke(5f * k, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(120f * k, perimeter - 120f * k), phase)))
            }
        } else {
            drawPath(shape, St.Cream.copy(alpha = 0.9f), style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
        }
    }
}

private val SweepEase = androidx.compose.animation.core.CubicBezierEasing(0.45f, 0f, 0.25f, 1f)

/**
 * The canvas shutter ("SignupSelfie", "DoorPhoto"): a green ring round a green disc, the ring
 * breathing outwards every 2 s ("sf-ring" / "dp-ring"), with "Take photo" under it.
 */
@Composable
fun ShutterButton(trigger: CameraTrigger, label: String, size: Dp = 84.dp, inner: Dp = 64.dp) {
    val reduce = LocalReduceMotion.current
    val t by rememberLoop(2000)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(size).drawBehind {
                if (!reduce) {
                    val p = OilEase.transform((t / 0.7f).coerceAtMost(1f))
                    drawCircle(St.Green.copy(alpha = 0.45f * (1f - p)), radius = this.size.minDimension / 2 + 14.dp.toPx() * p)
                }
            }.clip(CircleShape).background(St.Cream).border(4.dp, St.Green, CircleShape)
                .clickable(enabled = !trigger.busy, role = Role.Button) { trigger.shoot() }
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(inner).clip(CircleShape).background(St.Green), contentAlignment = Alignment.Center) {
                if (trigger.busy) CircularProgressIndicator(color = St.Cream, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
                else Icon(StIcons.Camera, contentDescription = null, tint = St.Cream, modifier = Modifier.size(inner * 0.45f))
            }
        }
        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Ink)
    }
}
