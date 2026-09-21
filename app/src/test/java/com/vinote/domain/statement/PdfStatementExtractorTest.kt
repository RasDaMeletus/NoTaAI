package com.vinote.domain.statement

import com.vinote.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfStatementExtractorTest {

    @Test
    fun `parseStatementText extracts debit and credit records`() {
        val sampleStatement = """
            PT BANK CENTRAL ASIA TBK
            REKENING TAHAPAN
            01/09/2026 TRSF E-BANKING DB 150.000,00 TAGIHAN LISTRIK
            02/09/2026 QRIS FORE COFFEE DB 48.000,00 PEMBELIAN QRIS
            05/09/2026 TRSF PEMINDAHAN DANA CR 2.500.000,00 GAJI BULANAN
        """.trimIndent()

        val results = PdfStatementExtractor.parseStatementText(sampleStatement, "user_test")

        assertEquals(3, results.size)

        // Expense 1
        assertEquals(150000L, results[0].amount)
        assertEquals(TransactionType.EXPENSE, results[0].type)

        // Expense 2
        assertEquals(48000L, results[1].amount)
        assertEquals(TransactionType.EXPENSE, results[1].type)

        // Income
        assertEquals(2500000L, results[2].amount)
        assertEquals(TransactionType.INCOME, results[2].type)
    }

    @Test
    fun `parseStatementText ignores small or corrupted text`() {
        val noisyText = """
            HALAMAN 1 DARI 2
            SALDO AWAL : 500
            NOMOR REKENING : 1234567890
            04/09/2026 BELANJA MINIMARKET 75.000,00 DB
        """.trimIndent()

        val results = PdfStatementExtractor.parseStatementText(noisyText, "user_test")
        assertEquals(1, results.size)
        assertEquals(75000L, results[0].amount)
    }
}
