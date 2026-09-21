package com.vinote.domain.statement

import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionSource
import com.vinote.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StatementParseResult(
    val transactions: List<TransactionItem>,
    val totalRows: Int,
    val successCount: Int,
    val failedRows: Int
)

object BankStatementParser {

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    )

    fun parseCsv(csvContent: String, defaultUserId: String = ""): StatementParseResult {
        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return StatementParseResult(emptyList(), 0, 0, 0)
        }

        val firstLine = lines.first().lowercase()
        val hasHeader = firstLine.contains("tanggal") || firstLine.contains("date") ||
                firstLine.contains("nominal") || firstLine.contains("amount") || firstLine.contains("id,")

        val dataLines = if (hasHeader) lines.drop(1) else lines
        val isNoTaFormat = firstLine.contains("id,tanggal,tipe,kategori,judul,merchant,dompet,nominal")

        val resultList = mutableListOf<TransactionItem>()
        var failedCount = 0

        for (line in dataLines) {
            try {
                val item = if (isNoTaFormat) {
                    parseNoTaCsvLine(line, defaultUserId)
                } else {
                    parseGenericCsvLine(line, defaultUserId)
                }

                if (item != null && item.amount > 0) {
                    resultList.add(item)
                } else {
                    failedCount++
                }
            } catch (_: Exception) {
                failedCount++
            }
        }

        return StatementParseResult(
            transactions = resultList,
            totalRows = dataLines.size,
            successCount = resultList.size,
            failedRows = failedCount
        )
    }

    private fun parseNoTaCsvLine(line: String, userId: String): TransactionItem? {
        val tokens = parseCsvLineTokens(line)
        if (tokens.size < 8) return null

        // Format: ID,Tanggal,Tipe,Kategori,Judul,Merchant,Dompet,Nominal (IDR),Terkonfirmasi
        val dateStr = tokens[1]
        val typeStr = tokens[2].lowercase()
        val category = tokens[3]
        val title = tokens[4]
        val merchant = tokens[5]
        val wallet = tokens.getOrNull(6)
        val amountStr = tokens[7].replace(".", "").replace(",", "").trim()

        val amount = amountStr.toLongOrNull() ?: return null
        val type = if (typeStr.contains("masuk") || typeStr.contains("income")) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }

        val timestamp = parseDateToMillis(dateStr)
        val fingerprint = generateFingerprint(timestamp, amount, title)

        return TransactionItem(
            userId = userId,
            title = title.ifBlank { "Transaksi Impor" },
            amount = amount,
            category = category.ifBlank { "General" },
            type = type,
            timestamp = timestamp,
            merchant = merchant,
            walletName = wallet?.ifBlank { "Bank / E-Wallet" },
            source = TransactionSource.BANK_SYNC,
            fingerprint = fingerprint,
            isConfirmed = true
        )
    }

    private fun parseGenericCsvLine(line: String, userId: String): TransactionItem? {
        val delimiter = if (line.contains(";")) ";" else ","
        val tokens = line.split(delimiter).map { it.trim().trim('\"') }
        if (tokens.size < 3) return null

        // Try to identify: Date, Title/Description, Amount, and optional Type
        var dateStr: String? = null
        var title: String? = null
        var amount: Long? = null
        var type = TransactionType.EXPENSE

        for (token in tokens) {
            val lower = token.lowercase()
            if (lower == "pemasukan" || lower == "income" || lower == "kredit" || lower == "cr") {
                type = TransactionType.INCOME
                continue
            }
            if (lower == "pengeluaran" || lower == "expense" || lower == "debit" || lower == "db") {
                type = TransactionType.EXPENSE
                continue
            }

            // Check if amount
            val cleanAmountCandidate = token.replace("rp", "", ignoreCase = true)
                .replace(".", "")
                .replace(",", "")
                .trim()
            val parsedNum = cleanAmountCandidate.toLongOrNull()
            if (parsedNum != null && parsedNum > 0 && amount == null) {
                amount = parsedNum
                continue
            }

            // Check if date
            if (dateStr == null && looksLikeDate(token)) {
                dateStr = token
                continue
            }

            // Else treat as title/description
            if (title == null && token.isNotBlank() && token.length > 1) {
                title = token
            }
        }

        if (amount == null) return null
        val timestamp = dateStr?.let { parseDateToMillis(it) } ?: System.currentTimeMillis()
        val finalTitle = title ?: (if (type == TransactionType.INCOME) "Pemasukan Mutasi" else "Pengeluaran Mutasi")
        val fingerprint = generateFingerprint(timestamp, amount, finalTitle)

        return TransactionItem(
            userId = userId,
            title = finalTitle,
            amount = amount,
            category = guessCategory(finalTitle, type),
            type = type,
            timestamp = timestamp,
            source = TransactionSource.BANK_SYNC,
            fingerprint = fingerprint,
            isConfirmed = true
        )
    }

    private fun looksLikeDate(token: String): Boolean {
        return token.contains("-") || token.contains("/") || token.contains(":")
    }

    private fun parseDateToMillis(dateStr: String): Long {
        for (format in dateFormats) {
            try {
                val date = format.parse(dateStr)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }

    private fun guessCategory(title: String, type: TransactionType): String {
        if (type == TransactionType.INCOME) return "Income"
        val lower = title.lowercase()
        return when {
            lower.contains("makan") || lower.contains("kopi") || lower.contains("resto") || lower.contains("food") -> "Food"
            lower.contains("listrik") || lower.contains("pulsa") || lower.contains("pdam") || lower.contains("wifi") -> "Bills"
            lower.contains("bensin") || lower.contains("grab") || lower.contains("gojek") || lower.contains("parkir") -> "Transport"
            lower.contains("indomaret") || lower.contains("alfamart") || lower.contains("supermarket") -> "Groceries"
            lower.contains("shopee") || lower.contains("tokopedia") || lower.contains("lazada") -> "Shopping"
            else -> "General"
        }
    }

    fun generateFingerprint(timestamp: Long, amount: Long, title: String): String {
        val dateDay = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(timestamp))
        val cleanTitle = title.trim().lowercase().replace("\\s+".toRegex(), "")
        return "${dateDay}_${amount}_${cleanTitle}"
    }

    fun filterDuplicates(
        incoming: List<TransactionItem>,
        existing: List<TransactionItem>
    ): List<TransactionItem> {
        val existingFingerprints = existing.mapNotNull { it.fingerprint }.toHashSet()
        val existingSignatures = existing.map {
            val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(it.timestamp))
            "${day}_${it.amount}_${it.title.trim().lowercase()}"
        }.toHashSet()

        val uniqueList = mutableListOf<TransactionItem>()
        val seenInBatch = HashSet<String>()

        for (item in incoming) {
            val fp = item.fingerprint ?: generateFingerprint(item.timestamp, item.amount, item.title)
            val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(item.timestamp))
            val sig = "${day}_${item.amount}_${item.title.trim().lowercase()}"

            if (fp !in existingFingerprints && sig !in existingSignatures && fp !in seenInBatch) {
                seenInBatch.add(fp)
                uniqueList.add(item)
            }
        }

        return uniqueList
    }

    private fun parseCsvLineTokens(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (c in line) {
            when {
                c == '\"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
