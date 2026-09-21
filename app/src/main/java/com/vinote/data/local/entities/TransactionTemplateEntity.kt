package com.vinote.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vinote.data.model.TransactionType

@Entity(tableName = "transaction_templates")
data class TransactionTemplateEntity(
    @PrimaryKey
    val id: String = "tmpl_${System.currentTimeMillis()}_${(1000..9999).random()}",
    val userId: String = "",
    val name: String,
    val amount: Long, // in IDR
    val category: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val walletName: String? = null,
    val colorHex: String = "#4F8CFF",
    val iconName: String = "EditNote",
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
