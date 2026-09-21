package com.vinote.domain.ocr

data class QrisParseResult(
    val merchantName: String,
    val amount: Long?,
    val city: String?,
    val isDynamic: Boolean,
    val rawPayload: String
)

object QrisPayloadParser {

    /**
     * Checks if the scanned string matches the Indonesian QRIS (EMVCo) standard.
     */
    fun isQrisPayload(content: String): Boolean {
        val trimmed = content.trim()
        return (trimmed.startsWith("000201") || trimmed.contains("ID.CO.QRIS")) && trimmed.length >= 20
    }

    /**
     * Parses TLV (Tag-Length-Value) blocks according to EMVCo / ASPI QRIS standard.
     */
    fun parseQris(rawContent: String): QrisParseResult? {
        val content = rawContent.trim()
        if (!isQrisPayload(content)) return null

        var merchantName: String? = null
        var amount: Long? = null
        var city: String? = null
        var isDynamic = false

        var index = 0
        while (index + 4 <= content.length) {
            val tag = content.substring(index, index + 2)
            val lengthStr = content.substring(index + 2, index + 4)
            val length = lengthStr.toIntOrNull() ?: break

            index += 4
            if (index + length > content.length) break

            val value = content.substring(index, index + length)
            index += length

            when (tag) {
                "01" -> {
                    // "11" = Static QR, "12" = Dynamic QR (with embedded amount)
                    isDynamic = (value == "12")
                }
                "54" -> {
                    // Transaction Amount (e.g. "25000" or "45000.00")
                    amount = value.toDoubleOrNull()?.toLong()
                }
                "59" -> {
                    // Merchant Name
                    merchantName = value.trim()
                }
                "60" -> {
                    // Merchant City
                    city = value.trim()
                }
            }
        }

        val finalMerchant = merchantName ?: "Merchant QRIS"
        return QrisParseResult(
            merchantName = finalMerchant,
            amount = amount,
            city = city,
            isDynamic = isDynamic,
            rawPayload = content
        )
    }
}
