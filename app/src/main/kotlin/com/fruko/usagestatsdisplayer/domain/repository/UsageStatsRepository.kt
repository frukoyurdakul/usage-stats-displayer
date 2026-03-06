package com.fruko.usagestatsdisplayer.domain.repository

import com.fruko.usagestatsdisplayer.domain.model.DailyUsage
import com.fruko.usagestatsdisplayer.domain.model.SortOption
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame
import com.fruko.usagestatsdisplayer.domain.model.UsageStatInfo
import kotlinx.coroutines.flow.Flow

interface UsageStatsRepository {
    fun getUsageStats(
        timeFrame: TimeFrame,
        sortOption: SortOption,
        searchQuery: String,
        includeSystemApps: Boolean
    ): Flow<List<UsageStatInfo>>

    fun getAppSessions(packageName: String, timeFrame: TimeFrame): Flow<List<DailyUsage>>
    fun getAppUsage(packageName: String, timeFrame: TimeFrame): Flow<UsageStatInfo?>
}
