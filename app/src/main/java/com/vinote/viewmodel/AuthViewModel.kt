package com.vinote.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinote.data.repository.AuthRepository
import com.vinote.domain.auth.GoogleSignInManager
import com.vinote.domain.model.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Authentication state machine for ViNote.
 *
 * Manages Google Sign-In flow using GoogleSignInManager and AuthRepository.
 * The canonical user identity lives in Firebase Auth.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignInManager: GoogleSignInManager,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun getSignInIntent() = googleSignInManager.getSignInIntent()

    /**
     * Checks if a user is already signed in.
     */
    fun restoreSession() {
        _authState.value = AuthState.Loading
        val userId = authRepository.getUserId()
        if (userId != null) {
            _authState.value = AuthState.Authenticated(userId)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * Allows immediate direct/guest/offline login without blocking on Play Services.
     */
    fun loginDirectly(name: String = "NoTa User", email: String = "user@vinote.local") {
        authRepository.loginWithDirectProfile(email, name, "offline")
        val userId = authRepository.getUserId() ?: "user_default"
        _authState.value = AuthState.Authenticated(userId)
    }

    /**
     * Handles the result from the Google Sign-In Intent.
     */
    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val result = googleSignInManager.handleSignInResult(data)) {
                is AuthResult.Success -> {
                    val userId = authRepository.getUserId()
                    if (userId != null) {
                        _authState.value = AuthState.Authenticated(userId)
                    } else {
                        _authState.value = AuthState.Error("Firebase Auth succeeded but userId is null")
                    }
                }
                is AuthResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    /**
     * Signs out the user from Google and Firebase.
     */
    fun signOut() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                googleSignInManager.signOut()
            } catch (_: Exception) {
                // Proceed with local cleanup regardless of network result
            } finally {
                authRepository.clearUserId()
                _authState.value = AuthState.Unauthenticated
            }
        }
    }
}

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val userId: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
