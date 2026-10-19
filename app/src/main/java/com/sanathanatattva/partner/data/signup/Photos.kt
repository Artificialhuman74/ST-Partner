package com.sanathanatattva.partner.data.signup

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Shrinks a camera photo before upload: riders are often on slow, metered data, and the
 * server keeps documents at 1600px anyway. Applies the EXIF rotation so the uploaded
 * image is upright, then writes WebP (about half the bytes of JPEG at the same quality)
 * on Android 11+, JPEG on older phones.
 */
suspend fun prepareForUpload(source: File, maxSide: Int = 1600): File = withContext(Dispatchers.IO) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(source.path, bounds)
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val decoded = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return@withContext source

    val degrees = when (ExifInterface(source.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    val longest = max(decoded.width, decoded.height)
    val scale = if (longest > maxSide) maxSide.toFloat() / longest else 1f
    val matrix = Matrix().apply {
        if (scale != 1f) postScale(scale, scale)
        if (degrees != 0f) postRotate(degrees)
    }
    val upright = if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

    val webp = Build.VERSION.SDK_INT >= 30
    val out = File(source.parentFile, source.nameWithoutExtension + if (webp) "-upload.webp" else "-upload.jpg")
    out.outputStream().use {
        if (webp) upright.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, it)
        else upright.compress(Bitmap.CompressFormat.JPEG, 85, it)
    }
    if (upright !== decoded) upright.recycle()
    decoded.recycle()
    out
}
