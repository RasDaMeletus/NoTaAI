package com.vinote.data.repository

import com.vinote.data.gateway.BalanceFetchResult
import com.vinote.data.gateway.MidtransGatewayService
import com.vinote.data.gateway.PaymentGatewayService
import com.vinote.data.gateway.UnofficialDanaService
import com.vinote.data.gateway.UnofficialGoPayService
import com.vinote.data.gateway.UnofficialOvoService
import com.vinote.data.local.entities.WalletAccountEntity
import com.vinote.data.local.WalletAccountDao
import com.vinote.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.gotrue.auth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that coordinates balance fetching across different payment gateways.
 * It delegates to the appropriate service based on the wallet's gatewayType.
 */
@Singleton
class WalletGatewayRepository @Inject constructor(
    private val midtransGatewayService: MidtransGatewayService,
    private val unofficialGoPayService: UnofficialGoPayService,
    private val unofficialOvoService: UnofficialOvoService,
    private val unofficialDanaService: UnofficialDanaService,
    private val walletAccountDao: WalletAccountDao,
    private val supabaseClientProvider: SupabaseClientProvider
) {

    /**
     * Fetch live balance for a specific wallet.
     * Uses the wallet's stored gatewayType to determine which service to call.
     */
    suspend fun fetchWalletBalance(wallet: WalletAccountEntity): BalanceFetchResult {
        return when (wallet.gatewayType) {
            PaymentGatewayService.Provider.GOPAY.displayName -> {
                unofficialGoPayService.fetchBalance(
                    PaymentGatewayService.Provider.GOPAY,
                    wallet.linkedAccountId ?: "",
                    wallet.gatewayAccessToken
                )
            }
            PaymentGatewayService.Provider.OVO.displayName -> {
                unofficialOvoService.fetchBalance(
                    PaymentGatewayService.Provider.OVO,
                    wallet.linkedAccountId ?: "",
                    wallet.gatewayAccessToken
                )
            }
            PaymentGatewayService.Provider.DANA.displayName -> {
                unofficialDanaService.fetchBalance(
                    PaymentGatewayService.Provider.DANA,
                    wallet.linkedAccountId ?: "",
                    wallet.gatewayAccessToken
                )
            }
            PaymentGatewayService.Provider.BANK_BCA.displayName,
            PaymentGatewayService.Provider.BANK_BRI.displayName,
            PaymentGatewayService.Provider.BANK_BNI.displayName,
            PaymentGatewayService.Provider.BANK_MANDIRI.displayName,
            PaymentGatewayService.Provider.BANK_PERMATA.displayName,
            PaymentGatewayService.Provider.BANK_CIMB.displayName -> {
                val provider = when (wallet.gatewayType) {
                    PaymentGatewayService.Provider.BANK_BCA.displayName -> PaymentGatewayService.Provider.BANK_BCA
                    PaymentGatewayService.Provider.BANK_BRI.displayName -> PaymentGatewayService.Provider.BANK_BRI
                    PaymentGatewayService.Provider.BANK_BNI.displayName -> PaymentGatewayService.Provider.BANK_BNI
                    PaymentGatewayService.Provider.BANK_MANDIRI.displayName -> PaymentGatewayService.Provider.BANK_MANDIRI
                    PaymentGatewayService.Provider.BANK_PERMATA.displayName -> PaymentGatewayService.Provider.BANK_PERMATA
                    PaymentGatewayService.Provider.BANK_CIMB.displayName -> PaymentGatewayService.Provider.BANK_CIMB
                    else -> PaymentGatewayService.Provider.BANK_BCA
                }
                midtransGatewayService.fetchBalance(
                    provider,
                    wallet.linkedAccountId ?: "",
                    wallet.gatewayAccessToken
                )
            }
            else -> BalanceFetchResult.Error("Unsupported gateway type: ${wallet.gatewayType}")
        }
    }

    /**
     * Fetch live balances for all wallets that have a gatewayType set.
     */
    suspend fun fetchAllWalletBalances(): List<kotlin.Pair<WalletAccountEntity, BalanceFetchResult>> {
        val wallets = walletAccountDao.getAllWalletsWithGateway()
        return wallets.map { wallet ->
            wallet to fetchWalletBalance(wallet)
        }
    }

    /**
     * Update a wallet's balance status in the local database.
     */
    suspend fun updateWalletBalance(walletId: String, newBalance: Double) {
        walletAccountDao.updateBalance(walletId, newBalance)
    }

    /**
     * Link a new wallet account via the specified gateway.
     */
    suspend fun linkWalletAccount(
        provider: PaymentGatewayService.Provider,
        phoneNumber: String
    ): BalanceFetchResult {
        return when (provider) {
            PaymentGatewayService.Provider.GOPAY -> unofficialGoPayService.initiateLinking(provider, phoneNumber)
            PaymentGatewayService.Provider.OVO -> unofficialOvoService.initiateLinking(provider, phoneNumber)
            PaymentGatewayService.Provider.DANA -> unofficialDanaService.initiateLinking(provider, phoneNumber)
            PaymentGatewayService.Provider.BANK_BCA,
            PaymentGatewayService.Provider.BANK_BRI,
            PaymentGatewayService.Provider.BANK_BNI,
            PaymentGatewayService.Provider.BANK_MANDIRI,
            PaymentGatewayService.Provider.BANK_PERMATA,
            PaymentGatewayService.Provider.BANK_CIMB -> {
                val midtransProvider = when (provider) {
                    PaymentGatewayService.Provider.BANK_BCA -> PaymentGatewayService.Provider.BANK_BCA
                    PaymentGatewayService.Provider.BANK_BRI -> PaymentGatewayService.Provider.BANK_BRI
                    PaymentGatewayService.Provider.BANK_BNI -> PaymentGatewayService.Provider.BANK_BNI
                    PaymentGatewayService.Provider.BANK_MANDIRI -> PaymentGatewayService.Provider.BANK_MANDIRI
                    PaymentGatewayService.Provider.BANK_PERMATA -> PaymentGatewayService.Provider.BANK_PERMATA
                    PaymentGatewayService.Provider.BANK_CIMB -> PaymentGatewayService.Provider.BANK_CIMB
                    else -> PaymentGatewayService.Provider.BANK_BCA // Fallback, should not happen
                }
                midtransGatewayService.initiateLinking(midtransProvider, phoneNumber)
            }
            else -> BalanceFetchResult.Error("Linking not supported for provider: ${provider.displayName}")
        }
    }

    /**
     * Get all wallet accounts that are linked to a payment gateway (have gatewayType set).
     */
    fun getLinkedWalletsFlow() = walletAccountDao.getAllWalletsWithGatewayFlow()

    /**
     * Canonical Supabase auth user id for the signed-in session, or null when
     * no session is available. Wallet lookups are scoped to this id.
     */
    fun getCanonicalUserId(): String? {
        return try {
            supabaseClientProvider.clientOrNull
                ?.auth?.currentUserOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }
}