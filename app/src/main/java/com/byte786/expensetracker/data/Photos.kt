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

/** Copies receipt photos and custom icons into private storage, shrunk so backups stay small. */
object Photos {
    private const val MAX_SIDE = 1600
    private const val ICON_SIDE = 256

    /** Reads an image, upright (EXIF rotation applied) and at most [maxSide] px on its longest side. */
    suspend fun load(ctx: Context, source: Uri, maxSide: Int = 2048): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = ctx.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2
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

            val scale = maxSide.toFloat() / maxOf(bmp.width, bmp.height)
            if (scale < 1f || rotation != 0f) {
                val m = Matrix().apply {
                    if (scale < 1f) postScale(scale, scale)
                    postRotate(rotation)
                }
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }
            bmp
        }.getOrNull()
    }

    /** Saves a (cropped) receipt photo as JPEG; returns its file name. */
    suspend fun savePhoto(bmp: Bitmap, dir: File): String? = save(bmp, dir, MAX_SIDE, png = false)

    /** Saves a square custom icon as PNG; returns its file name. */
    suspend fun saveIcon(bmp: Bitmap, dir: File): String? = save(bmp, dir, ICON_SIDE, png = true)

    private suspend fun save(src: Bitmap, dir: File, maxSide: Int, png: Boolean): String? = withContext(Dispatchers.IO) {
        runCatching {
            val scale = maxSide.toFloat() / maxOf(src.width, src.height)
            val bmp = if (scale < 1f) {
                Bitmap.createScaledBitmap(src, (src.width * scale).toInt().coerceAtLeast(1), (src.height * scale).toInt().coerceAtLeast(1), true)
            } else src
            dir.mkdirs()
            val name = "${UUID.randomUUID()}.${if (png) "png" else "jpg"}"
            File(dir, name).outputStream().use {
                bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, if (png) 100 else 82, it)
            }
            name
        }.getOrNull()
    }
}
