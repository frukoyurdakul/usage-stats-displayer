package com.fruko.usagestatsdisplayer.domain.repository

import com.fruko.usagestatsdisplayer.domain.model.SortOption
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getIncludeSystemApps(): Flow<Boolean>
    suspend fun setIncludeSystemApps(include: Boolean)

    fun getSortOption(): Flow<SortOption>
    suspend fun setSortOption(option: SortOption)

    fun getTimeFrame(): Flow<TimeFrame>
    suspend fun setTimeFrame(timeFrame: TimeFrame)
}
