package com.vinote.data.repository

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Singleton

/**
 * Repository responsible for securely managing authentication state.
 *
 * Uses Firebase Authentication directly, storing only the Firebase User ID.
 */
@Singleton
interface AuthRepository {
    /**
     * Gets the current authenticated user ID, or null if not authenticated.
     */
    fun getUserId(): String?

    /**
     * Sets the authenticated user ID when session is established.
     */
    fun setUserId(userId: String?)

    /**
     * Clears the authenticated session (logout).
     */
    fun clearUserId()

    /**
     * Checks if the user is currently authenticated.
     */
    fun isAuthenticated(): Boolean
}
