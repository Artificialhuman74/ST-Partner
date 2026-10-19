package com.sanathanatattva.partner.data.signup

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * How sharp a photo is: variance of the Laplacian over a small grayscale copy.
 * Blurry photos have few hard edges, so the variance is low. Used only to nudge
 * ("Might be blurry"), never to block, because a plain card can score low too.
 */
suspend fun sharpness(file: File): Double = withContext(Dispatchers.Default) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 480) sample *= 2
    val bmp = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return@withContext 0.0
    val w = bmp.width
    val h = bmp.height
    val px = IntArray(w * h)
    bmp.getPixels(px, 0, w, 0, 0, w, h)
    bmp.recycle()
    val gray = FloatArray(px.size) { i ->
        val c = px[i]
        0.299f * (c shr 16 and 0xFF) + 0.587f * (c shr 8 and 0xFF) + 0.114f * (c and 0xFF)
    }
    var sum = 0.0
    var sumSq = 0.0
    var n = 0
    for (y in 1 until h - 1) {
        for (x in 1 until w - 1) {
            val i = y * w + x
            val lap = (gray[i - 1] + gray[i + 1] + gray[i - w] + gray[i + w] - 4 * gray[i]).toDouble()
            sum += lap
            sumSq += lap * lap
            n++
        }
    }
    if (n == 0) 0.0 else sumSq / n - (sum / n) * (sum / n)
}

/** Below this the photo gets a gentle "Might be blurry" note. */
const val BLURRY_BELOW = 40.0
