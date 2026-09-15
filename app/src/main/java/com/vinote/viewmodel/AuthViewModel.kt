package com.vinote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinote.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Authentication state machine for ViNote.
 * Manages Supabase Email Auth flow using AuthRepository.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

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

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val result = authRepository.signInWithSupabaseEmail(email, password)) {
                is Result -> {
                    val userId = authRepository.getUserId()
                    if (userId != null) {
                        _authState.value = AuthState.Authenticated(userId)
                    } else {
                        _authState.value = AuthState.Error("Supabase sign-in succeeded but userId is null")
                    }
                }
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val result = authRepository.signUpWithSupabaseEmail(email, password)) {
                is Result -> {
                    val userId = authRepository.getUserId()
                    if (userId != null) {
                        _authState.value = AuthState.Authenticated(userId)
                    } else {
                        _authState.value = AuthState.Error("Supabase sign-up succeeded but userId is null")
                    }
                }
            }
        }
    }

    /**
     * Signs out the user from Supabase.
     */
    fun signOut() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepository.signOutFromSupabase()
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
