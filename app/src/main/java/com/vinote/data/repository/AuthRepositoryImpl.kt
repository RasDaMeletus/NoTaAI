package com.vinote.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.vinote.domain.model.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository using Firebase Authentication.
 *
 * This manages authentication state directly with FirebaseAuth, providing user ID
 * and listening for auth state changes.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _userId.value = auth.currentUser?.uid
        }
        _userId.value = firebaseAuth.currentUser?.uid
    }

    override fun getUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    override fun setUserId(userId: String?) {
        // For Firebase, setting userId directly is handled by the auth state listener.
    }

    override fun clearUserId() {
        firebaseAuth.signOut()
    }

    override fun isAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun getIdToken(): String? {
        return firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
    }

    fun getCurrentFirebaseUser() = firebaseAuth.currentUser

}
