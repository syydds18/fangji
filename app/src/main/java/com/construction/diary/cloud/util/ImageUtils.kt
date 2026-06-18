package com.construction.diary.cloud.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {
    fun saveImage(context: Context, uri: Uri): String? {
        try {
            val imagesDir = File(context.getExternalFilesDir(null), "images")
            imagesDir.mkdirs()
            val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val outFile = File(imagesDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                val scaled = scaleBitmap(bitmap, 1200)
                FileOutputStream(outFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                if (scaled != bitmap) scaled.recycle()
                bitmap.recycle()
            }
            return fileName
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getImageFile(context: Context, fileName: String): File {
        return File(context.getExternalFilesDir(null), "images/$fileName")
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }
}
