package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("passguard_settings", Context.MODE_PRIVATE)

    fun hasMasterPasswordTracker(): Boolean {
        return prefs.contains("master_salt") && prefs.contains("master_hash")
    }

    fun saveMasterAuth(saltBase64: String, hashBase64: String) {
        prefs.edit()
            .putString("master_salt", saltBase64)
            .putString("master_hash", hashBase64)
            .apply()
    }

    fun getMasterSalt(): String? = prefs.getString("master_salt", null)

    fun getMasterHash(): String? = prefs.getString("master_hash", null)
    
    fun wipe() {
        prefs.edit().clear().apply()
    }
}
