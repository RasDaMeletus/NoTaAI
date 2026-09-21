package com.vinote.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vinote.data.model.TransactionType

@Entity(tableName = "transaction_categories")
data class TransactionCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val name: String,
    val type: TransactionType,
    val isCustom: Boolean = false
)