package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.SettingsStore
import com.example.data.VaultDatabase
import com.example.data.VaultRepository

class PassGuardApplication : Application() {

    lateinit var database: VaultDatabase
        private set

    lateinit var repository: VaultRepository
        private set

    lateinit var settingsStore: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        
        database = Room.databaseBuilder(
            applicationContext,
            VaultDatabase::class.java,
            "vault_db"
        ).fallbackToDestructiveMigration().build()
        
        repository = VaultRepository(database.vaultDao())
        
        settingsStore = SettingsStore(applicationContext)
    }
}
