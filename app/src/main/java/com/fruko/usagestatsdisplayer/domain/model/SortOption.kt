package com.fruko.usagestatsdisplayer.domain.model

enum class SortOption {
    APP_NAME_ASC,
    APP_NAME_DESC,
    USAGE_DESC,
    USAGE_ASC;
    
    companion object {
        fun toggle(current: SortOption): SortOption {
            return when (current) {
                APP_NAME_ASC -> APP_NAME_DESC
                APP_NAME_DESC -> APP_NAME_ASC
                USAGE_DESC -> USAGE_ASC
                USAGE_ASC -> USAGE_DESC
            }
        }
    }
}
