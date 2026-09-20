package com.vinote.data.repository

import com.vinote.data.local.entity.UserSession
import com.vinote.data.supabase.SupabaseClientProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import io.github.jan.supabase.gotrue.user.UserSession as SupabaseSession
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.providers.builtin.Email as SupabaseEmailProvider

/**
 * Implementation of AuthRepository using Supabase Authentication as the sole auth provider.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClientProvider: SupabaseClientProvider,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context
) : AuthRepository {

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId

    private val _currentSession = MutableStateFlow<com.vinote.data.local.entity.UserSession?>(null)
    override val currentSession: StateFlow<com.vinote.data.local.entity.UserSession?> = _currentSession

    private val _supabaseSessionFlow = MutableStateFlow<SupabaseSession?>(null)
    override val supabaseSessionFlow: StateFlow<SupabaseSession?> = _supabaseSessionFlow

    // Offline / guest mode. Supabase Auth is the production path, but the app
    // must be usable with no account and no network: this provisions a stable
    // local id that Room rows are scoped to, and restores it on relaunch.
    private val guestPrefs = appContext.getSharedPreferences(
        "vinote_guest", android.content.Context.MODE_PRIVATE
    )

    init {
        // Supabase auth state listener (guarded for offline-first resilience)
        supabaseClientProvider?.clientOrNull?.let { supabaseClient ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    supabaseClient.auth.sessionStatus.collect { status ->
                        val session = when (status) {
                            is SessionStatus.Authenticated -> status.session
                            else -> null
                        }
                        _supabaseSessionFlow.value = session
                        val user = session?.user
                        if (user != null) {
                            _userId.value = user.id
                            _currentSession.value = com.vinote.data.local.entity.UserSession(
                                userId = user.id,
                                email = user.email ?: "",
                                name = user.userMetadata?.get("full_name")?.toString() ?: "NoTa User",
                                isAuthenticated = true
                            )
                        } else {
                            // Keep the guest id unless a real Supabase id has
                            // taken over, so offline data is never orphaned.
                            if (_userId.value == null ||
                                _userId.value!!.startsWith("guest_")) {
                                // A NotAuthenticated after we had a guest id
                                // means Supabase has no session; keep guest.
                            } else {
                                _userId.value = null
                                _currentSession.value = null
                            }
                        }
                    }
                } catch (t: Throwable) {
                    android.util.Log.w("AuthRepo", "Supabase auth session monitoring disabled: ${t.message}")
                }
            }
        }

        // Restore a previous guest/offline session so a relaunch does not throw
        // the user back to onboarding. Supabase sessions are handled above.
        try {
            guestPrefs.getString("guest_id", null)?.let { gid ->
                _userId.value = gid
                _currentSession.value = UserSession(
                    userId = gid,
                    email = "user@vinote.local",
                    name = "NoTa User",
                    provider = "offline",
                    isAuthenticated = false,
                    isOfflineMode = true
                )
            }
        } catch (t: Throwable) {
            android.util.Log.w("AuthRepo", "Guest session restore failed: ${t.message}")
        }
    }

    override fun getUserId(): String? = _userId.value

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
        _userId.value = null
        _currentSession.value = null
    }

    override fun isAuthenticated(): Boolean = _currentSession.value?.isAuthenticated == true

    override fun getCanonicalUserId(): String = _userId.value
        ?: throw IllegalStateException("No authenticated user. User must sign in via Supabase Auth.")

    // Offline / guest mode.
    // Supabase Auth is the production path, but the app must be usable with no
    // account and no network. This provisions a stable local id that Room rows
    // are scoped to; it can later be migrated when the user signs in.
    override fun loginWithDirectProfile(email: String, name: String, provider: String) {
        // Stable per-device id: data created as a guest stays reachable on
        // relaunch, instead of orphaning rows behind a random id each time.
        val guestId = guestPrefs.getString("guest_id", null)
            ?: "guest_${java.util.UUID.randomUUID()}".also {
                guestPrefs.edit().putString("guest_id", it).apply()
            }

        _userId.value = guestId
        _currentSession.value = UserSession(
            userId = guestId,
            email = email,
            name = name,
            provider = provider,
            isAuthenticated = false,
            isOfflineMode = true
        )
    }

    // ===== Supabase Authentication Implementation =====

    override suspend fun signInWithSupabaseGoogle(): Result<Unit> {
        return try {
            val client = supabaseClientProvider?.clientOrNull
                ?: return Result.failure(IllegalStateException("Supabase is not configured"))
            client.auth.signInWith(io.github.jan.supabase.gotrue.providers.Google)
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
