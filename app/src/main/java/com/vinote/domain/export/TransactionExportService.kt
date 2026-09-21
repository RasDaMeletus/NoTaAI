package com.vinote.domain.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TransactionExportService {

    fun generateCsvContent(transactions: List<TransactionItem>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()

        // CSV Header
        sb.append("ID,Tanggal,Tipe,Kategori,Judul,Merchant,Dompet,Nominal (IDR),Terkonfirmasi\n")

        for (tx in transactions) {
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val typeStr = if (tx.type == TransactionType.INCOME) "Pemasukan" else "Pengeluaran"
            val titleClean = escapeCsv(tx.title)
            val catClean = escapeCsv(tx.category)
            val merchantClean = escapeCsv(tx.merchant)
            val walletClean = escapeCsv(tx.walletName ?: "Tunai")
            val confirmedStr = if (tx.isConfirmed) "Ya" else "Pending"

            sb.append("${tx.id},$dateStr,$typeStr,$catClean,$titleClean,$merchantClean,$walletClean,${tx.amount},$confirmedStr\n")
        }

        return sb.toString()
    }

    fun exportTransactionsToCsvFile(context: Context, transactions: List<TransactionItem>): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(exportDir, "NoTa_Transaksi_$timeStamp.csv")

        val content = generateCsvContent(transactions)
        FileWriter(file).use { writer ->
            writer.write(content)
        }

        return file
    }

    fun createShareIntent(context: Context, file: File): Intent {
        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            android.net.Uri.fromFile(file)
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Transaksi Keuangan NoTa (CSV)")
            putExtra(Intent.EXTRA_TEXT, "Berikut terlampir ekspor transaksi keuangan dari NoTa.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun exportTransactionsToPdfFile(
        context: Context,
        transactions: List<TransactionItem>,
        totalIncome: Long,
        totalExpense: Long
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(exportDir, "NoTa_Laporan_$timeStamp.pdf")

        val document = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }

        // Header
        paint.color = android.graphics.Color.rgb(0, 108, 76)
        paint.textSize = 18f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("NoTa — Laporan Transaksi Keuangan", 40f, 50f, paint)

        val printDate = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        paint.color = android.graphics.Color.DKGRAY
        paint.textSize = 10f
        paint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText("Dibuat pada: $printDate | Status: Offline Report", 40f, 68f, paint)

        // Divider
        paint.color = android.graphics.Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawLine(40f, 80f, 555f, 80f, paint)

        // Summary Box
        paint.color = android.graphics.Color.rgb(240, 246, 242)
        canvas.drawRoundRect(40f, 95f, 555f, 155f, 8f, 8f, paint)

        val netBalance = totalIncome - totalExpense
        paint.color = android.graphics.Color.rgb(0, 108, 76)
        paint.textSize = 11f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("RINGKASAN KEUANGAN:", 55f, 115f, paint)

        paint.textSize = 10f
        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = android.graphics.Color.rgb(20, 20, 20)
        canvas.drawText("Total Pemasukan: Rp ${java.text.NumberFormat.getNumberInstance(Locale("id", "ID")).format(totalIncome)}", 55f, 135f, paint)
        canvas.drawText("Total Pengeluaran: Rp ${java.text.NumberFormat.getNumberInstance(Locale("id", "ID")).format(totalExpense)}", 230f, 135f, paint)
        canvas.drawText("Saldo Bersih: Rp ${java.text.NumberFormat.getNumberInstance(Locale("id", "ID")).format(netBalance)}", 410f, 135f, paint)

        // Table Header
        var y = 185f
        paint.color = android.graphics.Color.rgb(230, 230, 230)
        canvas.drawRect(40f, y - 15f, 555f, y + 8f, paint)

        paint.color = android.graphics.Color.BLACK
        paint.textSize = 9.5f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("Tanggal", 45f, y, paint)
        canvas.drawText("Kategori", 130f, y, paint)
        canvas.drawText("Judul / Merchant", 220f, y, paint)
        canvas.drawText("Dompet", 370f, y, paint)
        canvas.drawText("Nominal", 470f, y, paint)

        // Table Rows
        paint.typeface = android.graphics.Typeface.DEFAULT
        val itemDateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        val numberFormat = java.text.NumberFormat.getNumberInstance(Locale("id", "ID"))

        for (tx in transactions.take(32)) {
            y += 18f
            if (y > 780f) break

            val dateStr = itemDateFormat.format(Date(tx.timestamp))
            val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
            val amtStr = "$sign Rp ${numberFormat.format(tx.amount)}"

            paint.color = android.graphics.Color.DKGRAY
            canvas.drawText(dateStr, 45f, y, paint)
            canvas.drawText(tx.category.take(12), 130f, y, paint)
            canvas.drawText(tx.title.take(22), 220f, y, paint)
            canvas.drawText((tx.walletName ?: "Tunai").take(12), 370f, y, paint)

            paint.color = if (tx.type == TransactionType.INCOME) android.graphics.Color.rgb(0, 130, 80) else android.graphics.Color.rgb(180, 20, 20)
            canvas.drawText(amtStr, 470f, y, paint)
        }

        // Footer
        paint.color = android.graphics.Color.GRAY
        paint.textSize = 8.5f
        canvas.drawLine(40f, 795f, 555f, 795f, paint)
        canvas.drawText("Dokumen ini dibuat otomatis secara offline oleh NoTa (Personal Finance Companion).", 40f, 812f, paint)

        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    fun createPdfShareIntent(context: Context, file: File): Intent {
        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            android.net.Uri.fromFile(file)
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Transaksi Keuangan NoTa (PDF)")
            putExtra(Intent.EXTRA_TEXT, "Berikut terlampir laporan transaksi keuangan dari NoTa dalam format PDF.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun escapeCsv(value: String): String {
        var clean = value.replace("\r", " ").replace("\n", " ")
        if (clean.contains(",") || clean.contains("\"")) {
            clean = clean.replace("\"", "\"\"")
            return "\"$clean\""
        }
        return clean
    }
}

