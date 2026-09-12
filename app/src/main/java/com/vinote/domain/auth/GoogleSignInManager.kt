package com.vinote.domain.auth

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.example.R
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Sign-In flow using both GoogleSignInClient (for initiating the flow)
 * and Firebase Authentication.
 *
 * Provides methods to initiate sign-in, handle activity results, and sign out.
 */
@Singleton
class GoogleSignInManager @Inject constructor(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    /**
     * Returns the Intent required to start the Google Sign-In flow.
     */
    fun getSignInIntent() = googleSignInClient.signInIntent

    /**
     * Handles the result from the Google Sign-In Intent.
     * Exchanges the Google ID token for a Firebase credential and signs in to Firebase.
     */
    suspend fun handleSignInResult(data: android.content.Intent?): com.vinote.domain.model.AuthResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(credential).await()
                com.vinote.domain.model.AuthResult.Success
            } else {
                com.vinote.domain.model.AuthResult.Error("Google ID Token not found")
            }
        } catch (e: ApiException) {
            com.vinote.domain.model.AuthResult.Error("Google Sign-In failed: ${e.statusCode}")
        } catch (e: Exception) {
            com.vinote.domain.model.AuthResult.Error("Authentication error: ${e.localizedMessage}")
        }
    }

    /**
     * Signs out the user from Google and Firebase.
     */
    suspend fun signOut() {
        firebaseAuth.signOut()
        googleSignInClient.signOut().await()
    }
}
