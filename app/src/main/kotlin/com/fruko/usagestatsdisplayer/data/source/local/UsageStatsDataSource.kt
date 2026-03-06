package com.fruko.usagestatsdisplayer.data.source.local

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame
import com.fruko.usagestatsdisplayer.domain.model.UsageStatInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

class UsageStatsDataSource @Inject constructor(
    private val usageStatsManager: UsageStatsManager,
    @ApplicationContext private val context: Context
) {
    suspend fun getUsageStats(
        timeFrame: TimeFrame,
        includeSystemApps: Boolean
    ): List<UsageStatInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val endTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = endTime
        calendar.add(Calendar.DAY_OF_YEAR, -timeFrame.days)
        val startTime = calendar.timeInMillis

        // Get exact total aggregations
        val aggregatedStats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)

        // Retrieve daily stats to get max usage per day
        val dailyStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (aggregatedStats.isNullOrEmpty()) {
            return@withContext emptyList()
        }

        val dailyGroups = dailyStatsList?.groupBy { it.packageName } ?: emptyMap()

        buildList {
            for (stat in aggregatedStats.values) {
                val packageName = stat.packageName

                // Filter out apps with no launcher icons
                if (packageManager.getLaunchIntentForPackage(packageName) == null) continue

                val appInfo = try {
                    packageManager.getApplicationInfo(packageName, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    continue
                }

                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!includeSystemApps && isSystemApp) {
                    continue
                }

                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val totalTime = stat.totalTimeInForeground
                
                var maxUsageInDay = 0L
                var maxUsageDayTimestamp = 0L

                val packageDailyStats = dailyGroups[packageName]
                if (packageDailyStats != null) {
                    for (dailyStat in packageDailyStats) {
                        // Ensure this is a Daily bucket (roughly <= 2 days)
                        // Otherwise, Android has rolled older days into weekly/monthly buckets
                        val bucketDuration = dailyStat.lastTimeStamp - dailyStat.firstTimeStamp
                        if (bucketDuration <= 1000L * 60 * 60 * 24 * 2) {
                            val dayUsage = dailyStat.totalTimeInForeground
                            if (dayUsage > maxUsageInDay) {
                                maxUsageInDay = dayUsage
                                maxUsageDayTimestamp = dailyStat.firstTimeStamp
                            }
                        }
                    }
                }

                if (totalTime > 0) {
                    val averageTime = totalTime / timeFrame.days
                    add(
                        UsageStatInfo(
                            packageName = packageName,
                            appName = appName,
                            totalTimeInForeground = totalTime,
                            averageTime = averageTime,
                            maxUsageInDay = maxUsageInDay,
                            maxUsageDayTimestamp = maxUsageDayTimestamp
                        )
                    )
                }
            }
        }
    }
}
