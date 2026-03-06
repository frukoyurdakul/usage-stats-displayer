package com.fruko.usagestatsdisplayer.domain.repository

import com.fruko.usagestatsdisplayer.domain.model.UsageStatInfo
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame
import com.fruko.usagestatsdisplayer.domain.model.SortOption
import kotlinx.coroutines.flow.Flow

interface UsageStatsRepository {
    fun getUsageStats(
        timeFrame: TimeFrame,
        sortOption: SortOption,
        searchQuery: String,
        includeSystemApps: Boolean
    ): Flow<List<UsageStatInfo>>
}
