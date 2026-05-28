package com.example.data

import com.example.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultRepository(private val vaultDao: VaultDao) {

    // Reads from DB and decrypts on the fly
    val allDecryptedItems: Flow<List<DecryptedVaultItem>> = vaultDao.getAllItems().map { items ->
        items.map { item ->
            // Only decrypt if session is active
            if (CryptoManager.isSessionActive()) {
                try {
                    DecryptedVaultItem(
                        id = item.id,
                        title = CryptoManager.decrypt(item.titleEnc),
                        username = CryptoManager.decrypt(item.usernameEnc),
                        url = CryptoManager.decrypt(item.urlEnc),
                        password = CryptoManager.decrypt(item.passwordEnc),
                        notes = CryptoManager.decrypt(item.notesEnc),
                        totpSecret = CryptoManager.decrypt(item.totpSecretEnc),
                        createdAt = item.createdAt
                    )
                } catch (e: Exception) {
                    // Fallback to empty if decryption fails (e.g., wrong key or corrupt data)
                    DecryptedVaultItem(
                        id = item.id,
                        title = "Decryption Error",
                        username = "", url = "", password = "", notes = "", totpSecret = "",
                        createdAt = item.createdAt
                    )
                }
            } else {
                 DecryptedVaultItem(
                        id = item.id,
                        title = "Locked",
                        username = "", url = "", password = "", notes = "", totpSecret = "",
                        createdAt = item.createdAt
                    )
            }
        }
    }

    suspend fun insertOrUpdate(decryptedItem: DecryptedVaultItem) {
        if (!CryptoManager.isSessionActive()) throw IllegalStateException("Cannot insert, vault is locked")
        val encryptedItem = VaultItem(
            id = decryptedItem.id,
            titleEnc = CryptoManager.encrypt(decryptedItem.title),
            usernameEnc = CryptoManager.encrypt(decryptedItem.username),
            urlEnc = CryptoManager.encrypt(decryptedItem.url),
            passwordEnc = CryptoManager.encrypt(decryptedItem.password),
            notesEnc = CryptoManager.encrypt(decryptedItem.notes),
            totpSecretEnc = CryptoManager.encrypt(decryptedItem.totpSecret)
        )
        if (decryptedItem.id == 0) {
            vaultDao.insertItem(encryptedItem)
        } else {
            vaultDao.updateItem(encryptedItem)
        }
    }

    suspend fun deleteById(id: Int) = vaultDao.deleteItemById(id)
}
