package com.example.services.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.engine.ExtractedReceiptData
import com.example.data.engine.ExtractedVoiceEntity
import com.example.data.engine.OfflineNlpEngine
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Production-ready Hugging Face Inference Client for Hybrid AI Processing.
 * Supports Serverless & Dedicated Inference endpoints for:
 * 1. Document OCR & Receipt Parsing (Vision / Document OCR models)
 * 2. Voice ASR & Financial Entity Extraction (Whisper & IndoBERT/LLM reasoning)
 */
class HuggingFaceApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "HuggingFaceAI"
        const val DEFAULT_OCR_MODEL = "microsoft/trocr-base-printed"
        const val DEFAULT_ASR_MODEL = "openai/whisper-small"
        const val DEFAULT_NLP_MODEL = "meta-llama/Llama-3.2-1B-Instruct"
        private const val HF_ROUTER_URL = "https://api-inference.huggingface.co/models"
    }

    /**
     * Performs Online Receipt OCR using Hugging Face Vision/OCR models.
     * Takes a camera bitmap, sends it to Hugging Face, and parses structured receipt details.
     */
    suspend fun performOnlineReceiptOcr(
        bitmap: Bitmap,
        apiKey: String? = null,
        modelName: String = DEFAULT_OCR_MODEL
    ): Result<ExtractedReceiptData> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initiating online Hugging Face OCR request with model: $modelName")

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val imageBytes = stream.toByteArray()

            val requestBuilder = Request.Builder()
                .url("$HF_ROUTER_URL/$modelName")
                .post(imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
                .addHeader("x-wait-for-model", "true")

            val token = apiKey?.trim()?.ifEmpty { null }
            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.w(TAG, "Hugging Face OCR API returned status ${response.code}: $responseBody")
                return@withContext Result.failure(Exception("HF API error HTTP ${response.code}: $responseBody"))
            }

            Log.d(TAG, "Hugging Face OCR Raw Response: $responseBody")

            // Parse response: Can be [{"generated_text": "..."}] or raw text or JSON object
            val recognizedLines = mutableListOf<String>()
            try {
                if (responseBody.trim().startsWith("[")) {
                    val jsonArray = JSONArray(responseBody)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val text = obj.optString("generated_text", "")
                        if (text.isNotBlank()) {
                            recognizedLines.addAll(text.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() })
                        }
                    }
                } else if (responseBody.trim().startsWith("{")) {
                    val jsonObj = JSONObject(responseBody)
                    val text = jsonObj.optString("generated_text", jsonObj.optString("text", ""))
                    if (text.isNotBlank()) {
                        recognizedLines.addAll(text.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() })
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "JSON parsing fallback, raw lines used", e)
                recognizedLines.addAll(responseBody.lines().map { it.trim() }.filter { it.isNotEmpty() })
            }

            if (recognizedLines.isEmpty()) {
                return@withContext Result.failure(Exception("Empty text recognized by Hugging Face OCR"))
            }

            // Enrich recognized lines with on-device Indonesian NLP parser
            val structuredReceipt = OfflineNlpEngine.parseReceiptTextLines(recognizedLines)
            val enrichedReceipt = structuredReceipt.copy(
                isOfflineEngine = false,
                confidence = 0.99f
            )

            Result.success(enrichedReceipt)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing Hugging Face Online OCR", e)
            Result.failure(e)
        }
    }

    /**
     * Performs Online Natural Language Entity Extraction & Categorization via Hugging Face AI.
     * Takes transcribed text and extracts structured expense intent.
     */
    suspend fun performOnlineNlpExtraction(
        transcript: String,
        apiKey: String? = null,
        modelName: String = DEFAULT_NLP_MODEL
    ): Result<ExtractedVoiceEntity> = withContext(Dispatchers.IO) {
        try {
            val token = apiKey?.trim()?.ifEmpty { null }
            val systemPrompt = "You are an Indonesian financial expense extractor. Parse this transcript into JSON with keys: title, amount (integer number in IDR), category (Food, Transport, Groceries, Entertainment, Bills, Shopping, Health, Income), merchant (optional), wallet (GoPay, OVO, DANA, BCA, Mandiri, Cash, etc), type (EXPENSE or INCOME). Return ONLY valid JSON."
            val userContent = "Transcript: \"$transcript\""

            val jsonPayload = JSONObject().apply {
                put("inputs", "$systemPrompt\n$userContent\nJSON Response:")
                put("parameters", JSONObject().apply {
                    put("max_new_tokens", 150)
                    put("temperature", 0.1)
                    put("return_full_text", false)
                })
            }

            val requestBuilder = Request.Builder()
                .url("$HF_ROUTER_URL/$modelName")
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .addHeader("x-wait-for-model", "true")

            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.w(TAG, "Hugging Face NLP API error ${response.code}: $responseBody")
                // Graceful fallback to on-device NLP
                val offlineFallback = OfflineNlpEngine.parseSpokenTransaction(transcript)
                return@withContext Result.success(offlineFallback)
            }

            // Extract JSON from response text
            val jsonText = if (responseBody.trim().startsWith("[")) {
                val array = JSONArray(responseBody)
                if (array.length() > 0) array.getJSONObject(0).optString("generated_text", "") else ""
            } else if (responseBody.trim().startsWith("{")) {
                val obj = JSONObject(responseBody)
                obj.optString("generated_text", responseBody)
            } else {
                responseBody
            }

            val extractedJson = extractFirstJsonObject(jsonText)
            if (extractedJson != null) {
                val title = extractedJson.optString("title", transcript)
                val amount = extractedJson.optLong("amount", 0L)
                val category = extractedJson.optString("category", "Food")
                val merchant = extractedJson.optString("merchant", title)
                val wallet = extractedJson.optString("wallet", "Cash")
                val typeStr = extractedJson.optString("type", "EXPENSE")
                val type = if (typeStr.equals("INCOME", ignoreCase = true) || category.equals("Income", ignoreCase = true)) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }

                if (amount > 0L) {
                    return@withContext Result.success(
                        ExtractedVoiceEntity(
                            title = title,
                            merchant = merchant,
                            amount = amount,
                            category = category,
                            type = type,
                            walletName = wallet,
                            confidence = 0.98f,
                            isOfflineEngine = false
                        )
                    )
                }
            }

            // If online LLM output could not be parsed into valid amount, fallback to on-device regex NLP
            val localParsed = OfflineNlpEngine.parseSpokenTransaction(transcript)
            Result.success(localParsed)
        } catch (e: Exception) {
            Log.e(TAG, "Online NLP failed, falling back to on-device engine", e)
            val fallback = OfflineNlpEngine.parseSpokenTransaction(transcript)
            Result.success(fallback)
        }
    }

    private fun extractFirstJsonObject(text: String): JSONObject? {
        val startIdx = text.indexOf('{')
        val endIdx = text.lastIndexOf('}')
        if (startIdx in 0 until endIdx) {
            val candidate = text.substring(startIdx, endIdx + 1)
            return try {
                JSONObject(candidate)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }
}
