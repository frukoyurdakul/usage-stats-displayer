package com.fruko.usagestatsdisplayer.di

import com.fruko.usagestatsdisplayer.data.repository.SettingsRepositoryImpl
import com.fruko.usagestatsdisplayer.data.repository.UsageStatsRepositoryImpl
import com.fruko.usagestatsdisplayer.domain.repository.SettingsRepository
import com.fruko.usagestatsdisplayer.domain.repository.UsageStatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindUsageStatsRepository(
        usageStatsRepositoryImpl: UsageStatsRepositoryImpl
    ): UsageStatsRepository
}
