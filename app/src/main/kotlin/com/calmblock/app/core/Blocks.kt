package com.calmblock.app.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

enum class Mode { PERMANENT, TIMED, SCHEDULED }

/**
 * A block as the rules see it. Whether it is active is never stored; it is
 * derived from these fields and the current time by [Rules].
 */
data class Block(
    val id: Long,
    val name: String,
    val mode: Mode,
    val builtIn: Boolean,
    val confirmedAt: Long?,
    val timedEndAt: Long?,
    val days: Int,
    val startMinute: Int,
    val endMinute: Int,
    val freezeMinutes: Int,
    val enabled: Boolean,
    val createdAt: Long,
)

sealed interface Status {
    /** Blocking now. [until] is null when it never ends. */
    data class Active(val until: Long?) : Status

    /** Not blocking yet, but nothing may be changed until [until]. */
    data class Frozen(val until: Long) : Status

    data object Inactive : Status
}

/** A single scheduled window, in epoch millis. */
data class Window(val start: Long, val end: Long)

object Rules {

    fun status(block: Block, now: Long, zone: ZoneId = ZoneId.systemDefault()): Status {
        if (block.builtIn) return Status.Active(null)
        return when (block.mode) {
            Mode.PERMANENT -> if (block.confirmedAt != null) Status.Active(null) else Status.Inactive
            Mode.TIMED -> {
                val end = block.timedEndAt
                if (end != null && end > now) Status.Active(end) else Status.Inactive
            }
            Mode.SCHEDULED -> if (block.enabled) scheduledStatus(block, now, zone) else Status.Inactive
        }
    }

    /** The next moment at which [status] can change, or null if it never will. */
    fun nextTransition(block: Block, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (block.builtIn) return null
        return when (block.mode) {
            Mode.PERMANENT -> null
            Mode.TIMED -> block.timedEndAt?.takeIf { it > now }
            Mode.SCHEDULED -> {
                if (!block.enabled) return null
                val windows = windows(block, now, zone)
                val current = windows.firstOrNull { it.start <= now && now < it.end }
                if (current != null) return current.end
                val next = windows.firstOrNull { it.start > now } ?: return null
                val freezeStart = next.start - block.freezeMinutes * MINUTE
                if (now < freezeStart) freezeStart else next.start
            }
        }
    }

    fun isEditable(status: Status): Boolean = status == Status.Inactive

    /** Additions are allowed to a permanent block even once confirmed. Removals never are. */
    fun canAdd(block: Block, status: Status): Boolean =
        status == Status.Inactive || block.builtIn || block.mode == Mode.PERMANENT

    fun canRemove(status: Status): Boolean = status == Status.Inactive

    fun canDelete(block: Block, status: Status): Boolean =
        !block.builtIn && block.confirmedAt == null && status == Status.Inactive

    private fun scheduledStatus(block: Block, now: Long, zone: ZoneId): Status {
        val windows = windows(block, now, zone)
        windows.firstOrNull { it.start <= now && now < it.end }?.let { return Status.Active(it.end) }
        val next = windows.firstOrNull { it.start > now } ?: return Status.Inactive
        val freezeStart = next.start - block.freezeMinutes * MINUTE
        return if (now >= freezeStart) Status.Frozen(next.end) else Status.Inactive
    }

    /** Windows whose start day is enabled, from yesterday through a week ahead, sorted. */
    fun windows(block: Block, now: Long, zone: ZoneId): List<Window> {
        if (block.days == 0) return emptyList()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val out = ArrayList<Window>()
        for (offset in -1L..8L) {
            val day = today.plusDays(offset)
            if (!dayEnabled(block.days, day.dayOfWeek)) continue
            val start = at(day, block.startMinute, zone)
            val endDay = if (block.endMinute > block.startMinute) day else day.plusDays(1)
            val end = at(endDay, block.endMinute, zone)
            if (end > start) out.add(Window(start, end))
        }
        return out.sortedBy { it.start }
    }

    fun dayEnabled(days: Int, day: DayOfWeek): Boolean = days and (1 shl (day.value - 1)) != 0

    fun toggleDay(days: Int, day: DayOfWeek): Int = days xor (1 shl (day.value - 1))

    private fun at(day: LocalDate, minuteOfDay: Int, zone: ZoneId): Long =
        ZonedDateTime.of(day.atStartOfDay(), zone).plusMinutes(minuteOfDay.toLong()).toInstant().toEpochMilli()

    const val MINUTE = 60L * 1000L
    const val HOUR = 60L * MINUTE
}
