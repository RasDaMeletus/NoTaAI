package com.vinote.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vinote.data.repository.AuthRepository
import com.vinote.data.repository.AuthRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for AuthRepository to ensure secure persistent auth state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        authRepository = AuthRepositoryImpl(context)
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