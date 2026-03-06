package com.fruko.usagestatsdisplayer.data.source.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fruko.usagestatsdisplayer.domain.model.SortOption
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val INCLUDE_SYSTEM_APPS = booleanPreferencesKey("include_system_apps")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val TIME_FRAME = stringPreferencesKey("time_frame")
    }

    val includeSystemAppsFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[INCLUDE_SYSTEM_APPS] ?: false
    }

    suspend fun setIncludeSystemApps(include: Boolean) {
        dataStore.edit { preferences ->
            preferences[INCLUDE_SYSTEM_APPS] = include
        }
    }

    val sortOptionFlow: Flow<SortOption> = dataStore.data.map { preferences ->
        val sortString = preferences[SORT_OPTION] ?: SortOption.USAGE_DESC.name
        try {
            SortOption.valueOf(sortString)
        } catch (e: IllegalArgumentException) {
            SortOption.USAGE_DESC
        }
    }

    suspend fun setSortOption(option: SortOption) {
        dataStore.edit { preferences ->
            preferences[SORT_OPTION] = option.name
        }
    }

    val timeFrameFlow: Flow<TimeFrame> = dataStore.data.map { preferences ->
        val timeFrameString = preferences[TIME_FRAME] ?: TimeFrame.DAILY.name
        try {
            TimeFrame.valueOf(timeFrameString)
        } catch (e: IllegalArgumentException) {
            TimeFrame.DAILY
        }
    }

    suspend fun setTimeFrame(timeFrame: TimeFrame) {
        dataStore.edit { preferences ->
            preferences[TIME_FRAME] = timeFrame.name
        }
    }
}
