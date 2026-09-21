package com.vinote.data.gateway

/**
 * Catalog of e-wallets the user can choose to integrate.
 *
 * Each entry maps a display name to the gateway provider that knows how to
 * link and fetch balances for it. Banks remain a plain text list in the UI;
 * e-wallets need a real provider because linking goes through an OTP flow.
 */
data class EwalletOption(
    val displayName: String,
    val provider: PaymentGatewayService.Provider,
    val brandColorHex: String,
    val packageName: String?
)

object EwalletCatalog {

    val options: List<EwalletOption> = listOf(
        EwalletOption(
            displayName = "GoPay",
            provider = PaymentGatewayService.Provider.GOPAY,
            brandColorHex = "#00A862",
            packageName = "com.gojek.app"
        ),
        EwalletOption(
            displayName = "OVO",
            provider = PaymentGatewayService.Provider.OVO,
            brandColorHex = "#4C2A86",
            packageName = "com.ovo.ui"
        ),
        EwalletOption(
            displayName = "DANA",
            provider = PaymentGatewayService.Provider.DANA,
            brandColorHex = "#118EEA",
            packageName = "id.dana"
        )
    )

    /**
     * The provider for a wallet by display name, or null if it is not an
     * e-wallet this app knows how to link.
     */
    fun providerFor(displayName: String): PaymentGatewayService.Provider? =
        options.firstOrNull { it.displayName.equals(displayName, ignoreCase = true) }?.provider
}
