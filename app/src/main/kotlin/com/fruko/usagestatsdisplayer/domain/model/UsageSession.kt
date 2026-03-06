package com.fruko.usagestatsdisplayer.domain.model

data class UsageSession(
    val startTime: Long,
    val endTime: Long
) {
    val durationMillis: Long get() = endTime - startTime
}
