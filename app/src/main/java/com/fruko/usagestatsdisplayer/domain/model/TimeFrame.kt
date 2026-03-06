package com.fruko.usagestatsdisplayer.domain.model

enum class TimeFrame(val days: Int) {
    DAILY(1),
    WEEKLY(7),
    MONTHLY(30)
}
