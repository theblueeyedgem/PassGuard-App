package com.example.security

import android.util.Base64
import java.security.SecureRandom
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 300000 // Halve the iteration count to improve speed while still keeping it strong
    private const val KEY_LENGTH = 256
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val SALT_LENGTH_BYTE = 16

    private var sessionKey: SecretKey? = null

    fun deriveKeyAndHash(password: String, saltBase64: String): Pair<SecretKey, String> {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val secretKeyBytes = factory.generateSecret(spec).encoded
        
        // This is the AES key. It must NEVER be stored.
        val aesKey = SecretKeySpec(secretKeyBytes, "AES")
        
        // Hash the AES key with SHA-256 to create a secure verification hash that is safe to store in SharedPreferences
        val digest = MessageDigest.getInstance("SHA-256")
        val verificationHash = Base64.encodeToString(digest.digest(secretKeyBytes), Base64.NO_WRAP)
        
        return Pair(aesKey, verificationHash)
    }

    fun initSession(key: SecretKey) {
        sessionKey = key
    }

    fun clearSession() {
        sessionKey = null
    }
    
    fun isSessionActive() = sessionKey != null

    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH_BYTE)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val key = sessionKey ?: throw IllegalStateException("Session key not initialized")
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Return IV + CipherText encoded in Base64
        val combined = iv + cipherText
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(cipherTextBase64: String): String {
        if (cipherTextBase64.isEmpty()) return ""
        val key = sessionKey ?: throw IllegalStateException("Session key not initialized")
        val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
        
        val iv = combined.copyOfRange(0, IV_LENGTH_BYTE)
        val cipherText = combined.copyOfRange(IV_LENGTH_BYTE, combined.size)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        val plainTextBytes = cipher.doFinal(cipherText)
        return String(plainTextBytes, Charsets.UTF_8)
    }

    // Hash string for verifying master password (Zero-Knowledge) is handled in deriveKeyAndHash
    
    fun generateSecureRandomPassword(length: Int = 16): String {
        val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowercase = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val symbols = "!@#$%^&*()-_=+[]{}|;:,.<>?"
        val allChars = uppercase + lowercase + digits + symbols
        val random = SecureRandom()
        
        val password = StringBuilder()
        // Ensure at least one of each type
        password.append(uppercase[random.nextInt(uppercase.length)])
        password.append(lowercase[random.nextInt(lowercase.length)])
        password.append(digits[random.nextInt(digits.length)])
        password.append(symbols[random.nextInt(symbols.length)])
        
        for (i in 4 until length) {
            password.append(allChars[random.nextInt(allChars.length)])
        }
        
        return password.toString().toList().shuffled(random).joinToString("")
    }
}
