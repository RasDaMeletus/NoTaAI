package com.vinote.data.wallet.adapters

import com.vinote.data.model.TransactionType
import com.vinote.domain.wallet.WalletNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShopeePayAdapterTest {

    private lateinit var adapter: ShopeePayAdapter

    @Before
    fun setup() {
        adapter = ShopeePayAdapter()
    }

    @Test
    fun `isFinancialNotification identifies ShopeePay payment messages`() {
        val notif = WalletNotification(
            packageName = "com.shopee.id",
            title = "ShopeePay",
            text = "Pembayaran berhasil Rp 45.000 ke ShopeeFood"
        )
        assertTrue(adapter.isFinancialNotification(notif))
    }

    @Test
    fun `parseNotification correctly parses expense for ShopeeFood`() {
        val notif = WalletNotification(
            packageName = "com.shopee.id",
            title = "ShopeePay",
            text = "Pembayaran berhasil Rp 45.000 ke ShopeeFood"
        )
        val result = adapter.parseNotification(notif)
        assertNotNull(result)
        assertEquals(45000L, result?.amount)
        assertEquals(TransactionType.EXPENSE, result?.type)
        assertEquals("Food", result?.category)
        assertEquals("ShopeeFood", result?.merchant)
    }

    @Test
    fun `parseNotification correctly parses income for cashback`() {
        val notif = WalletNotification(
            packageName = "com.shopee.id",
            title = "ShopeePay",
            text = "Kamu menerima Cashback Rp 5.000 dari promo Shopee"
        )
        val result = adapter.parseNotification(notif)
        assertNotNull(result)
        assertEquals(5000L, result?.amount)
        assertEquals(TransactionType.INCOME, result?.type)
        assertEquals("Income", result?.category)
    }

    @Test
    fun `parseNotification correctly parses bill payment`() {
        val notif = WalletNotification(
            packageName = "com.shopee.id",
            title = "ShopeePay",
            text = "Pembayaran tagihan PLN berhasil Rp 120.000"
        )
        val result = adapter.parseNotification(notif)
        assertNotNull(result)
        assertEquals(120000L, result?.amount)
        assertEquals(TransactionType.EXPENSE, result?.type)
        assertEquals("Bills", result?.category)
    }
}
