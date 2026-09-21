package com.vinote.data.wallet.adapters

import com.vinote.data.model.TransactionType
import com.vinote.domain.wallet.WalletNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OVOAdapterTest {

    private val adapter = OVOAdapter()

    @Test
    fun `supports both ovo id and com ovoapp package names`() {
        assertTrue(adapter.supportedPackageNames.contains("ovo.id"))
        assertTrue(adapter.supportedPackageNames.contains("com.ovoapp"))
    }

    @Test
    fun `identifies financial notifications`() {
        val notif = WalletNotification(
            packageName = "com.ovoapp",
            title = "Transaksi Berhasil",
            text = "Pembayaran OVO Rp 45.000 ke Fore Coffee berhasil."
        )
        assertTrue(adapter.isFinancialNotification(notif))
    }

    @Test
    fun `parses expense notification and extracts merchant`() {
        val notif = WalletNotification(
            packageName = "ovo.id",
            title = "Pembayaran Berhasil",
            text = "Kamu telah membayar Rp 32.500 ke Kopi Kenangan menggunakan OVO Cash."
        )

        val parsed = adapter.parseNotification(notif)
        assertNotNull(parsed)
        assertEquals(32500L, parsed!!.amount)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("Food", parsed.category)
        assertEquals("Kopi Kenangan", parsed.merchant)
        assertEquals("OVO", parsed.walletName)
    }

    @Test
    fun `parses top up income notification`() {
        val notif = WalletNotification(
            packageName = "com.ovoapp",
            title = "Top Up Berhasil",
            text = "Top Up OVO Cash sebesar Rp 150.000 berhasil."
        )

        val parsed = adapter.parseNotification(notif)
        assertNotNull(parsed)
        assertEquals(150000L, parsed!!.amount)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals("Income", parsed.category)
    }
}
