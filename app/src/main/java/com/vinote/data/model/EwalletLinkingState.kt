package com.vinote.data.model

sealed class EwalletLinkingState {
    object Idle : EwalletLinkingState()
    data class SendingOtp(val phoneNumber: String, val provider: String) : EwalletLinkingState()
    data class AwaitingOtp(val phoneNumber: String, val provider: String, val referenceId: String) : EwalletLinkingState()
    data class VerifyingOtp(val phoneNumber: String, val provider: String) : EwalletLinkingState()
    data class Success(val provider: String, val phoneNumber: String) : EwalletLinkingState()
    data class Error(val message: String) : EwalletLinkingState()
}
