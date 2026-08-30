package com.example.data.auth

data class UserSession(
    val userId: String,
    val email: String,
    val name: String,
    val avatarUrl: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAt: Long = System.currentTimeMillis() + (30L * 24 * 3600 * 1000), // 30 days default
    val provider: String = "google",
    val isAuthenticated: Boolean = true,
    val isOfflineMode: Boolean = false,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)
