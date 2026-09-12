package com.vinote.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.vinote.data.local.entity.UserSession
import io.github.jan.supabase.gotrue.user.UserSession as SupabaseSession
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository responsible for securely managing authentication state.
 *
 * Supports both Firebase Authentication and Supabase Authentication.
 */
@Singleton
interface AuthRepository {
    val currentSession: StateFlow<UserSession?>

    /** Gets the current authenticated user ID, or null if not authenticated. */
    fun getUserId(): String?

    /** Gets the canonical user ID for the current session, falling back to a default "user_default" identifier if no user is signed in. */
    fun getCanonicalUserId(): String

    /** Sets the authenticated user ID when session is established. */
    fun setUserId(userId: String?)

    /** Clears the authenticated session (logout). */
    fun clearUserId()

    /** Checks if the user is currently authenticated. */
    fun isAuthenticated(): Boolean

    /** Signs in with Google credentials. Stub: returns a not-implemented result. */
    suspend fun signInWithGoogle(context: android.content.Context): com.vinote.domain.model.AuthResult =
        com.vinote.domain.model.AuthResult.Error("Google sign-in not configured in this build")

    /** Direct profile login (used by onboarding/quick-setup flows). Stub. */
    fun loginWithDirectProfile(email: String, name: String, provider: String = "google") {}

    /** Logout the current user. Stub. */
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
