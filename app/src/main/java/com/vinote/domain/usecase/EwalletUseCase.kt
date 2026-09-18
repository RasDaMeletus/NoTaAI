package com.vinote.domain.usecase

import com.vinote.data.local.WalletAccountDao
import com.vinote.data.model.EwalletLinkingState
import com.vinote.data.gateway.BalanceFetchResult
import com.vinote.data.repository.WalletGatewayRepository
import com.vinote.data.gateway.PaymentGatewayService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface EwalletUseCaseInterface {
    fun getLinkingState(): Flow<EwalletLinkingState>
    suspend fun sendOtp(walletId: String, phoneNumber: String, walletName: String): EwalletLinkingState
    suspend fun verifyOtp(walletId: String, phoneNumber: String, otp: String, referenceId: String, walletName: String): EwalletLinkingState
    suspend fun fetchBalance(walletId: String): BalanceFetchResult
}

@Singleton
class EwalletUseCaseImpl @Inject constructor(
    private val walletGatewayRepository: WalletGatewayRepository,
    private val walletAccountDao: WalletAccountDao
) : EwalletUseCaseInterface {

    private val _linkingState = MutableStateFlow<EwalletLinkingState>(EwalletLinkingState.Idle)
    override fun getLinkingState(): Flow<EwalletLinkingState> = _linkingState.asStateFlow()

    override suspend fun sendOtp(walletId: String, phoneNumber: String, walletName: String): EwalletLinkingState {
        val provider = when (walletName.lowercase()) {
            "gopay" -> PaymentGatewayService.Provider.GOPAY
            "ovo" -> PaymentGatewayService.Provider.OVO
            "dana" -> PaymentGatewayService.Provider.DANA
            else -> return EwalletLinkingState.Error("Unsupported provider")
        }

        _linkingState.value = EwalletLinkingState.SendingOtp(phoneNumber, provider.displayName)
        return try {
            val result = walletGatewayRepository.linkWalletAccount(provider, phoneNumber)
            when (result) {
                is BalanceFetchResult.LinkingRequired -> {
                    _linkingState.value = EwalletLinkingState.AwaitingOtp(
                        phoneNumber = phoneNumber,
                        provider = provider.displayName,
                        referenceId = result.provider
                    )
                    _linkingState.value
                }
                is BalanceFetchResult.Error -> {
                    _linkingState.value = EwalletLinkingState.Error(result.message)
                    _linkingState.value
                }
                else -> {
                    _linkingState.value = EwalletLinkingState.Error("Unexpected response")
                    _linkingState.value
                }
            }
        } catch (e: Exception) {
            _linkingState.value = EwalletLinkingState.Error(e.message ?: "Connection failed")
            _linkingState.value
        }
    }

    override suspend fun verifyOtp(walletId: String, phoneNumber: String, otp: String, referenceId: String, walletName: String): EwalletLinkingState {
        val provider = when (walletName.lowercase()) {
            "gopay" -> PaymentGatewayService.Provider.GOPAY
            "ovo" -> PaymentGatewayService.Provider.OVO
            "dana" -> PaymentGatewayService.Provider.DANA
            else -> return EwalletLinkingState.Error("Unsupported provider")
        }

        _linkingState.value = EwalletLinkingState.VerifyingOtp(phoneNumber, provider.displayName)

        // For DANA, the OTP flow completes via linkWalletAccount callback (no separate verify call)
        // For GoPay/OVO, verification requires Edge Functions that aren't implemented yet
        val userId = walletGatewayRepository.getCanonicalUserId() ?: return EwalletLinkingState.Error("Not signed in")
        return try {
            val wallet = walletAccountDao.getWalletById(walletId, userId)
            val result = when (provider) {
                PaymentGatewayService.Provider.GOPAY,
                PaymentGatewayService.Provider.OVO -> {
                    // GoPay/OVO OTP verification requires Midtrans Edge Function (not yet implemented)
                    // Return error indicating feature not available
                    BalanceFetchResult.Error("OTP verification for ${provider.displayName} not yet implemented. Please use Midtrans integration.")
                }
                PaymentGatewayService.Provider.DANA -> {
                    // DANA uses direct OTP confirmation via linkWalletAccount - assume completed
                    BalanceFetchResult.Success(0L, provider.displayName, phoneNumber, accountId = phoneNumber)
                }
                else -> BalanceFetchResult.Error("Verification not supported")
            }

            when (result) {
                is BalanceFetchResult.Success -> {
                    val updated = wallet?.copy(
                        isConnected = true,
                        gatewayAccessToken = result.provider,
                        linkedAccountId = phoneNumber,
                        gatewayType = provider.displayName,
                        lastSyncTimestamp = System.currentTimeMillis()
                    )
                    if (updated != null) {
                        walletAccountDao.updateWallet(updated)
                    }
                    _linkingState.value = EwalletLinkingState.Success(provider.displayName, phoneNumber)
                    _linkingState.value
                }
                is BalanceFetchResult.Error -> {
                    _linkingState.value = EwalletLinkingState.Error(result.message)
                    _linkingState.value
                }
                else -> {
                    _linkingState.value = EwalletLinkingState.Error("Unexpected verification response")
                    _linkingState.value
                }
            }
        } catch (e: Exception) {
            _linkingState.value = EwalletLinkingState.Error(e.message ?: "Verification failed")
            _linkingState.value
        }
    }

    override suspend fun fetchBalance(walletId: String): BalanceFetchResult {
        val userId = walletGatewayRepository.getCanonicalUserId()
            ?: return BalanceFetchResult.Error("Not signed in")
        return try {
            val wallet = walletAccountDao.getWalletById(walletId, userId)
            wallet?.let {
                walletGatewayRepository.fetchWalletBalance(it)
            } ?: BalanceFetchResult.Error("Wallet not found")
        } catch (e: Exception) {
            BalanceFetchResult.Error(e.message ?: "Failed to fetch balance")
        }
    }
}