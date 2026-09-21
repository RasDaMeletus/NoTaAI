package com.vinote.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vinote.data.model.TransactionType

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey
    val id: String = "rec_${System.currentTimeMillis()}_${(1000..9999).random()}",
    val userId: String = "",
    val title: String,
    val amount: Long,
    val category: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val walletName: String? = null,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val dayOfPeriod: Int = 1, // e.g. day 1 of month, or 1=Monday
    val nextDueDate: Long,
    val lastExecutedAt: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
