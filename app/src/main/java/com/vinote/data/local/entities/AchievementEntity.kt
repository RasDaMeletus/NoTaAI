package com.vinote.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "icon")
    val icon: String = "🏆",

    @ColumnInfo(name = "progress")
    val progress: Int = 0,

    @ColumnInfo(name = "target")
    val target: Int = 1,

    @ColumnInfo(name = "is_unlocked")
    val isUnlocked: Boolean = false,

    @ColumnInfo(name = "unlocked_at")
    val unlockedAt: Long? = null
)
