package com.vinote.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.vinote.domain.model.AuthResult
import com.vinote.data.supabase.SupabaseClientProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import io.github.jan.supabase.gotrue.user.UserSession as SupabaseSession
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.providers.Google as SupabaseGoogleProvider
import io.github.jan.supabase.gotrue.providers.builtin.Email as SupabaseEmailProvider

/**
 * Implementation of AuthRepository using Firebase Authentication and Supabase Authentication.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val supabaseClientProvider: SupabaseClientProvider? = null
) : AuthRepository {

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId

    private val _currentSession = MutableStateFlow<com.vinote.data.local.entity.UserSession?>(null)
    override val currentSession: StateFlow<com.vinote.data.local.entity.UserSession?> = _currentSession

    private val _supabaseSessionFlow = MutableStateFlow<SupabaseSession?>(null)
    override val supabaseSessionFlow: StateFlow<SupabaseSession?> = _supabaseSessionFlow

    init {
        // Firebase auth state listener
        firebaseAuth.addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            _userId.value = uid
            _currentSession.value = uid?.let {
                com.vinote.data.local.entity.UserSession(
                    userId = it,
                    email = auth.currentUser?.email ?: "",
                    name = auth.currentUser?.displayName ?: "",
                    isAuthenticated = true
                )
            }
        }
        val uid = firebaseAuth.currentUser?.uid
        _userId.value = uid
        _currentSession.value = uid?.let {
            com.vinote.data.local.entity.UserSession(
                userId = it,
                email = firebaseAuth.currentUser?.email ?: "",
                name = firebaseAuth.currentUser?.displayName ?: "",
                isAuthenticated = true
            )
        }

        // Supabase auth state listener (guarded for offline-first resilience)
        supabaseClientProvider?.clientOrNull?.let { supabaseClient ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    supabaseClient.auth.sessionStatus.collect { status ->
                        _supabaseSessionFlow.value = when (status) {
                            is SessionStatus.Authenticated -> status.session
                            else -> null
                        }
                    }
                } catch (t: Throwable) {
                    android.util.Log.w("AuthRepo", "Supabase auth session monitoring disabled: ${t.message}")
                }
            }
        }
    }

    override fun getUserId(): String? = firebaseAuth.currentUser?.uid ?: _userId.value

    override fun setUserId(userId: String?) {
        _userId.value = userId
        if (userId == null) {
            _currentSession.value = null
        } else if (_currentSession.value == null) {
            _currentSession.value = com.vinote.data.local.entity.UserSession(
                userId = userId,
                email = "user@vinote.local",
                name = "NoTa User",
                isAuthenticated = true
            )
        }
    }

    override fun clearUserId() {
        firebaseAuth.signOut()
        _userId.value = null
        _currentSession.value = null
    }

    override fun isAuthenticated(): Boolean = firebaseAuth.currentUser != null || (_currentSession.value?.isAuthenticated == true)

    override fun getCanonicalUserId(): String = firebaseAuth.currentUser?.uid ?: _userId.value ?: "user_default"

    override fun loginWithDirectProfile(email: String, name: String, provider: String) {
        val uid = "user_${System.currentTimeMillis()}"
        _userId.value = uid
        _currentSession.value = com.vinote.data.local.entity.UserSession(
            userId = uid,
            email = email,
            name = name,
            provider = provider,
            isAuthenticated = true
        )
    }

    suspend fun getIdToken(): String? {
        return firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
    }

    fun getCurrentFirebaseUser() = firebaseAuth.currentUser

    // ===== Supabase Authentication Implementation =====

    override suspend fun signInWithSupabaseGoogle(): Result<Unit> {
        return try {
            val client = supabaseClientProvider?.clientOrNull
                ?: return Result.failure(IllegalStateException("Supabase is not configured"))
            client.auth.signInWith(SupabaseGoogleProvider)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun signUpWithSupabaseEmail(email: String, password: String): Result<Unit> {
        return try {
            val client = supabaseClientProvider?.clientOrNull
                ?: return Result.failure(IllegalStateException("Supabase is not configured"))
            client.auth.signUpWith(SupabaseEmailProvider) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun signInWithSupabaseEmail(email: String, password: String): Result<Unit> {
        return try {
            val client = supabaseClientProvider?.clientOrNull
                ?: return Result.failure(IllegalStateException("Supabase is not configured"))
            client.auth.signInWith(SupabaseEmailProvider) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun signOutFromSupabase(): Result<Unit> {
        return try {
            val client = supabaseClientProvider?.clientOrNull
                ?: return Result.failure(IllegalStateException("Supabase is not configured"))
            client.auth.signOut()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
