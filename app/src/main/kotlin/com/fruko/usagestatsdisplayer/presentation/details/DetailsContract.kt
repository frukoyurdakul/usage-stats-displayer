package com.fruko.usagestatsdisplayer.presentation.details

import android.graphics.drawable.Drawable
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame

data class SessionUiModel(
    val startTime: Long,       // kept for list key identity
    val timeRange: String,     // e.g. "15:00:00 – 18:15:15"
    val duration: String       // e.g. "3 hours 15 minutes 15 seconds"
)

data class DailyUsageUiModel(
    val dayLabel: String,
    val sessions: List<SessionUiModel>
)

data class DetailsState(
    val packageName: String = "",
    val isLoading: Boolean = true,
    val timeFrame: TimeFrame = TimeFrame.DAILY,
    val icon: Drawable? = null,
    val appName: String = "",
    val averageUsageText: String = "",
    val peakDayText: String? = null,
    val dailyUsages: List<DailyUsageUiModel> = emptyList()
)

sealed class DetailsEvent {
    object RefreshRequested : DetailsEvent()
    data class TimeFrameChanged(val timeFrame: TimeFrame) : DetailsEvent()
}
