package com.riccardopinato.cashmateai.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "cashmate_settings")

class UserSettings(private val context: Context) {
    private companion object {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val darkTheme: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[DARK_THEME] ?: false
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsStore.edit { preferences ->
            preferences[DARK_THEME] = enabled
        }
    }
}
