package com.vinote.services.wallet

import com.vinote.data.model.TransactionType
import com.vinote.domain.wallet.ParsedWalletTransaction
import com.vinote.domain.wallet.WalletNotification
import com.vinote.services.wallet.WalletDeduplicationService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for WalletDeduplicationService to ensure duplicate notification detection.
 */
class WalletDeduplicationServiceTest {

    private lateinit var deduplicationService: WalletDeduplicationService

    @Before
    fun setup() {
        deduplicationService = WalletDeduplicationService()
    }

    private fun createParsedTx(
        packageName: String,
        title: String,
        text: String,
        timestamp: Long = System.currentTimeMillis(),
        id: String = "1001_12345"
    ): ParsedWalletTransaction {
        return ParsedWalletTransaction(
            title = title,
            amount = 25000L,
            category = "Food",
            type = TransactionType.EXPENSE,
            merchant = "GrabFood",
            walletName = "GoPay",
            rawNotification = WalletNotification(
                packageName = packageName,
                title = title,
                text = text,
                bigText = null,
                timestamp = timestamp,
                id = id
            ),
            confidence = 0.95f
        )
    }

    @Test
    fun `should detect duplicate notifications with same package and timestamp`() {
        val baseTimestamp = System.currentTimeMillis()
        val tx1 = createParsedTx(
            packageName = "com.gopay.dompet",
            title = "Payment Successful",
            text = "You spent Rp 25,000 at GrabFood",
            timestamp = baseTimestamp,
            id = "1001_12345"
        )

        val tx2 = createParsedTx(
            packageName = "com.gopay.dompet",
            title = "Payment Successful",
            text = "You spent Rp 25,000 at GrabFood",
            timestamp = baseTimestamp, // Same timestamp
            id = "1001_12345" // Same ID
        )

        // First notification should not be duplicate
        assertFalse(deduplicationService.isDuplicate(tx1))
        // Second notification with same ID should be duplicate
        assertTrue(deduplicationService.isDuplicate(tx2))
    }

    @Test
    fun `should allow different notifications`() {
        val baseTimestamp = System.currentTimeMillis()
        val tx1 = createParsedTx(
            packageName = "com.gopay.dompet",
            title = "Payment Successful",
            text = "You spent Rp 25,000 at GrabFood",
            timestamp = baseTimestamp,
            id = "1001_12345"
        )

        val tx2 = createParsedTx(
            packageName = "com.dana.id",
            title = "Top Up Successful",
            text = "Your balance is Rp 500,000",
            timestamp = baseTimestamp,
            id = "2002_67890"
        )

        assertFalse(deduplicationService.isDuplicate(tx1))
        assertFalse(deduplicationService.isDuplicate(tx2))
    }
}