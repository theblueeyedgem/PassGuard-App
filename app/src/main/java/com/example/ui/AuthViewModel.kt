package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsStore
import com.example.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthScreenState {
    LOADING,
    SETUP_REQUIRED,
    LOGIN_REQUIRED,
    AUTHENTICATED
}

data class AuthUiState(
    val screenState: AuthScreenState = AuthScreenState.LOADING,
    val error: String? = null
)

class AuthViewModel(private val settingsStore: SettingsStore) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        if (CryptoManager.isSessionActive()) {
            _uiState.update { it.copy(screenState = AuthScreenState.AUTHENTICATED) }
        } else if (settingsStore.hasMasterPasswordTracker()) {
            _uiState.update { it.copy(screenState = AuthScreenState.LOGIN_REQUIRED) }
        } else {
            _uiState.update { it.copy(screenState = AuthScreenState.SETUP_REQUIRED) }
        }
    }

    fun setupMasterPassword(password: String) {
        if (password.length < 8) {
            _uiState.update { it.copy(error = "Password must be at least 8 characters") }
            return
        }
        _uiState.update { it.copy(screenState = AuthScreenState.LOADING, error = null) }
        
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val salt = CryptoManager.generateSalt()
                val (aesKey, hash) = CryptoManager.deriveKeyAndHash(password, salt)
                settingsStore.saveMasterAuth(salt, hash)
                
                CryptoManager.initSession(aesKey)
                _uiState.update { it.copy(screenState = AuthScreenState.AUTHENTICATED, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(screenState = AuthScreenState.SETUP_REQUIRED, error = "Setup failed") }
            }
        }
    }

    fun login(password: String) {
        val storedSalt = settingsStore.getMasterSalt()
        val storedHash = settingsStore.getMasterHash()
        
        if (storedSalt == null || storedHash == null) {
            _uiState.update { it.copy(error = "Corrupted Auth State") }
            return
        }
        
        _uiState.update { it.copy(screenState = AuthScreenState.LOADING, error = null) }
        
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val (aesKey, newHash) = CryptoManager.deriveKeyAndHash(password, storedSalt)
                if (newHash == storedHash) {
                    CryptoManager.initSession(aesKey)
                    _uiState.update { it.copy(screenState = AuthScreenState.AUTHENTICATED, error = null) }
                } else {
                    _uiState.update { it.copy(screenState = AuthScreenState.LOGIN_REQUIRED, error = "Invalid Master Password") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(screenState = AuthScreenState.LOGIN_REQUIRED, error = "Error during login") }
            }
        }
    }

    fun logout() {
        CryptoManager.clearSession()
        checkAuthState()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
