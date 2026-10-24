package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.User
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed interface AuthState {
    object Initial : AuthState
    object Loading : AuthState
    data class Authenticated(val firebaseUser: FirebaseUser?, val user: User) : AuthState
    object Unauthenticated : AuthState
    data class Error(val message: String) : AuthState
}

class FirebaseAuthRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val auth: FirebaseAuth get() = Firebase.auth
    private val credentialManager = CredentialManager.create(context)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Observe Firebase Auth state
        try {
            auth.addAuthStateListener { firebaseAuth ->
                val fbUser = firebaseAuth.currentUser
                if (fbUser != null) {
                    val user = User(
                        uid = fbUser.uid,
                        fullName = fbUser.displayName ?: "LinkUp User",
                        fullNameLower = (fbUser.displayName ?: "LinkUp User").lowercase(),
                        email = fbUser.email ?: "",
                        userId = "",
                        isOnline = true
                    )
                    _authState.value = AuthState.Authenticated(fbUser, user)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        } catch (_: Exception) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<User> {
        return try {
            _authState.value = AuthState.Loading
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val fbUser = authResult.user
            val user = User(
                uid = fbUser?.uid ?: UUID.randomUUID().toString(),
                fullName = fbUser?.displayName ?: "Google User",
                fullNameLower = (fbUser?.displayName ?: "Google User").lowercase(),
                email = fbUser?.email ?: "",
                userId = "",
                isOnline = true
            )
            _authState.value = AuthState.Authenticated(fbUser, user)
            Result.success(user)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.localizedMessage ?: "Google Sign-In failed")
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailCredentials(email: String, pass: String): Result<User> {
        return try {
            _authState.value = AuthState.Loading
            val uid = auth.currentUser?.uid ?: UUID.nameUUIDFromBytes(email.toByteArray()).toString()
            val name = email.substringBefore("@").replace(".", " ")
                .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            val user = User(
                uid = uid,
                fullName = name,
                fullNameLower = name.lowercase(),
                email = email,
                userId = "",
                isOnline = true
            )
            _authState.value = AuthState.Authenticated(auth.currentUser, user)
            Result.success(user)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.localizedMessage ?: "Sign-in failed")
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmailCredentials(email: String, pass: String): Result<User> {
        return try {
            _authState.value = AuthState.Loading
            val uid = auth.currentUser?.uid ?: UUID.nameUUIDFromBytes(email.toByteArray()).toString()
            val name = email.substringBefore("@").replace(".", " ")
                .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            val user = User(
                uid = uid,
                fullName = name,
                fullNameLower = name.lowercase(),
                email = email,
                userId = "",
                isOnline = true
            )
            _authState.value = AuthState.Authenticated(auth.currentUser, user)
            Result.success(user)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.localizedMessage ?: "Sign-up failed")
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        }
    }
}
