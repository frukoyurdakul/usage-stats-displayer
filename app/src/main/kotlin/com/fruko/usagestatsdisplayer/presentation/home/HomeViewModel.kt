package com.fruko.usagestatsdisplayer.presentation.home

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fruko.usagestatsdisplayer.domain.model.SortOption
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame
import com.fruko.usagestatsdisplayer.domain.repository.SettingsRepository
import com.fruko.usagestatsdisplayer.domain.repository.UsageStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    private val settingsRepository: SettingsRepository,
    private val packageManager: PackageManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _hasPermission = MutableStateFlow(false)
    private val _forceRefresh = MutableStateFlow(0) // Trigger manual refresh

    private var cachedList: List<UsageStatUiModel> = emptyList()

    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    private fun formatTime(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (millis % (1000 * 60)) / 1000
        
        val parts = mutableListOf<String>()
        if (hours > 0) parts.add("$hours hours")
        if (minutes > 0) parts.add("$minutes minutes")
        parts.add("$seconds seconds")
        
        return parts.joinToString(" ")
    }

    @OptIn(FlowPreview::class)
    val state: StateFlow<HomeState> = combine(
        settingsRepository.getTimeFrame(),
        settingsRepository.getSortOption(),
        _searchQuery.debounce(300).distinctUntilChanged(),
        _hasPermission,
        settingsRepository.getIncludeSystemApps()
    ) { tf, so, sq, hasP, includeSys ->
        Params(tf, so, sq, hasP, includeSys)
    }.combine(_forceRefresh) { p, _ ->
        p
    }.flatMapLatest { p ->
        if (!p.hasP) {
            flowOf(HomeState(hasPermission = false, isLoading = false))
        } else {
            usageStatsRepository.getUsageStats(p.tf, p.so, p.sq, p.includeSys).map { stats ->
                val uiModels = stats.map { info ->
                    val totalUsageText = "Total Usage: ${formatTime(info.totalTimeInForeground)}"
                    val averageUsageText = if (p.tf != TimeFrame.DAILY) {
                        "Avg per Day: ${formatTime(info.averageTime)}"
                    } else null
                    val maxUsageText = if (p.tf != TimeFrame.DAILY) {
                        val date = dateFormat.format(Date(info.maxUsageDayTimestamp))
                        "Max Usage: ${formatTime(info.maxUsageInDay)} (on $date)"
                    } else null

                    UsageStatUiModel(
                        packageName = info.packageName,
                        appName = info.appName,
                        totalUsageText = totalUsageText,
                        averageUsageText = averageUsageText,
                        maxUsageText = maxUsageText,
                        icon = packageManager.getApplicationIcon(info.packageName)
                    )
                }

                cachedList = uiModels
                HomeState(
                    isLoading = false,
                    hasPermission = true,
                    usageStats = uiModels,
                    timeFrame = p.tf,
                    sortOption = p.so,
                    searchQuery = p.sq
                )
            }.flowOn(Dispatchers.Default).onStart {
                emit(
                    HomeState(
                        isLoading = true,
                        hasPermission = true,
                        usageStats = cachedList,
                        timeFrame = p.tf,
                        sortOption = p.so,
                        searchQuery = p.sq,
                    ),
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SearchQueryChanged -> _searchQuery.value = event.query
            is HomeEvent.SortOptionChanged -> {
                viewModelScope.launch {
                    settingsRepository.setSortOption(event.sortOption)
                }
            }
            is HomeEvent.TimeFrameChanged -> {
                viewModelScope.launch {
                    settingsRepository.setTimeFrame(event.timeFrame)
                }
            }
            is HomeEvent.PermissionGranted -> _hasPermission.value = true
            is HomeEvent.RefreshRequested -> _forceRefresh.value += 1
        }
    }

    private data class Params(
        val tf: TimeFrame,
        val so: SortOption,
        val sq: String,
        val hasP: Boolean,
        val includeSys: Boolean
    )
}
