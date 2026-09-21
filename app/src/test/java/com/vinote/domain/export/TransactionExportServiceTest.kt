package com.vinote.domain.export

import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionExportServiceTest {

    @Test
    fun `generateCsvContent includes header and properly formatted transactions`() {
        val tx1 = TransactionItem(
            id = 101L,
            title = "Kopi Kenangan",
            amount = 25000L,
            category = "Food",
            type = TransactionType.EXPENSE,
            merchant = "Kopi Kenangan",
            walletName = "GoPay",
            timestamp = 1700000000000L,
            isConfirmed = true
        )

        val tx2 = TransactionItem(
            id = 102L,
            title = "Gaji, Bulanan",
            amount = 5000000L,
            category = "Income",
            type = TransactionType.INCOME,
            merchant = "Kantor",
            walletName = "BCA",
            timestamp = 1700000000000L,
            isConfirmed = true
        )

        val csv = TransactionExportService.generateCsvContent(listOf(tx1, tx2))

        // Check header
        assertTrue(csv.contains("ID,Tanggal,Tipe,Kategori,Judul,Merchant,Dompet,Nominal (IDR),Terkonfirmasi"))

        // Check expense row
        assertTrue(csv.contains("101"))
        assertTrue(csv.contains("Pengeluaran"))
        assertTrue(csv.contains("Food"))
        assertTrue(csv.contains("Kopi Kenangan"))
        assertTrue(csv.contains("25000"))

        // Check income row and comma escaping
        assertTrue(csv.contains("102"))
        assertTrue(csv.contains("Pemasukan"))
        assertTrue(csv.contains("\"Gaji, Bulanan\""))
        assertTrue(csv.contains("5000000"))
    }
}
