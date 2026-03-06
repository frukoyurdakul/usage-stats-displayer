package com.fruko.usagestatsdisplayer.presentation.settings

data class SettingsState(
    val includeSystemApps: Boolean = false
)

sealed interface SettingsEvent {
    data class ToggleSystemApps(val include: Boolean) : SettingsEvent
}
