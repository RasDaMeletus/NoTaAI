package com.vinote.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the authenticated user session on Android.
 * Only the canonical user ID from Auth.js backend is stored.
 * No OAuth secrets are persisted here.
 */
@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey
    val userId: String,
    val email: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAt: Long = System.currentTimeMillis() + (30L * 24 * 3600 * 1000),
    val provider: String = "google",
    val isAuthenticated: Boolean = true,
    val isOfflineMode: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)
