package com.example.data.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class UpdateDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    companion object {
        private const val TAG = "UpdateDownloader"
    }

    suspend fun downloadApk(
        context: Context,
        updateInfo: UpdateInfo,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val targetDir = context.externalCacheDir ?: context.cacheDir
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val apkFile = File(targetDir, updateInfo.apkFileName)
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val request = Request.Builder()
                .url(updateInfo.apkUrl)
                .header("User-Agent", "Anshu-Mock-Android-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Download HTTP error ${response.code}"))
                }

                val body = response.body ?: return@withContext Result.failure(Exception("Download body was null"))
                val contentLength = if (body.contentLength() > 0) body.contentLength() else updateInfo.apkSize

                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null

                try {
                    inputStream = body.byteStream()
                    outputStream = FileOutputStream(apkFile)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastReportedProgress = -1

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        if (contentLength > 0) {
                            val progress = ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                            if (progress != lastReportedProgress) {
                                lastReportedProgress = progress
                                onProgress(progress)
                            }
                        }
                    }

                    outputStream.flush()
                    Log.d(TAG, "APK download complete: ${apkFile.absolutePath}, size: ${apkFile.length()} bytes")
                    Result.success(apkFile)
                } finally {
                    try { inputStream?.close() } catch (_: Exception) {}
                    try { outputStream?.close() } catch (_: Exception) {}
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to download update APK", e)
            Result.failure(e)
        }
    }
}
