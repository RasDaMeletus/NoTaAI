package com.vinote.domain.statement

import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankStatementParserTest {

    @Test
    fun `parseCsv parses NoTa exported CSV format accurately`() {
        val csv = """
            ID,Tanggal,Tipe,Kategori,Judul,Merchant,Dompet,Nominal (IDR),Terkonfirmasi
            1,2026-09-06 10:00:00,Pengeluaran,Food,Kopi Kenangan,Kopi Kenangan,GoPay,25000,Ya
            2,2026-09-06 14:30:00,Pemasukan,Income,Gaji Bulanan,PT Teknologi,BCA,8000000,Ya
        """.trimIndent()

        val result = BankStatementParser.parseCsv(csv, "user_test")

        assertEquals(2, result.totalRows)
        assertEquals(2, result.successCount)
        assertEquals(0, result.failedRows)

        val first = result.transactions[0]
        assertEquals("Kopi Kenangan", first.title)
        assertEquals(25000L, first.amount)
        assertEquals(TransactionType.EXPENSE, first.type)
        assertEquals("Food", first.category)
        assertEquals("GoPay", first.walletName)

        val second = result.transactions[1]
        assertEquals("Gaji Bulanan", second.title)
        assertEquals(8000000L, second.amount)
        assertEquals(TransactionType.INCOME, second.type)
        assertEquals("Income", second.category)
        assertEquals("BCA", second.walletName)
    }

    @Test
    fun `parseCsv parses generic 4-column CSV statement`() {
        val csv = """
            Tanggal,Keterangan,Nominal,Tipe
            2026-09-06,Makan Siang Soto Betawi,35000,Pengeluaran
            2026-09-05,Transfer Masuk Bonus,250000,Pemasukan
        """.trimIndent()

        val result = BankStatementParser.parseCsv(csv, "user_test")

        assertEquals(2, result.successCount)
        assertEquals("Makan Siang Soto Betawi", result.transactions[0].title)
        assertEquals(35000L, result.transactions[0].amount)
        assertEquals(TransactionType.EXPENSE, result.transactions[0].type)
        assertEquals("Food", result.transactions[0].category)

        assertEquals("Transfer Masuk Bonus", result.transactions[1].title)
        assertEquals(250000L, result.transactions[1].amount)
        assertEquals(TransactionType.INCOME, result.transactions[1].type)
    }

    @Test
    fun `parseCsv skips corrupted lines cleanly`() {
        val csv = """
            Tanggal,Keterangan,Nominal,Tipe
            2026-09-06,Belanja Minimarket,45000,Pengeluaran
            INVALID_LINE_WITHOUT_NUMBERS
            ,
            2026-09-05,Pulsa Telkomsel,50000,Pengeluaran
        """.trimIndent()

        val result = BankStatementParser.parseCsv(csv, "user_test")
        assertEquals(2, result.successCount)
        assertEquals(2, result.failedRows)
    }

    @Test
    fun `filterDuplicates filters identical transactions from batch`() {
        val incoming = listOf(
            TransactionItem(id = 1L, title = "Kopi Kenangan", amount = 25000L, category = "Food", timestamp = 1700000000000L),
            TransactionItem(id = 2L, title = "Beli Buku", amount = 95000L, category = "General", timestamp = 1700050000000L),
            // Duplicate inside incoming batch
            TransactionItem(id = 3L, title = "Beli Buku", amount = 95000L, category = "General", timestamp = 1700050000000L)
        )

        val existing = listOf(
            TransactionItem(id = 10L, title = "Kopi Kenangan", amount = 25000L, category = "Food", timestamp = 1700000000000L)
        )

        val unique = BankStatementParser.filterDuplicates(incoming, existing)

        assertEquals(1, unique.size)
        assertEquals("Beli Buku", unique[0].title)
    }
}
