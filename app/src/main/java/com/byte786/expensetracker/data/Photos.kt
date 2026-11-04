package com.byte786.expensetracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Copies receipt photos into private storage, shrunk so backups stay small. */
object Photos {
    private const val MAX_SIDE = 1600

    suspend fun import(ctx: Context, source: Uri, photosDir: File): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = ctx.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > MAX_SIDE * 2 || bounds.outHeight / sample > MAX_SIDE * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@runCatching null

            val rotation = resolver.openInputStream(source)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            val scale = MAX_SIDE.toFloat() / maxOf(bmp.width, bmp.height)
            if (scale < 1f || rotation != 0f) {
                val m = Matrix().apply {
                    if (scale < 1f) postScale(scale, scale)
                    postRotate(rotation)
                }
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }
            val name = "${UUID.randomUUID()}.jpg"
            File(photosDir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            name
        }.getOrNull()
    }
}
