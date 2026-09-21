package com.vinote.domain.statement

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionSource
import com.vinote.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

object PdfStatementExtractor {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extractTransactionsFromPdf(
        pdfFile: File,
        userId: String = ""
    ): StatementParseResult = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            return@withContext StatementParseResult(emptyList(), 0, 0, 0)
        }

        val allTransactions = mutableListOf<TransactionItem>()
        var totalRowsScanned = 0
        var failedRows = 0

        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)
            val pageCount = renderer.pageCount.coerceAtMost(5) // scan up to 5 pages

            for (pageIdx in 0 until pageCount) {
                val page = renderer.openPage(pageIdx)
                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val image = InputImage.fromBitmap(bitmap, 0)
                val visionText = recognizer.process(image).await()
                bitmap.recycle()

                val pageText = visionText.text
                val extracted = parseStatementText(pageText, userId)
                allTransactions.addAll(extracted)
                totalRowsScanned += extracted.size
            }

            renderer.close()
            fileDescriptor.close()
        } catch (_: Exception) {
            failedRows++
        }

        StatementParseResult(
            transactions = allTransactions,
            totalRows = totalRowsScanned,
            successCount = allTransactions.size,
            failedRows = failedRows
        )
    }

    fun parseStatementText(text: String, userId: String): List<TransactionItem> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val results = mutableListOf<TransactionItem>()

        val amountRegex = """(?:rp\.?\s*)?([0-9]{1,3}(?:\.[0-9]{3})+(?:,[0-9]{2})?)""".toRegex(RegexOption.IGNORE_CASE)
        val dateRegex = """(\b\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?\b)""".toRegex()

        for (line in lines) {
            val amountMatch = amountRegex.find(line)
            if (amountMatch != null) {
                val rawAmount = amountMatch.groupValues[1].replace(".", "").split(",")[0]
                val amount = rawAmount.toLongOrNull() ?: continue
                if (amount < 1000L) continue // filter noise / small numbers

                val dateMatch = dateRegex.find(line)
                val lower = line.lowercase()
                val isIncome = lower.contains("cr") || lower.contains("kredit") || lower.contains("masuk")
                val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

                val description = line.replace(amountMatch.value, "")
                    .replace(dateMatch?.value ?: "", "")
                    .trim()
                    .takeIf { it.isNotBlank() } ?: (if (type == TransactionType.INCOME) "Mutasi Masuk" else "Mutasi Keluar")

                val timestamp = System.currentTimeMillis()
                val fingerprint = BankStatementParser.generateFingerprint(timestamp, amount, description)

                results.add(
                    TransactionItem(
                        userId = userId,
                        title = description.take(40),
                        amount = amount,
                        category = if (type == TransactionType.INCOME) "Income" else "General",
                        type = type,
                        timestamp = timestamp,
                        source = TransactionSource.BANK_SYNC,
                        fingerprint = fingerprint,
                        isConfirmed = true
                    )
                )
            }
        }

        return results
    }
}
