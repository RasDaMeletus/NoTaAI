package com.vinote.domain.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrisPayloadParserTest {

    @Test
    fun `isQrisPayload recognizes valid EMVCo QRIS headers`() {
        val validPayload = "00020101021226580016ID.CO.QRIS.WWW01189360052300000123455204581253033605405250005802ID5913KOPI KENANGAN6007JAKARTA6304ABCD"
        assertTrue(QrisPayloadParser.isQrisPayload(validPayload))
    }

    @Test
    fun `isQrisPayload rejects non QRIS strings`() {
        assertFalse(QrisPayloadParser.isQrisPayload("https://google.com"))
        assertFalse(QrisPayloadParser.isQrisPayload("12345"))
        assertFalse(QrisPayloadParser.isQrisPayload(""))
    }

    @Test
    fun `parseQris extracts merchant name amount and city accurately`() {
        // Tag 01: 0212 (Dynamic QR)
        // Tag 54: 0525000 (Amount 25000)
        // Tag 59: 13KOPI KENANGAN (Merchant)
        // Tag 60: 07JAKARTA (City)
        val payload = "0002010102125405250005913KOPI KENANGAN6007JAKARTA6304ABCD"

        val result = QrisPayloadParser.parseQris(payload)
        assertNotNull(result)
        assertEquals("KOPI KENANGAN", result!!.merchantName)
        assertEquals(25000L, result.amount)
        assertEquals("JAKARTA", result.city)
        assertTrue(result.isDynamic)
    }

    @Test
    fun `parseQris handles static QR without amount`() {
        // Tag 01: 0211 (Static QR)
        // Tag 59: 10WARUNG IBU
        val payload = "0002010102115910WARUNG IBU6007BANDUNG63041234"

        val result = QrisPayloadParser.parseQris(payload)
        assertNotNull(result)
        assertEquals("WARUNG IBU", result!!.merchantName)
        assertNull(result.amount)
        assertEquals("BANDUNG", result.city)
        assertFalse(result.isDynamic)
    }
}
