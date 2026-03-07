package com.fruko.usagestatsdisplayer.presentation.details

import android.content.pm.PackageManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fruko.usagestatsdisplayer.domain.model.DailyUsage
import com.fruko.usagestatsdisplayer.domain.model.UsageStatInfo
import com.fruko.usagestatsdisplayer.domain.repository.SettingsRepository
import com.fruko.usagestatsdisplayer.domain.repository.UsageStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    private val settingsRepository: SettingsRepository,
    private val packageManager: PackageManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packageName: String = checkNotNull(savedStateHandle["packageName"])
    private val appName: String = checkNotNull(savedStateHandle["appName"])

    private val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("MMMM d", Locale.getDefault())

    private val _state = MutableStateFlow(
        DetailsState(packageName = packageName, appName = appName)
    )
    val state: StateFlow<DetailsState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val initialTimeFrame = settingsRepository.getTimeFrame().first()
            val appIcon = try {
                packageManager.getApplicationIcon(packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
            _state.update { it.copy(timeFrame = initialTimeFrame, icon = appIcon) }
            loadData()
        }
    }

    fun onEvent(event: DetailsEvent) {
        when (event) {
            is DetailsEvent.TimeFrameChanged -> {
                // Deliberately NOT writing back to DataStore — screen-local only
                _state.update { it.copy(timeFrame = event.timeFrame) }
                loadData()
            }
        }
    }

    private fun loadData() {
        val timeFrame = _state.value.timeFrame
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.Default) {
            val appUsage = usageStatsRepository.getAppUsage(packageName, timeFrame).first()
            val rawSessions = usageStatsRepository.getAppSessions(packageName, timeFrame).first()

            val dailyUiModels = rawSessions.map { daily -> daily.toUiModel() }

            _state.update {
                it.copy(
                    isLoading = false,
                    appName = appName,
                    averageUsageText = appUsage?.let { u -> formatDuration(u.averageTime) } ?: "",
                    peakDayText = appUsage?.toPeakText(),
                    dailyUsages = dailyUiModels
                )
            }
        }
    }

    private fun DailyUsage.toUiModel(): DailyUsageUiModel = DailyUsageUiModel(
        dayLabel = dayLabel,
        sessions = sessions.map { session ->
            val start = clockFormat.format(session.startTime)
            val end = clockFormat.format(session.endTime)
            SessionUiModel(
                startTime = session.startTime,
                timeRange = "$start – $end",
                duration = formatDuration(session.durationMillis)
            )
        }
    )

    private fun UsageStatInfo.toPeakText(): String? {
        if (maxUsageInDay <= 0L) return null
        val day = dayFormat.format(maxUsageDayTimestamp)
        return "Peak day: $day (${formatDuration(maxUsageInDay)})"
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val parts = mutableListOf<String>()
        if (hours > 0) parts.add("$hours hours")
        if (minutes > 0) parts.add("$minutes minutes")
        parts.add("$seconds seconds")
        return parts.joinToString(" ")
    }
}
