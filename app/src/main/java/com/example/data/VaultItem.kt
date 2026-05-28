package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titleEnc: String,
    val usernameEnc: String,
    val urlEnc: String,
    val passwordEnc: String,
    val notesEnc: String,
    val totpSecretEnc: String,
    val createdAt: Long = System.currentTimeMillis()
)

// A plain data class for UI to display decrypted properties
data class DecryptedVaultItem(
    val id: Int = 0,
    val title: String,
    val username: String,
    val url: String,
    val password: String,
    val notes: String,
    val totpSecret: String,
    val createdAt: Long
)
