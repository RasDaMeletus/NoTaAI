package com.vinote.services.ai

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vinote.data.engine.ExtractedReceiptData
import com.vinote.data.engine.ExtractedVoiceEntity
import com.vinote.data.engine.OfflineNlpEngine
import com.vinote.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class HybridAiProcessor(private val application: Application) {

    private val _engineStatus = MutableStateFlow(AiEngineStatus())
    val engineStatus: StateFlow<AiEngineStatus> = _engineStatus

    private val androidSpeechRecognizer = AndroidSpeechRecognizer(application)
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun setWifiOnlyPreference(enabled: Boolean) {
        _engineStatus.value = _engineStatus.value.copy(isWifiOnlyPreferred = enabled)
    }

    fun setForceOfflineMode(forced: Boolean) {
        _engineStatus.value = _engineStatus.value.copy(isForceOffline = forced, isOnline = !forced)
    }

    /**
     * Process voice transcript text using regex-based extraction.
     */
    suspend fun parseVoiceText(text: String): ExtractedVoiceEntity {
        Log.d("HybridAiProcessor", "Processing text: $text")
        // Use the same rich offline NLP engine as the simulator path so real speech
        // gets merchant/category/wallet/amount parity with sample utterances.
        return OfflineNlpEngine.parseSpokenTransaction(text)
    }

    /**
     * Process voice using Android's built-in SpeechRecognizer.
     */
    suspend fun recognizeSpeech(languageCode: String = "en-US"): ExtractedVoiceEntity {
        val transcript = androidSpeechRecognizer.startListening(languageCode)
            ?: return ExtractedVoiceEntity(
                title = "No transcript available",
                merchant = "",
                amount = 0,
                category = "General",
                type = TransactionType.EXPENSE
            )

        Log.d("HybridAiProcessor", "Transcript: $transcript")
        return OfflineNlpEngine.parseSpokenTransaction(transcript)
    }

    /**
     * Process receipt image using ML Kit Text Recognition.
     * Reference: E:\vinote\lib\shared\services\ocr_service.dart
     */
    suspend fun processReceipt(bitmap: Bitmap): ExtractedReceiptData {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val mlKitResult: Text = suspendCancellableCoroutine { cont ->
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { result -> cont.resume(result) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            }

            parseReceiptAdvanced(mlKitResult)
        } catch (e: Exception) {
            Log.e("HybridAiProcessor", "OCR failed", e)
            ExtractedReceiptData(
                merchant = "OCR Error",
                date = "",
                items = emptyList(),
                subtotal = 0,
                taxOrFee = 0,
                totalAmount = 0,
                category = "General",
                walletOrPayment = "",
                rawLines = emptyList()
            )
        }
    }

    private data class ReceiptLine(
        val text: String,
        val top: Int,
        val bottom: Int,
        val left: Int,
        val right: Int,
        val height: Int
    ) {
        val width: Int get() = right - left
        val centerX: Int get() = (left + right) / 2
    }

    private fun parseReceiptAdvanced(mlKitResult: Text): ExtractedReceiptData {
        val lines = mutableListOf<String>()
        val elements = mutableListOf<ReceiptLine>()

        for (block in mlKitResult.textBlocks) {
            for (line in block.lines) {
                lines.add(line.text)
                val box: Rect? = line.boundingBox
                if (box != null) {
                    elements.add(ReceiptLine(
                        text = line.text.uppercase(),
                        top = box.top,
                        bottom = box.bottom,
                        left = box.left,
                        right = box.right,
                        height = box.height()
                    ))
                } else {
                    elements.add(ReceiptLine(
                        text = line.text.uppercase(),
                        top = 0,
                        bottom = 0,
                        left = 0,
                        right = 0,
                        height = 0
                    ))
                }
            }
        }

        val combinedText = lines.joinToString("\n")

        var amount = 0L
        var storeName = "Unknown Store"
        var confidence = 0.7f

        if (elements.isNotEmpty()) {
            // Store Name Detection (top-most non-numeric lines)
            val sortedByTop = elements.sortedBy { it.top }
            for (element in sortedByTop.take(8)) {
                val text = element.text
                if (text.length > 3 &&
                    !Pattern.compile("\\d{5,}").matcher(text).find() &&
                    !text.contains(':') &&
                    !text.contains("TGL") &&
                    !text.contains("DATE")) {
                    storeName = text
                    break
                }
            }

            // Amount Detection with Anchor + Spatial Logic
            val highPriorityTotalKeywords = listOf("GRAND", "NETT", "DUE", "TOTAL BAYAR", "HARUS DIBAYAR")
            val normalTotalKeywords = listOf("TOTAL", "JUMLAH", "TAGIHAN", "PAYMENT", "RP", "IDR")
            val subtotalKeywords = listOf("SUBTOTAL", "SUB TOTAL", "JUMLAH SEBELUM", "HARGA AWAL")
            val exclusionKeywords = listOf("KEMBALI", "CHANGE", "TUNAI", "CASH", "PROMO", "DISC", "PAJAK", "TAX", "PPN", "SERVICE", "DISKON")

            var totalAnchor: ReceiptLine? = null
            var bestSimilarity = 0.0
            var isHighPriority = false

            val bottomToTop = elements.sortedByDescending { it.top }
            for (element in bottomToTop) {
                for (kw in highPriorityTotalKeywords) {
                    if (calculateSimilarity(element.text, kw) > 0.85) {
                        totalAnchor = element
                        isHighPriority = true
                        break
                    }
                }
                if (isHighPriority) break

                for (kw in normalTotalKeywords) {
                    val sim = calculateSimilarity(element.text, kw)
                    if (sim > 0.8 && sim > bestSimilarity) {
                        val isSubtotal = subtotalKeywords.any { element.text.contains(it) }
                        val isExcluded = exclusionKeywords.any { element.text.contains(it) }
                        if (!isSubtotal && !isExcluded) {
                            bestSimilarity = sim
                            totalAnchor = element
                        }
                    }
                }
                if (totalAnchor != null && bestSimilarity > 0.9) break
            }

            if (totalAnchor != null) {
                val amountRegex = Pattern.compile("(\\d{1,3}([.,]\\d{3})*([.,]\\d{2})?)|(\\d{4,})")

                // Strategy A: Same line, look right of anchor
                val anchor = totalAnchor
                val sameLineElements = elements.filter { e ->
                    e !== anchor &&
                    abs(e.top - anchor.top) < (anchor.height * 0.5) &&
                    e.left > anchor.left
                }.sortedBy { it.left }

                for (e in sameLineElements) {
                    val match = amountRegex.matcher(e.text)
                    if (match.find()) {
                        val foundAmount = cleanAmount(match.group(0)!!)
                        if (foundAmount > 100) {
                            amount = foundAmount
                            confidence = 0.9f
                            break
                        }
                    }
                }

                // Strategy B: Look below the anchor
                if (amount == 0L) {
                    val belowElements = elements.filter { e ->
                        e !== anchor &&
                        e.top > anchor.top &&
                        e.top < anchor.bottom + (anchor.height * 3) &&
                        abs(e.centerX - anchor.centerX) < (anchor.width * 2)
                    }.sortedBy { it.top }

                    for (e in belowElements) {
                        val match = amountRegex.matcher(e.text)
                        if (match.find()) {
                            val foundAmount = cleanAmount(match.group(0)!!)
                            if (foundAmount > 100) {
                                amount = foundAmount
                                confidence = 0.9f
                                break
                            }
                        }
                    }
                }
            }

            // Fallback: Largest amount heuristic
            if (amount == 0L) {
                val amountRegex = Pattern.compile("(\\d{1,3}([.,]\\d{3})*([.,]\\d{2})?)|(\\d{4,})")
                val allAmounts = mutableListOf<Long>()
                for (e in elements) {
                    val matcher = amountRegex.matcher(e.text)
                    while (matcher.find()) {
                        val foundAmount = cleanAmount(matcher.group(0)!!)
                        if (foundAmount > 100) allAmounts.add(foundAmount)
                    }
                }
                if (allAmounts.isNotEmpty()) {
                    allAmounts.sort()
                    amount = allAmounts.last()
                    confidence = 0.6f
                }
            }
        }

        val category = getCategoryFromText(storeName, combinedText)

        val dateRegex = Pattern.compile("(\\d{1,2})[./\\- ](\\d{1,2})[./\\- ](\\d{2,4})")
        val dateMatcher = dateRegex.matcher(combinedText.uppercase())
        var dateStr = ""
        if (dateMatcher.find()) {
            try {
                val d1 = dateMatcher.group(1)!!.toInt()
                val d2 = dateMatcher.group(2)!!.toInt()
                var year = dateMatcher.group(3)!!.toInt()
                if (year < 100) year += 2000
                if (year < 2000) year += 2000
                val (day, month) = if (d1 > 12 && d1 <= 31) d1 to d2 else d1 to d2
                dateStr = String.format("%04d-%02d-%02d", year, month, day)
            } catch (_: Exception) {}
        }

        return ExtractedReceiptData(
            merchant = storeName,
            date = dateStr,
            items = emptyList(),
            subtotal = amount,
            taxOrFee = 0,
            totalAmount = amount,
            category = category,
            walletOrPayment = "",
            rawLines = lines,
            confidence = confidence,
            isOfflineEngine = true
        )
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        val distance = levenshteinDistance(s1, s2)
        return 1.0 - (distance.toDouble() / max(s1.length, s2.length))
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val prev = IntArray(s2.length + 1) { it }
        for (i in s1.indices) {
            val curr = IntArray(s2.length + 1)
            curr[0] = i + 1
            for (j in s2.indices) {
                val cost = if (s1[i] == s2[j]) 0 else 1
                curr[j + 1] = min(min(curr[j] + 1, prev[j + 1] + 1), prev[j] + cost)
            }
            for (k in 0..s2.length) prev[k] = curr[k]
        }
        return prev[s2.length]
    }

    private fun getCategoryFromText(description: String, rawText: String): String {
        val combined = "$description $rawText".uppercase()

        val categoryKeywords = mapOf(
            "Food" to listOf("RESTO", "CAFE", "WARUNG", "COFFEE", "KOPI", "BURG", "PIZZA", "BAKSO", "MIE", "MAKAN", "RESTORAN", "KFC", "MCD", "STARBUCKS", "SOLARIA", "MIXUE", "NASI", "SATE", "TEH", "GOKANA", "HOKBEN", "GEPREK", "BOBA", "JOLLIBEE", "BURGER KING", "SUBWAY", "YOSHINOYA"),
            "Shopping" to listOf("ALFA", "MART", "MALL", "STORE", "GROCERY", "TOKO", "BELANJA", "SAYUR", "SUPER", "HYPERMART", "LOTTEMART", "ALFAMART", "ALFAMIDI", "INDOMARET", "YOGYA", "SUPERINDO", "TOKOPEDIA", "SHOPEE", "LAZADA", "MINISO"),
            "Transport" to listOf("GOJEK", "GRAB", "PERTAMINA", "SHELL", "BENSIN", "PARKIR", "TOLL", "TAXI", "BLUEBIRD", "TRAIN", "KAI", "FLIGHT", "TIKET", "SPBU", "GOCAR", "GRABCAR", "TRAVELOKA", "MRT", "LRT"),
            "Health" to listOf("APOTEK", "KLINIK", "HOSPITAL", "RUMAH SAKIT", "KIMIA FARMA", "OBAT", "GUARDIAN", "WATSONS", "HALODOC", "GYM", "FITNESS"),
            "Bills" to listOf("PLN", "LISTRIK", "PULSA", "INTERNET", "WIFI", "TELKOM", "WATER", "PDAM", "MOBILE", "DATA", "INDIHOME", "OVO", "GOPAY", "DANA", "LINKAJA", "SHOPEEPAY", "TELKOMSEL", "INDOSAT"),
            "Entertainment" to listOf("CINEMA", "XXI", "CGV", "NETFLIX", "SPOTIFY", "GAME", "BIOSKOP", "PLAYSTATION", "KARAOKE", "TIMEZONE", "DISNEY", "YOUTUBE", "STEAM")
        )

        for ((cat, keywords) in categoryKeywords) {
            if (keywords.any { combined.contains(it) }) return cat
        }
        return "Other"
    }

    private fun cleanAmount(amountStr: String): Long {
        var clean = amountStr.replace(Regex("[^\\d.,]"), "")

        if (clean.contains(',') && clean.contains('.')) {
            clean = clean.replace(".", "").replace(",", ".")
        } else if (clean.contains(',')) {
            clean = if (clean.indexOf(',') == clean.length - 3) {
                clean.replace(",", ".")
            } else {
                clean.replace(",", "")
            }
        } else if (clean.contains('.')) {
            clean = if (clean.indexOf('.') == clean.length - 4) {
                clean.replace(".", "")
            } else if (clean.indexOf('.') == clean.length - 3) {
                clean
            } else {
                clean.replace(".", "")
            }
        }

        return clean.toLongOrNull() ?: 0L
    }
}
