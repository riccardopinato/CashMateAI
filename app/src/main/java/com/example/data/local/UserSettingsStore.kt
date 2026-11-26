package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserSettingsStore(private val context: Context) {
    companion object {
        private val IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val CURRENCY = stringPreferencesKey("currency")
        private val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        private val LAST_BACKUP_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("last_backup_timestamp")
    }

    val isPremium: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_PREMIUM] ?: false
    }

    val currency: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENCY] ?: "€"
    }

    // Fail-closed: migration/sync are not validated; retain legacy preference without acting on it.
    val autoSyncEnabled: Flow<Boolean> = context.dataStore.data.map { false }

    val lastBackupTimestamp: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_BACKUP_TIMESTAMP] ?: 0L
    }

    suspend fun setPremium(isPremium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_PREMIUM] = isPremium
        }
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY] = currency
        }
    }

    suspend fun setAutoSync(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setLastBackup(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }
}
