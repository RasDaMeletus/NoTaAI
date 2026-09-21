package com.vinote.services.media

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class ReceiptImageProcessor {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processImageForText(bitmap: Bitmap): List<String> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val result = textRecognizer.process(image).await()
            result.textBlocks.flatMap { it.lines.map { line -> line.text } }
        } catch (e: Exception) {
            Log.e("ReceiptImageProcessor", "ML Kit Text Recognition failed", e)
            emptyList()
        }
    }
}
