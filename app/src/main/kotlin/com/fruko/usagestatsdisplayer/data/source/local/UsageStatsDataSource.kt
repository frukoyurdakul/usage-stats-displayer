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
    /**
     * Build sessions from the raw event stream for a single package.
     *
     * Strategy:
     *  - ACTIVITY_RESUMED  → session opens for this package
     *  - ACTIVITY_PAUSED / ACTIVITY_STOPPED for this package → session closes
     *  - SCREEN_NON_INTERACTIVE (device screen off) → closes any open session for this package,
     *    because the user cannot be actively using the app when the screen is off
     *
     * Sessions shorter than 5 s are discarded as spurious.
     */
    private fun buildSessions(
        events: List<UsageEvents.Event>,
        targetPackage: String,
        rangeEndTime: Long
    ): List<UsageSession> {
        val sessions = mutableListOf<UsageSession>()
        var sessionStart = 0L

        for (event in events) {
            when (event.packageName) {
                targetPackage if event.eventType == UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (sessionStart == 0L) sessionStart = event.timeStamp
                }

                // App went to background (explicit pause/stop)
                targetPackage if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                        event.eventType == UsageEvents.Event.ACTIVITY_STOPPED) -> {
                    if (sessionStart != 0L) {
                        val end = event.timeStamp
                        if (end >= sessionStart + 5_000L) {
                            sessions.add(UsageSession(sessionStart, end))
                        }
                        sessionStart = 0L
                    }
                }
            }
        }

        // App is still in foreground at the end of the queried range
        if (sessionStart != 0L) {
            if (rangeEndTime >= sessionStart + 5_000L) {
                sessions.add(UsageSession(sessionStart, rangeEndTime))
            }
        }

        return sessions
    }

    /**
     * Collect all relevant raw events in chronological order for the given time window.
     *
     * Events collected:
     *  - ACTIVITY_RESUMED / ACTIVITY_PAUSED / ACTIVITY_STOPPED   (all packages)
     *  - SCREEN_NON_INTERACTIVE                                   (system, no package name)
     */
    private fun collectEvents(startTime: Long, endTime: Long): List<UsageEvents.Event> {
        val raw = usageStatsManager.queryEvents(startTime, endTime)
        val result = mutableListOf<UsageEvents.Event>()
        while (raw.hasNextEvent()) {
            val event = UsageEvents.Event()
            raw.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> result.add(event)
            }
        }
        return result
    }

    suspend fun getUsageStats(
        timeFrame: TimeFrame,
        includeSystemApps: Boolean
    ): List<UsageStatInfo> = withContext(Dispatchers.IO) {
        val endTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = endTime
            add(Calendar.DAY_OF_YEAR, -timeFrame.days)
        }
        val startTime = calendar.timeInMillis

        val events = collectEvents(startTime, endTime)

        // Discover every package that had at least one ACTIVITY_RESUMED event
        val packages = events
            .filter { it.eventType == UsageEvents.Event.ACTIVITY_RESUMED }
            .map { it.packageName }
            .toSet()

        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        buildList {
            for (packageName in packages) {
                if (packageManager.getLaunchIntentForPackage(packageName) == null) continue

                val appInfo = try {
                    packageManager.getApplicationInfo(packageName, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    continue
                }

                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!includeSystemApps && isSystemApp) continue

                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val sessions = buildSessions(events, packageName, endTime)
                if (sessions.isEmpty()) continue

                val totalTime = sessions.sumOf { it.durationMillis }

                // Group by calendar day to find the peak day
                val byDay = sessions.groupBy { dayFormat.format(it.startTime) }
                var maxUsageInDay = 0L
                var maxUsageDayTimestamp = 0L
                for ((_, daySessions) in byDay) {
                    val dayTotal = daySessions.sumOf { it.durationMillis }
                    if (dayTotal > maxUsageInDay) {
                        maxUsageInDay = dayTotal
                        maxUsageDayTimestamp = daySessions.first().startTime
                    }
                }

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

    suspend fun getAppSessions(
        packageName: String,
        timeFrame: TimeFrame
    ): List<DailyUsage> = withContext(Dispatchers.IO) {
        val endTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = endTime
            add(Calendar.DAY_OF_YEAR, -timeFrame.days)
        }
        val startTime = calendar.timeInMillis

        val events = collectEvents(startTime, endTime)
        val sessions = buildSessions(events, packageName, endTime)

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
