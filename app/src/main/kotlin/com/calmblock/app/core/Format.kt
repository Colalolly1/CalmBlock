package com.calmblock.app.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Format {
    private val clock = DateTimeFormatter.ofPattern("HH:mm")
    private val dayClock = DateTimeFormatter.ofPattern("EEE HH:mm")
    private val longDate = DateTimeFormatter.ofPattern("EEE d MMM HH:mm")

    fun minutes(total: Long): String {
        val h = total / 60
        val m = total % 60
        return when {
            h == 0L -> "${m}m"
            m == 0L -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }

    fun remaining(untilMillis: Long, now: Long): String {
        val mins = ((untilMillis - now) / 60000L).coerceAtLeast(0)
        if (mins < 60) return "${mins}m"
        val days = mins / (60 * 24)
        if (days >= 2) return "${days}d ${(mins % (60 * 24)) / 60}h"
        return minutes(mins)
    }

    fun clock(minuteOfDay: Int): String = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

    fun parseClock(text: String): Int? {
        val parts = text.trim().split(':')
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    /** "22:00" if today, "Tue 07:00" if within a week, otherwise "Tue 3 Mar 07:00". */
    fun moment(millis: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val t = Instant.ofEpochMilli(millis).atZone(zone)
        val n = Instant.ofEpochMilli(now).atZone(zone)
        return when {
            t.toLocalDate() == n.toLocalDate() -> t.format(clock)
            millis - now < 6 * 24 * Rules.HOUR -> t.format(dayClock)
            else -> t.format(longDate)
        }
    }
}
