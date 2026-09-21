package com.vinote.data.gateway

/**
 * Abstraction for payment gateway balance fetching.
 * Implementations handle the actual API calls (Midtrans, unofficial, etc.)
 */
interface PaymentGatewayService {

    /**
     * Supported provider identifiers.
     */
    enum class Provider(val displayName: String, val type: String) {
        GOPAY("GoPay", "E-Wallet"),
        DANA("DANA", "E-Wallet"),
        OVO("OVO", "E-Wallet"),
        BANK_BCA("BCA", "Bank"),
        BANK_BRI("BRI", "Bank"),
        BANK_BNI("BNI", "Bank"),
        BANK_MANDIRI("Mandiri", "Bank"),
        BANK_PERMATA("Permata", "Bank"),
        BANK_CIMB("CIMB Niaga", "Bank"),
        OTHER("Other", "General")
    }

    /**
     * Fetch the current balance for a linked account.
     * @param provider The e-wallet/bank provider
     * @param accountId The linked account ID (phone number for e-wallets, account number for banks)
     * @param accessToken Optional OAuth/access token for official APIs
     */
    suspend fun fetchBalance(
        provider: Provider,
        accountId: String,
        accessToken: String? = null
    ): BalanceFetchResult

    /**
     * Check if the provider is supported by this gateway.
     */
    fun supportsProvider(provider: Provider): Boolean

    /**
     * Initiate account linking. Returns a URL or deeplink the user should open.
     */
    suspend fun initiateLinking(
        provider: Provider,
        phoneNumber: String
    ): BalanceFetchResult

    /**
     * Get the gateway name for display.
     */
    val gatewayName: String
}