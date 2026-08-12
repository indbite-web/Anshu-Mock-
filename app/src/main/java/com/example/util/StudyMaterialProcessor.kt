package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import com.example.data.remote.InlineData
import com.example.data.remote.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

enum class MaterialType {
    IMAGE, PDF
}

data class PdfInfo(
    val fileName: String,
    val pageCount: Int,
    val uri: Uri
)

object StudyMaterialProcessor {

    private const val TAG = "StudyMaterialProcessor"
    private const val MAX_IMAGE_DIMENSION = 1200
    private const val MAX_PDF_PAGES_TO_PROCESS = 10

    suspend fun processMaterialToParts(
        context: Context,
        imageUris: List<Uri>,
        pdfUri: Uri?
    ): List<Part> = withContext(Dispatchers.IO) {
        val parts = mutableListOf<Part>()

        // 1. Process Images
        for (uri in imageUris) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val scaled = scaleBitmap(bitmap, MAX_IMAGE_DIMENSION)
                        val base64 = bitmapToBase64(scaled)
                        parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64)))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process image URI: $uri", e)
            }
        }

        // 2. Process PDF
        if (pdfUri != null) {
            try {
                context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val totalPages = renderer.pageCount
                        val pagesToRender = totalPages.coerceAtMost(MAX_PDF_PAGES_TO_PROCESS)
                        for (i in 0 until pagesToRender) {
                            var page: PdfRenderer.Page? = null
                            try {
                                page = renderer.openPage(i)
                                val scale = 1000f / page.width.toFloat()
                                val renderWidth = (page.width * scale).toInt().coerceAtLeast(100)
                                val renderHeight = (page.height * scale).toInt().coerceAtLeast(100)

                                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val base64 = bitmapToBase64(bitmap)
                                parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64)))
                            } catch (pe: Exception) {
                                Log.e(TAG, "Failed to render PDF page $i", pe)
                            } finally {
                                page?.close()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process PDF URI: $pdfUri", e)
            }
        }

        parts
    }

    suspend fun getPdfInfo(context: Context, pdfUri: Uri): PdfInfo = withContext(Dispatchers.IO) {
        var fileName = "Document.pdf"
        var pageCount = 0

        try {
            context.contentResolver.query(pdfUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        fileName = name
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching PDF display name", e)
        }

        try {
            context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    pageCount = renderer.pageCount
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading PDF page count", e)
        }

        PdfInfo(fileName = fileName, pageCount = pageCount, uri = pdfUri)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxCurrent = maxOf(width, height)
        if (maxCurrent <= maxDimension) return bitmap

        val ratio = maxDimension.toFloat() / maxCurrent.toFloat()
        val targetWidth = (width * ratio).toInt()
        val targetHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
