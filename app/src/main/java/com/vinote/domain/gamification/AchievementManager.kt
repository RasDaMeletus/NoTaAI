package com.vinote.domain.gamification

import com.vinote.data.local.AchievementDao
import com.vinote.data.local.entities.AchievementEntity
import com.vinote.data.model.GoalItem
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AchievementManager(
    private val achievementDao: AchievementDao
) {
    companion object {
        val DEFAULT_ACHIEVEMENTS = listOf(
            AchievementEntity(
                id = "smart_saver",
                type = "SAVINGS",
                title = "Hemat Pahlawan",
                description = "Mencatat total tabungan positif lebih dari Rp 500.000",
                icon = "🏅",
                progress = 0,
                target = 500000,
                isUnlocked = false
            ),
            AchievementEntity(
                id = "first_goal",
                type = "GOAL",
                title = "Goal Getter",
                description = "Capai 100% dari setidaknya satu target tabungan",
                icon = "🎯",
                progress = 0,
                target = 1,
                isUnlocked = false
            ),
            AchievementEntity(
                id = "streak_master",
                type = "STREAK",
                title = "Streak Master",
                description = "Catat minimal 7 transaksi pengeluaran/pemasukan",
                icon = "📊",
                progress = 0,
                target = 7,
                isUnlocked = false
            ),
            AchievementEntity(
                id = "receipt_hunter",
                type = "RECEIPT",
                title = "Receipt Hunter",
                description = "Pindai dan catat transaksi dari 3 struk belanja",
                icon = "🧾",
                progress = 0,
                target = 3,
                isUnlocked = false
            ),
            AchievementEntity(
                id = "wallet_master",
                type = "WALLET",
                title = "Automation Master",
                description = "Mendeteksi otomatis minimal 3 transaksi e-wallet/bank",
                icon = "⚡",
                progress = 0,
                target = 3,
                isUnlocked = false
            )
        )
    }

    suspend fun seedDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        val existing = achievementDao.getAllAchievements().first()
        val existingIds = existing.map { it.id }.toSet()
        DEFAULT_ACHIEVEMENTS.filter { it.id !in existingIds }.let { missing ->
            if (missing.isNotEmpty()) achievementDao.insertAll(missing)
        }
    }

    suspend fun evaluate(
        transactions: List<TransactionItem>,
        goals: List<GoalItem>,
        onNewAchievementUnlocked: ((AchievementEntity) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netSavings = (totalIncome - totalExpense).coerceAtLeast(0L).toInt()

        // 1. Smart Saver
        updateProgress("smart_saver", netSavings, onNewAchievementUnlocked)

        // 2. Goal Getter
        val achievedGoalsCount = goals.count { it.currentAmount >= it.targetAmount && it.targetAmount > 0 }
        updateProgress("first_goal", achievedGoalsCount, onNewAchievementUnlocked)

        // 3. Streak / Active Transactions
        val confirmedTxCount = transactions.count { it.isConfirmed }
        updateProgress("streak_master", confirmedTxCount, onNewAchievementUnlocked)

        // 4. Receipt Hunter (transactions with source RECEIPT_OCR or title contains Struk)
        val receiptTxCount = transactions.count {
            it.source.name == "RECEIPT_OCR" || it.title.contains("Struk", ignoreCase = true)
        }
        updateProgress("receipt_hunter", receiptTxCount, onNewAchievementUnlocked)

        // 5. Automation Master (transactions with wallet or source WALLET_NOTIFICATION)
        val autoWalletCount = transactions.count {
            it.source.name.contains("WALLET", ignoreCase = true) || (it.walletName != null && it.walletName != "Cash")
        }
        updateProgress("wallet_master", autoWalletCount, onNewAchievementUnlocked)
    }

    private suspend fun updateProgress(
        id: String,
        currentValue: Int,
        onUnlocked: ((AchievementEntity) -> Unit)?
    ) {
        val current = achievementDao.getAchievementById(id) ?: return
        val cappedProgress = currentValue.coerceAtMost(current.target)
        val willUnlock = cappedProgress >= current.target

        if (willUnlock && !current.isUnlocked) {
            val updated = current.copy(
                progress = current.target,
                isUnlocked = true,
                unlockedAt = System.currentTimeMillis()
            )
            achievementDao.updateAchievement(updated)
            onUnlocked?.invoke(updated)
        } else if (cappedProgress != current.progress) {
            val updated = current.copy(progress = cappedProgress)
            achievementDao.updateAchievement(updated)
        }
    }
}
