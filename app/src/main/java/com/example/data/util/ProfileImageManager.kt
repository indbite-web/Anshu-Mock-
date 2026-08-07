package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ProfileImageManager {

    private const val PROFILE_FILENAME = "profile_avatar.jpg"
    private const val TEMP_CAMERA_FILENAME = "temp_camera_avatar.jpg"
    private const val MAX_DIMENSION = 1024

    /**
     * Copies and optimizes an image from [sourceUri] into internal app storage.
     * Downscales large images off the main thread to optimize performance and memory.
     */
    suspend fun saveUriToInternalStorage(context: Context, sourceUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Decode bounds
                var inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
                    ?: return@withContext null

                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                val originalWidth = options.outWidth
                val originalHeight = options.outHeight

                if (originalWidth <= 0 || originalHeight <= 0) {
                    return@withContext null
                }

                // 2. Calculate sample size
                var sampleSize = 1
                while (originalWidth / sampleSize > MAX_DIMENSION || originalHeight / sampleSize > MAX_DIMENSION) {
                    sampleSize *= 2
                }

                // 3. Decode scaled bitmap
                inputStream = context.contentResolver.openInputStream(sourceUri)
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                inputStream?.close()

                if (bitmap == null) return@withContext null

                // 4. Save to internal storage file
                val destFile = File(context.filesDir, PROFILE_FILENAME)
                if (destFile.exists()) {
                    destFile.delete()
                }

                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }

                bitmap.recycle()

                Uri.fromFile(destFile).toString()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Creates a temporary file in cacheDir and returns a FileProvider URI for taking a camera photo.
     */
    fun createCameraTempUri(context: Context): Uri? {
        return try {
            val tempFile = File(context.cacheDir, TEMP_CAMERA_FILENAME)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            tempFile.createNewFile()
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes the local profile avatar file from app-private storage.
     */
    suspend fun deleteProfileImage(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, PROFILE_FILENAME)
                if (file.exists()) {
                    file.delete()
                } else {
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
