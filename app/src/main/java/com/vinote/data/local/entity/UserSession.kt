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
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)
