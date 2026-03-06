package com.fruko.usagestatsdisplayer.data.source.local

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
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

    suspend fun getUsageStats(timeFrame: TimeFrame, includeSystemApps: Boolean): List<UsageStatInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val endTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = endTime
        calendar.add(Calendar.DAY_OF_YEAR, -timeFrame.days)
        val startTime = calendar.timeInMillis

        // Retrieve daily stats to get max usage per day
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (stats.isNullOrEmpty()) {
            return@withContext emptyList()
        }

        // Filter out apps with no launcher icons and system apps if not included
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfoList = packageManager.queryIntentActivities(launcherIntent, 0)
        
        val validPackages = resolveInfoList.mapNotNull { it.activityInfo?.packageName }.toSet()
        
        // Group by package name
        val groupedStats = stats.groupBy { it.packageName }
        
        val usages = mutableListOf<UsageStatInfo>()
        
        for ((packageName, packageStats) in groupedStats) {
            if (!validPackages.contains(packageName)) continue

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
            
            var totalTime = 0L
            var maxUsageInDay = 0L
            var maxUsageDayTimestamp = 0L
            
            for (stat in packageStats) {
                val dayUsage = stat.totalTimeInForeground
                totalTime += dayUsage
                if (dayUsage > maxUsageInDay) {
                    maxUsageInDay = dayUsage
                    maxUsageDayTimestamp = stat.firstTimeStamp
                }
            }
            
            if (totalTime > 0) {
                val averageTime = totalTime / timeFrame.days
                usages.add(
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
        
        usages
    }
}
