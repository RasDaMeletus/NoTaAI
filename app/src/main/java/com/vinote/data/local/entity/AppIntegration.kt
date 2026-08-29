package com.vinote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks which financial provider apps are enabled for notification detection.
 */
@Entity(
    tableName = "app_integrations",
    indices = [Index(value = ["userId"], name = "idx_app_integrations_userId")]
)
data class AppIntegration(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String,
    val provider: String,          // "gopay", "dana", etc.
    val packageName: String,       // Android package name
    val displayName: String,       // "GoPay", "DANA"
    val isEnabled: Boolean = true,
    val parserVersion: Int = 1,
    val lastNotificationAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
