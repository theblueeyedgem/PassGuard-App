package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DecryptedVaultItem
import com.example.data.VaultRepository
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(private val repository: VaultRepository) : ViewModel() {

    val vaultItems: StateFlow<List<DecryptedVaultItem>> = repository.allDecryptedItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveItem(item: DecryptedVaultItem) {
        viewModelScope.launch {
            repository.insertOrUpdate(item)
        }
    }

    fun deleteItem(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun generatePassword(): String {
        return CryptoManager.generateSecureRandomPassword(16)
    }
}
