package com.vinote.domain.transaction

sealed interface TransactionValidationResult {
    data object Valid : TransactionValidationResult
    data class Invalid(val reason: String) : TransactionValidationResult
}
