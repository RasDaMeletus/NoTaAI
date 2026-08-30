package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.auth.UserSession

@Entity(tableName = "user_sessions")
data class UserSessionEntity(
    @PrimaryKey
    val userId: String,
    val email: String,
    val name: String,
    val avatarUrl: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAt: Long = System.currentTimeMillis() + (30L * 24 * 3600 * 1000),
    val provider: String = "google",
    val isAuthenticated: Boolean = true,
    val isOfflineMode: Boolean = false,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
) {
    fun toUserSession(): UserSession = UserSession(
        userId = userId,
        email = email,
        name = name,
        avatarUrl = avatarUrl,
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAt = expiresAt,
        provider = provider,
        isAuthenticated = isAuthenticated,
        isOfflineMode = isOfflineMode,
        lastActiveTimestamp = lastActiveTimestamp
    )

    companion object {
        fun fromUserSession(session: UserSession): UserSessionEntity = UserSessionEntity(
            userId = session.userId,
            email = session.email,
            name = session.name,
            avatarUrl = session.avatarUrl,
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            expiresAt = session.expiresAt,
            provider = session.provider,
            isAuthenticated = session.isAuthenticated,
            isOfflineMode = session.isOfflineMode,
            lastActiveTimestamp = session.lastActiveTimestamp
        )
    }
}
