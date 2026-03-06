package com.fruko.usagestatsdisplayer.data.repository

import com.fruko.usagestatsdisplayer.data.source.local.SettingsDataSource
import com.fruko.usagestatsdisplayer.domain.model.SortOption
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame
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

    override fun getSortOption(): Flow<SortOption> {
        return dataSource.sortOptionFlow
    }

    override suspend fun setSortOption(option: SortOption) {
        dataSource.setSortOption(option)
    }

    override fun getTimeFrame(): Flow<TimeFrame> {
        return dataSource.timeFrameFlow
    }

    override suspend fun setTimeFrame(timeFrame: TimeFrame) {
        dataSource.setTimeFrame(timeFrame)
    }
}
