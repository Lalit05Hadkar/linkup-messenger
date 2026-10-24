package com.example.data.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Android Jetpack Compose provider equivalent to Flutter Riverpod's StateNotifierProvider/NotifierProvider.
 * Exposes a reactive StateFlow<AuthState> that components collect with lifecycle awareness.
 */
class AuthProvider(application: Application) : AndroidViewModel(application) {
    val repository = FirebaseAuthRepository(application.applicationContext)

    val authState: StateFlow<AuthState> = repository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Initial)

    fun sendPasswordReset(email: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.sendPasswordReset(email)
            onDone(res.isSuccess)
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.signOut()
            onComplete()
        }
    }
}
