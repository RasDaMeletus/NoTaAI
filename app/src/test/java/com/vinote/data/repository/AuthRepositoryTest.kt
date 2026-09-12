package com.vinote.data.repository

import com.vinote.data.local.entity.UserSession
import io.github.jan.supabase.gotrue.user.UserSession as SupabaseSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAuthRepository : AuthRepository {
    private var userId: String? = null
    private val _currentSession = MutableStateFlow<UserSession?>(null)
    override val currentSession: StateFlow<UserSession?> = _currentSession

    private val _supabaseSessionFlow = MutableStateFlow<SupabaseSession?>(null)
    override val supabaseSessionFlow: StateFlow<SupabaseSession?> = _supabaseSessionFlow

    override fun getUserId(): String? = userId
    override fun getCanonicalUserId(): String = userId ?: "user_default"

    override fun setUserId(userId: String?) {
        this.userId = userId
        _currentSession.value = userId?.let {
            UserSession(userId = it, email = "test@vinote.app", name = "Test User", isAuthenticated = true)
        }
    }

    override fun clearUserId() {
        userId = null
        _currentSession.value = null
    }

    override fun isAuthenticated(): Boolean = userId != null

    override suspend fun signInWithSupabaseGoogle(): Result<Unit> = Result.success(Unit)
    override suspend fun signUpWithSupabaseEmail(email: String, password: String): Result<Unit> = Result.success(Unit)
    override suspend fun signInWithSupabaseEmail(email: String, password: String): Result<Unit> = Result.success(Unit)
    override suspend fun signOutFromSupabase(): Result<Unit> = Result.success(Unit)
}

class AuthRepositoryTest {

    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        authRepository = FakeAuthRepository()
    }

    @Test
    fun `should start with no authenticated user`() {
        assertNull(authRepository.getUserId())
        assertFalse(authRepository.isAuthenticated())
    }

    @Test
    fun `should store and retrieve authenticated user id`() {
        val testUserId = "test_user_123"
        authRepository.setUserId(testUserId)
        val retrievedId = authRepository.getUserId()
        assertEquals(testUserId, retrievedId)
        assertTrue(authRepository.isAuthenticated())
    }

    @Test
    fun `should clear user id on clearUserId`() {
        authRepository.setUserId("test_user")
        authRepository.clearUserId()
        assertNull(authRepository.getUserId())
        assertFalse(authRepository.isAuthenticated())
    }
}