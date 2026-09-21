package com.vinote.domain.gamification

import com.vinote.data.local.AchievementDao
import com.vinote.data.local.entities.AchievementEntity
import com.vinote.data.model.GoalItem
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionSource
import com.vinote.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAchievementDao : AchievementDao {
    val store = mutableMapOf<String, AchievementEntity>()

    override fun getAllAchievements(): Flow<List<AchievementEntity>> = flowOf(store.values.toList())

    override suspend fun getAchievementById(id: String): AchievementEntity? = store[id]

    override fun getUnlockedCount(): Flow<Int> = flowOf(store.values.count { it.isUnlocked })

    override suspend fun insertAll(items: List<AchievementEntity>) {
        for (item in items) {
            if (!store.containsKey(item.id)) {
                store[item.id] = item
            }
        }
    }

    override suspend fun updateAchievement(item: AchievementEntity) {
        store[item.id] = item
    }

    override suspend fun updateProgressAndUnlock(id: String, progress: Int, isUnlocked: Boolean, unlockedAt: Long?) {
        val current = store[id] ?: return
        store[id] = current.copy(progress = progress, isUnlocked = isUnlocked, unlockedAt = unlockedAt)
    }
}

class AchievementManagerTest {

    @Test
    fun testSeedDefaultsAndUnlockSmartSaver() = runBlocking {
        val fakeDao = FakeAchievementDao()
        val manager = AchievementManager(fakeDao)

        manager.seedDefaultsIfNeeded()
        assertEquals(5, fakeDao.store.size)

        var unlockedTitle: String? = null

        val transactions = listOf(
            TransactionItem(id = 1L, title = "Gaji", amount = 3000000L, category = "Income", type = TransactionType.INCOME, isConfirmed = true),
            TransactionItem(id = 2L, title = "Belanja", amount = 1000000L, category = "Shopping", type = TransactionType.EXPENSE, isConfirmed = true)
        )

        // Net savings = 2,000,000 > 500,000 target for smart_saver
        manager.evaluate(transactions, emptyList()) { unlocked ->
            unlockedTitle = unlocked.title
        }

        val smartSaver = fakeDao.getAchievementById("smart_saver")
        assertTrue("Smart saver should be unlocked", smartSaver?.isUnlocked == true)
        assertEquals(500000, smartSaver?.progress)
    }

    @Test
    fun testGoalGetterUnlocksWhenGoalAchieved() = runBlocking {
        val fakeDao = FakeAchievementDao()
        val manager = AchievementManager(fakeDao)

        val goals = listOf(
            GoalItem(id = 1L, title = "Beli Laptop", targetAmount = 5000000L, currentAmount = 5000000L)
        )

        manager.evaluate(emptyList(), goals)

        val goalGetter = fakeDao.getAchievementById("first_goal")
        assertTrue("Goal Getter should be unlocked", goalGetter?.isUnlocked == true)
    }
}
