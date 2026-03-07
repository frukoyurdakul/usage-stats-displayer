package com.fruko.usagestatsdisplayer.data.source.local

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.fruko.usagestatsdisplayer.domain.model.DailyUsage
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame
import com.fruko.usagestatsdisplayer.domain.model.UsageSession
import com.fruko.usagestatsdisplayer.domain.model.UsageStatInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class UsageStatsDataSource @Inject constructor(
    private val usageStatsManager: UsageStatsManager,
    private val packageManager: PackageManager,
) {
    suspend fun getUsageStats(
        timeFrame: TimeFrame,
        includeSystemApps: Boolean
    ): List<UsageStatInfo> = withContext(Dispatchers.IO) {
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

    suspend fun getAppSessions(
        packageName: String,
        timeFrame: TimeFrame
    ): List<DailyUsage> = withContext(Dispatchers.IO) {
        val endTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = endTime
        calendar.add(Calendar.DAY_OF_YEAR, -timeFrame.days)
        val startTime = calendar.timeInMillis

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val usageEvents = mutableListOf<UsageEvents.Event>()

        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)
            if (event.packageName == packageName &&
                (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                 event.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                 event.eventType == UsageEvents.Event.ACTIVITY_STOPPED)
            ) {
                usageEvents.add(event)
            }
        }

        // Build sessions from paired RESUMED/PAUSED events
        val sessions = mutableListOf<UsageSession>()
        var currentSessionStart = 0L

        for (event in usageEvents) {
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (currentSessionStart == 0L) {
                        currentSessionStart = event.timeStamp
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (currentSessionStart != 0L && event.timeStamp > currentSessionStart) {
                        if (event.timeStamp >= currentSessionStart + 5000) {
                            sessions.add(UsageSession(currentSessionStart, event.timeStamp))
                        }
                        currentSessionStart = 0L
                    }
                }
            }
        }
        // If still in foreground at end of range, cap the session
        if (currentSessionStart != 0L) {
            sessions.add(UsageSession(currentSessionStart, endTime))
        }

        // Group sessions by day label; sessions spanning midnight get a combined label
        val dateFormat = SimpleDateFormat("MMMM d", Locale.getDefault())
        sessions
            .groupBy { session ->
                val startDay = dateFormat.format(session.startTime)
                val endDay = dateFormat.format(session.endTime)
                if (startDay == endDay) startDay else "$startDay - $endDay"
            }
            .map { (dayLabel, daySessions) ->
                DailyUsage(dayLabel, daySessions.sortedByDescending { it.startTime })
            }
            .sortedByDescending { it.sessions.firstOrNull()?.startTime ?: 0L }
    }
}
