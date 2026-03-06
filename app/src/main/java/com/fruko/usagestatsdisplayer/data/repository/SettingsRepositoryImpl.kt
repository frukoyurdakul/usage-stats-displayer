package com.fruko.usagestatsdisplayer.data.repository

import com.fruko.usagestatsdisplayer.data.source.local.SettingsDataSource
import com.fruko.usagestatsdisplayer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataSource: SettingsDataSource
) : SettingsRepository {

    override fun getIncludeSystemApps(): Flow<Boolean> {
        return dataSource.includeSystemAppsFlow
    }

    override suspend fun setIncludeSystemApps(include: Boolean) {
        dataSource.setIncludeSystemApps(include)
    }
}
