package com.vinote.domain.usecase

import com.vinote.data.local.BudgetDao
import com.vinote.data.model.BudgetAlertState
import com.vinote.ui.components.FormatUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface BudgetUseCaseInterface {
    fun getBudgetAlertState(): Flow<BudgetAlertState>
    suspend fun evaluateBudgetStatus(
        dailyLimit: Long,
        todaySpent: Long,
        isBudgetAlertActive: Boolean
    ): BudgetAlertState
}

@Singleton
class BudgetUseCaseImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetUseCaseInterface {

    private var alertState = BudgetAlertState()

    override fun getBudgetAlertState(): kotlinx.coroutines.flow.Flow<BudgetAlertState> =
        kotlinx.coroutines.flow.flowOf(alertState)

    override suspend fun evaluateBudgetStatus(
        dailyLimit: Long,
        todaySpent: Long,
        isBudgetAlertActive: Boolean
    ): BudgetAlertState {
        if (!isBudgetAlertActive) return alertState

        if (dailyLimit > 0 && todaySpent > dailyLimit && !alertState.isTriggered) {
            val overage = todaySpent - dailyLimit
            alertState = BudgetAlertState(
                isTriggered = true,
                spentToday = todaySpent,
                dailyLimit = dailyLimit,
                overageAmount = overage,
                message = "CRITICAL ALERT: You spent ${FormatUtils.formatRupiah(todaySpent)} today, exceeding your daily limit of ${FormatUtils.formatRupiah(dailyLimit)} by ${FormatUtils.formatRupiah(overage)}! NoTa is furious! 💢",
                isDismissed = false
            )
        }
        return alertState
    }
}