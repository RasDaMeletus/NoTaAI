package com.vinote.data.repository

import com.vinote.data.local.entity.UserSession
import io.github.jan.supabase.gotrue.user.UserSession as SupabaseSession
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository responsible for securely managing authentication state.
 * Supabase Email Auth as the sole auth provider.
 */
@Singleton
interface AuthRepository {
    val currentSession: StateFlow<UserSession?>

    /** Gets the current authenticated user ID, or null if not authenticated. */
    fun getUserId(): String?

    /** Gets the canonical user ID for the current session. Requires authentication. */
    fun getCanonicalUserId(): String { return getUserId() ?: throw IllegalStateException("User not authenticated") }

    /** Sets the authenticated user ID when session is established. */
    fun setUserId(userId: String?)

    /** Clears the authenticated session (logout). */
    fun clearUserId()

    /** Checks if the user is currently authenticated. */
    fun isAuthenticated(): Boolean

    /** Direct profile login (used by onboarding/quick-setup flows). */
    fun loginWithDirectProfile(email: String, name: String, provider: String = "offline") {}

    /** Logout the current user. */
    fun logout() {}

    // ===== Supabase Authentication =====

    /** Current Supabase auth session, null if not signed in via Supabase. */
    val supabaseSessionFlow: StateFlow<SupabaseSession?>

    /** Sign in with Google using Supabase. */
    suspend fun signInWithSupabaseGoogle(): Result<Unit>

    /** Sign up with email/password using Supabase. */
    suspend fun signUpWithSupabaseEmail(email: String, password: String): Result<Unit>

    /** Sign in with email/password using Supabase. */
    suspend fun signInWithSupabaseEmail(email: String, password: String): Result<Unit>

    /** Sign out from Supabase. */
    suspend fun signOutFromSupabase(): Result<Unit>
}
