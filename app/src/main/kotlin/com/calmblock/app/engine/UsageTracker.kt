package com.calmblock.app.engine

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.calmblock.app.admin.PolicyEngine
import com.calmblock.app.data.BlockDao
import com.calmblock.app.data.UsageDayEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Reads the system's own usage events and keeps a per-day table of minutes. */
class UsageTracker(private val context: Context, private val policy: PolicyEngine) {

    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    suspend fun rollup(dao: BlockDao, now: Long) {
        if (!Access.usageAccessOn(context)) return
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val rows = ArrayList<UsageDayEntity>()
        for (day in listOf(today.minusDays(1), today)) {
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = minOf(day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), now)
            foreground(start, end).forEach { (pkg, millis) ->
                val minutes = (millis / 60000L).toInt()
                if (minutes > 0) rows += UsageDayEntity(day.toString(), pkg, policy.label(pkg), minutes)
            }
        }
        if (rows.isNotEmpty()) dao.upsertUsage(rows)
    }

    /** Foreground milliseconds per package between [start] and [end]. */
    fun foreground(start: Long, end: Long): Map<String, Long> {
        val events = usm.queryEvents(start, end)
        val open = HashMap<String, Long>()
        val total = HashMap<String, Long>()
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val pkg = e.packageName ?: continue
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> open.putIfAbsent(pkg, e.timeStamp)
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val since = open.remove(pkg) ?: continue
                    total[pkg] = (total[pkg] ?: 0L) + (e.timeStamp - since).coerceAtLeast(0)
                }
            }
        }
        open.forEach { (pkg, since) -> total[pkg] = (total[pkg] ?: 0L) + (end - since).coerceAtLeast(0) }
        total.remove(context.packageName)
        return total
    }

    companion object {
        fun date(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
}
