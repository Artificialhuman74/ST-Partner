package com.sanathanatattva.partner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay

/**
 * A canvas one-shot entrance (CSS `animation: x .6s .3s cubic-bezier(...) both`): 0 → 1 over
 * [durationMs] after [delayMs], starting again whenever [key] changes. Already 1 with
 * "Remove animations" on.
 */
@Composable
fun rememberEntrance(key: Any?, durationMs: Int, delayMs: Int = 0, easing: Easing = OilEase): Animatable<Float, AnimationVector1D> {
    val reduce = LocalReduceMotion.current
    val progress = remember(key) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(key) {
        if (reduce) return@LaunchedEffect
        if (delayMs > 0) delay(delayMs.toLong())
        progress.animateTo(1f, tween(durationMs, easing = easing))
    }
    return progress
}

/** Fades in while sliding [from] (e.g. 10.dp below, -6.dp above) into place. */
fun Modifier.riseIn(progress: Animatable<Float, AnimationVector1D>, from: Dp) = graphicsLayer {
    val p = progress.value
    alpha = p
    translationY = from.toPx() * (1f - p)
}

/** Fades in while growing from [scaleFrom] to full size ("pop", "settle"). */
fun Modifier.popIn(progress: Animatable<Float, AnimationVector1D>, scaleFrom: Float) = graphicsLayer {
    val p = progress.value
    alpha = p
    scaleX = scaleFrom + (1f - scaleFrom) * p
    scaleY = scaleX
}
