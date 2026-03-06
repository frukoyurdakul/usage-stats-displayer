package com.fruko.usagestatsdisplayer.data.repository

import com.fruko.usagestatsdisplayer.data.source.local.UsageStatsDataSource
import com.fruko.usagestatsdisplayer.domain.model.SortOption
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame
import com.fruko.usagestatsdisplayer.domain.model.UsageStatInfo
import com.fruko.usagestatsdisplayer.domain.repository.UsageStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class UsageStatsRepositoryImpl @Inject constructor(
    private val dataSource: UsageStatsDataSource
) : UsageStatsRepository {

    override fun getUsageStats(
        timeFrame: TimeFrame,
        sortOption: SortOption,
        searchQuery: String,
        includeSystemApps: Boolean
    ): Flow<List<UsageStatInfo>> = flow {
        val stats = dataSource.getUsageStats(timeFrame, includeSystemApps)
        
        var filteredStats = stats
        if (searchQuery.isNotBlank()) {
            filteredStats = stats.filter { it.appName.contains(searchQuery, ignoreCase = true) }
        }
        
        val sortedStats = when (sortOption) {
            SortOption.APP_NAME_ASC -> filteredStats.sortedBy { it.appName.lowercase() }
            SortOption.APP_NAME_DESC -> filteredStats.sortedByDescending { it.appName.lowercase() }
            SortOption.USAGE_DESC -> filteredStats.sortedByDescending { it.totalTimeInForeground }
            SortOption.USAGE_ASC -> filteredStats.sortedBy { it.totalTimeInForeground }
        }
        
        emit(sortedStats)
    }.flowOn(Dispatchers.Default)
}
