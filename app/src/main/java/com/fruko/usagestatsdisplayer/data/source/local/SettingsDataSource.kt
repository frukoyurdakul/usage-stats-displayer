package com.fruko.usagestatsdisplayer.data.source.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val INCLUDE_SYSTEM_APPS = booleanPreferencesKey("include_system_apps")
    }

    val includeSystemAppsFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[INCLUDE_SYSTEM_APPS] ?: false
    }

    suspend fun setIncludeSystemApps(include: Boolean) {
        dataStore.edit { preferences ->
            preferences[INCLUDE_SYSTEM_APPS] = include
        }
    }
}
