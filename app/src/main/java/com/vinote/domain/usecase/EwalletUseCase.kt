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
    private val walletAccountDao: WalletAccountDao,
    private val unofficialGoPayService: com.vinote.data.gateway.UnofficialGoPayService,
    private val unofficialOvoService: com.vinote.data.gateway.UnofficialOvoService
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

        val userId = walletGatewayRepository.getCanonicalUserId() ?: return EwalletLinkingState.Error("Not signed in")
        return try {
            val wallet = walletAccountDao.getWalletById(walletId, userId)
            val result = when (provider) {
                PaymentGatewayService.Provider.GOPAY -> {
                    // GoPay: verify the OTP against the Edge Function, then
                    // store the returned access token for later balance fetches.
                    val otpToken = referenceId
                    val authResult = unofficialGoPayService.verifyOtp(phoneNumber, otp, otpToken)
                    if (authResult.isSuccess) {
                        val auth = authResult.getOrThrow()
                        if (auth.requiresMFA) {
                            // PIN challenge — needs a separate UI step, not yet exposed.
                            BalanceFetchResult.Error("GoPay memerlukan PIN. Verifikasi PIN belum tersedia di versi ini.")
                        } else {
                            BalanceFetchResult.Success(
                                balance = 0L,
                                provider = provider.displayName,
                                accountId = phoneNumber,
                                accessToken = auth.accessToken
                            )
                        }
                    } else {
                        BalanceFetchResult.Error(authResult.exceptionOrNull()?.message ?: "Verifikasi OTP GoPay gagal")
                    }
                }
                PaymentGatewayService.Provider.OVO -> {
                    // OVO: 3-step auth. verifyOtp here validates the OTP and
                    // returns an intermediate token; the security-code login
                    // step happens on the Edge Function side.
                    val authResult = unofficialOvoService.verifyOtp(phoneNumber, otp, referenceId)
                    if (authResult.isSuccess) {
                        BalanceFetchResult.Success(
                            balance = 0L,
                            provider = provider.displayName,
                            accountId = phoneNumber,
                            accessToken = authResult.getOrThrow()
                        )
                    } else {
                        BalanceFetchResult.Error(authResult.exceptionOrNull()?.message ?: "Verifikasi OTP OVO gagal")
                    }
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
                        gatewayAccessToken = result.accessToken ?: result.provider,
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