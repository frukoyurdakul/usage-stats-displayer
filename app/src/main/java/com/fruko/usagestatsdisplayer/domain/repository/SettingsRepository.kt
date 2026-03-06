package com.fruko.usagestatsdisplayer.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getIncludeSystemApps(): Flow<Boolean>
    suspend fun setIncludeSystemApps(include: Boolean)
}
