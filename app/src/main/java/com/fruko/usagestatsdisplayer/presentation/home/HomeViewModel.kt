package com.fruko.usagestatsdisplayer.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fruko.usagestatsdisplayer.domain.model.SortOption
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame
import com.fruko.usagestatsdisplayer.domain.repository.SettingsRepository
import com.fruko.usagestatsdisplayer.domain.repository.UsageStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _timeFrame = MutableStateFlow(TimeFrame.DAILY)
    private val _sortOption = MutableStateFlow(SortOption.USAGE_DESC)
    private val _searchQuery = MutableStateFlow("")
    private val _hasPermission = MutableStateFlow(false)
    private val _forceRefresh = MutableStateFlow(0) // Trigger manual refresh

    val state: StateFlow<HomeState> = combine(
        _timeFrame,
        _sortOption,
        _searchQuery,
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
                HomeState(
                    isLoading = false,
                    hasPermission = true,
                    usageStats = stats,
                    timeFrame = p.tf,
                    sortOption = p.so,
                    searchQuery = p.sq
                )
            }.onStart {
                emit(HomeState(isLoading = true, hasPermission = true, timeFrame = p.tf, sortOption = p.so, searchQuery = p.sq))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SearchQueryChanged -> _searchQuery.value = event.query
            is HomeEvent.SortOptionChanged -> _sortOption.value = event.sortOption
            is HomeEvent.TimeFrameChanged -> _timeFrame.value = event.timeFrame
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
