package com.fruko.usagestatsdisplayer.presentation.home

import androidx.compose.ui.graphics.ImageBitmap
import com.fruko.usagestatsdisplayer.domain.model.SortOption
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame


data class UsageStatUiModel(
    val packageName: String,
    val appName: String,
    val totalUsageText: String,
    val averageUsageText: String?,
    val maxUsageText: String?,
    val icon: ImageBitmap?
)

data class HomeState(
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val usageStats: List<UsageStatUiModel> = emptyList(),
    val timeFrame: TimeFrame = TimeFrame.DAILY,
    val sortOption: SortOption = SortOption.USAGE_DESC,
    val searchQuery: String = ""
)

sealed interface HomeEvent {
    data class SearchQueryChanged(val query: String) : HomeEvent
    data class TimeFrameChanged(val timeFrame: TimeFrame) : HomeEvent
    data class SortOptionChanged(val sortOption: SortOption) : HomeEvent
    object PermissionGranted : HomeEvent
    object RefreshRequested : HomeEvent
}
