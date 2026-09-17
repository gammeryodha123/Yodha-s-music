package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = MusicRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun checkLoginStatus() {
        if (repository.isUserLoggedIn()) {
            _authState.value = AuthState.Success
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val success = repository.signInWithEmail(email, pass)
            _authState.value = if (success) AuthState.Success else AuthState.Error("Sign in failed. Check credentials.")
        }
    }

    fun signUpWithEmail(email: String, pass: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val success = repository.signUpWithEmail(email, pass)
            _authState.value = if (success) AuthState.Success else AuthState.Error("Sign up failed. User may already exist.")
        }
    }

    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val success = repository.signInWithGoogle(idToken)
            _authState.value = if (success) AuthState.Success else AuthState.Error("Google Sign in failed.")
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
