package com.fruko.usagestatsdisplayer.domain.model

data class DailyUsage(
    val dayLabel: String,
    val sessions: List<UsageSession>
)
