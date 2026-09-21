package com.vinote.data.wallet.adapters

import com.vinote.data.model.TransactionType
import com.vinote.domain.wallet.WalletNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BRImoAdapterTest {

    private lateinit var adapter: BRImoAdapter

    @Before
    fun setup() {
        adapter = BRImoAdapter()
    }

    @Test
    fun `isFinancialNotification identifies BRImo financial events`() {
        val notif = WalletNotification(
            packageName = "id.co.bri.brimo",
            title = "BRImo",
            text = "Transaksi berhasil debet Rp 150.000 untuk pembayaran QRIS"
        )
        assertTrue(adapter.isFinancialNotification(notif))
    }

    @Test
    fun `parseNotification correctly parses QRIS debet expense`() {
        val notif = WalletNotification(
            packageName = "id.co.bri.brimo",
            title = "BRImo",
            text = "Transaksi berhasil debet Rp 85.000 untuk pembayaran QRIS di Kopi Kenangan"
        )
        val result = adapter.parseNotification(notif)
        assertNotNull(result)
        assertEquals(85000L, result?.amount)
        assertEquals(TransactionType.EXPENSE, result?.type)
        assertEquals("QRIS BRImo", result?.title)
    }

    @Test
    fun `parseNotification correctly parses kredit transfer in`() {
        val notif = WalletNotification(
            packageName = "id.co.bri.brimo",
            title = "BRImo",
            text = "Transaksi kredit masuk Rp 1.500.000 dari PT ABC"
        )
        val result = adapter.parseNotification(notif)
        assertNotNull(result)
        assertEquals(1500000L, result?.amount)
        assertEquals(TransactionType.INCOME, result?.type)
        assertEquals("Income", result?.category)
        assertEquals("BRImo Inflow", result?.title)
    }
}
