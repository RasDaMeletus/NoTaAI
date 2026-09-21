package com.vinote.domain.usecase

import com.vinote.data.local.GoalDao
import com.vinote.data.model.GoalItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface GoalUseCaseInterface {
    fun getAllGoals(): Flow<List<GoalItem>>
    suspend fun createGoal(goal: GoalItem)
    suspend fun updateGoal(goal: GoalItem)
    suspend fun deleteGoal(id: Long)
}

@Singleton
class GoalUseCaseImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalUseCaseInterface {

    override fun getAllGoals(): Flow<List<GoalItem>> =
        goalDao.getAllGoals()

    override suspend fun createGoal(goal: GoalItem) {
        goalDao.insertGoal(goal)
    }

    override suspend fun updateGoal(goal: GoalItem) {
        goalDao.updateGoal(goal)
    }

    override suspend fun deleteGoal(id: Long) {
        goalDao.deleteById(id)
    }
}