package com.fruko.usagestatsdisplayer.domain.model

data class UsageStatInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long, // Total usage across the timeframe
    val averageTime: Long, // Average usage per day in the timeframe
    val maxUsageInDay: Long, // Max usage on a single day
    val maxUsageDayTimestamp: Long // The timestamp of the day with max usage
)
