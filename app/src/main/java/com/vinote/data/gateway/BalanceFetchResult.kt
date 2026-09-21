package com.vinote.data.gateway

/**
 * Unified result type for balance fetching across all gateways.
 */
sealed class BalanceFetchResult {
    data class Success(
        val balance: Long,
        val currency: String = "IDR",
        val provider: String,
        val accountId: String,
        val accessToken: String? = null,
        val rawResponse: String? = null
    ) : BalanceFetchResult()

    data class Error(
        val message: String,
        val code: String? = null,
        val provider: String = ""
    ) : BalanceFetchResult()

    data class LinkingRequired(
        val provider: String,
        val linkingUrl: String? = null,
        val message: String = "Account not linked. Please link your $provider account first."
    ) : BalanceFetchResult()

    object Loading : BalanceFetchResult()
}
