package com.calmblock.app

import com.calmblock.app.core.Block
import com.calmblock.app.core.Domains
import com.calmblock.app.core.Mode
import com.calmblock.app.core.Rules
import com.calmblock.app.core.Status
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {

    private val zone = ZoneId.of("Europe/London")
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private fun block(mode: Mode, confirmedAt: Long? = null, timedEndAt: Long? = null, days: Int = 0, start: Int = 21 * 60, end: Int = 7 * 60, freeze: Int = 0) =
        Block(1, "b", mode, false, confirmedAt, timedEndAt, days, start, end, freeze, true, 0L)

    private val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        .fold(0) { acc, d -> Rules.toggleDay(acc, d) }

    @Test fun permanentIsInactiveUntilConfirmedThenForever() {
        assertEquals(Status.Inactive, Rules.status(block(Mode.PERMANENT), 1000L, zone))
        assertEquals(Status.Active(null), Rules.status(block(Mode.PERMANENT, confirmedAt = 5L), Long.MAX_VALUE / 2, zone))
        assertNull(Rules.nextTransition(block(Mode.PERMANENT, confirmedAt = 5L), 1000L, zone))
    }

    @Test fun timedEndsExactlyAtItsEnd() {
        val end = 10_000L
        val b = block(Mode.TIMED, timedEndAt = end)
        assertEquals(Status.Active(end), Rules.status(b, end - 1, zone))
        assertEquals(Status.Inactive, Rules.status(b, end, zone))
        assertEquals(end, Rules.nextTransition(b, end - 1, zone))
    }

    @Test fun scheduleCrossesMidnight() {
        // Tuesday 2026-09-08, window 21:00 to 07:00 on weekdays.
        val b = block(Mode.SCHEDULED, days = weekdays)
        assertEquals(Status.Inactive, Rules.status(b, at(2026, 9, 8, 12), zone))
        assertTrue(Rules.status(b, at(2026, 9, 8, 22), zone) is Status.Active)
        assertTrue(Rules.status(b, at(2026, 9, 9, 3), zone) is Status.Active)
        assertEquals(Status.Inactive, Rules.status(b, at(2026, 9, 9, 8), zone))
    }

    @Test fun fridayWindowRunsIntoSaturdayMorning() {
        val b = block(Mode.SCHEDULED, days = weekdays)
        assertTrue(Rules.status(b, at(2026, 9, 12, 2), zone) is Status.Active)
        assertEquals(Status.Inactive, Rules.status(b, at(2026, 9, 12, 23), zone))
    }

    @Test fun freezeWindowLocksEditsBeforeStart() {
        val b = block(Mode.SCHEDULED, days = weekdays, freeze = 120)
        assertEquals(Status.Inactive, Rules.status(b, at(2026, 9, 8, 18, 59), zone))
        val frozen = Rules.status(b, at(2026, 9, 8, 19), zone)
        assertTrue(frozen is Status.Frozen)
        assertEquals(at(2026, 9, 9, 7), (frozen as Status.Frozen).until)
        assertEquals(at(2026, 9, 8, 19), Rules.nextTransition(b, at(2026, 9, 8, 12), zone))
        assertEquals(at(2026, 9, 8, 21), Rules.nextTransition(b, at(2026, 9, 8, 20), zone))
    }

    @Test fun scheduleOffIsNeverActive() {
        val b = block(Mode.SCHEDULED, days = weekdays).copy(enabled = false)
        assertEquals(Status.Inactive, Rules.status(b, at(2026, 9, 8, 22), zone))
        assertNull(Rules.nextTransition(b, at(2026, 9, 8, 22), zone))
    }

    @Test fun noDaysMeansNeverActive() {
        val b = block(Mode.SCHEDULED, days = 0)
        assertEquals(Status.Inactive, Rules.status(b, at(2026, 9, 8, 22), zone))
        assertNull(Rules.nextTransition(b, at(2026, 9, 8, 22), zone))
    }

    @Test fun permissionsFollowStatus() {
        val confirmed = block(Mode.PERMANENT, confirmedAt = 1L)
        assertTrue(Rules.canAdd(confirmed, Status.Active(null)))
        assertFalse(Rules.canRemove(Status.Active(null)))
        assertFalse(Rules.canDelete(confirmed, Status.Active(null)))
        val draft = block(Mode.TIMED)
        assertTrue(Rules.canDelete(draft, Status.Inactive))
        assertFalse(Rules.canAdd(draft, Status.Active(5L)))
        assertFalse(Rules.canRemove(Status.Frozen(5L)))
    }

    @Test fun domainsNormaliseAndMatch() {
        assertEquals("example.com", Domains.normalise("https://www.Example.com/path?q=1"))
        assertEquals("news.ycombinator.com", Domains.normalise("news.ycombinator.com"))
        assertNull(Domains.normalise("not a domain"))
        assertNull(Domains.normalise("localhost"))
        assertTrue(Domains.isBlocked("m.youtube.com", setOf("youtube.com")))
        assertTrue(Domains.isBlocked("www.youtube.com", setOf("youtube.com")))
        assertFalse(Domains.isBlocked("notyoutube.com", setOf("youtube.com")))
    }
}
