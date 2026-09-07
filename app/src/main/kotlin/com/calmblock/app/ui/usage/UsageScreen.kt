package com.calmblock.app.ui.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.calmblock.app.core.Format
import com.calmblock.app.data.BlockedEventEntity
import com.calmblock.app.data.Export
import com.calmblock.app.data.Repo
import com.calmblock.app.data.UsageDayEntity
import com.calmblock.app.engine.Access
import com.calmblock.app.ui.CalmCard
import com.calmblock.app.ui.Chip
import com.calmblock.app.ui.Dim
import com.calmblock.app.ui.Hairline
import com.calmblock.app.ui.ListRow
import com.calmblock.app.ui.Note
import com.calmblock.app.ui.OutlineButton
import com.calmblock.app.ui.SectionLabel
import com.calmblock.app.ui.TopBar
import com.calmblock.app.ui.theme.Type
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Span { TODAY, WEEK, WEEKS }
private val DAY_NAME: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE")
private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

@Composable
fun UsageScreen(repo: Repo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var span by remember { mutableStateOf(Span.TODAY) }
    var message by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now()
    val weekStart = today.with(DayOfWeek.MONDAY)
    val from = today.minusWeeks(13).with(DayOfWeek.MONDAY)
    val rows by repo.usage(from.toString(), today.toString()).collectAsState(initial = emptyList())
    val events by repo.events(from.toString(), today.toString()).collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize()) {
        TopBar("Usage")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(Dim.screenPad), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Today", span == Span.TODAY) { span = Span.TODAY }
                Chip("This week", span == Span.WEEK) { span = Span.WEEK }
                Chip("Weeks", span == Span.WEEKS) { span = Span.WEEKS }
            }
            if (!Access.usageAccessOn(context)) Note("Usage access is not granted. See Settings.")

            when (span) {
                Span.TODAY -> {
                    val todayRows = rows.filter { it.date == today.toString() }
                    val yesterday = rows.filter { it.date == today.minusDays(1).toString() }
                    Summary(
                        "Today" to Format.minutes(total(todayRows)),
                        "Yesterday" to Format.minutes(total(yesterday)),
                        "Stopped by a block" to times(events.count { it.date == today.toString() }),
                    )
                    AppTotals(todayRows, yesterday)
                    Blocked(events.filter { it.date == today.toString() })
                }
                Span.WEEK -> {
                    val week = rows.filter { LocalDate.parse(it.date) >= weekStart }
                    val lastWeek = rows.filter { val d = LocalDate.parse(it.date); d >= weekStart.minusWeeks(1) && d < weekStart }
                    val weekEvents = events.filter { LocalDate.parse(it.date) >= weekStart }
                    WeekBody(week, lastWeek, weekEvents, weekStart, today)
                }
                Span.WEEKS -> Weeks(rows, events, today)
            }

            Spacer(Modifier.height(24.dp))
            OutlineButton("Export CSV", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad)) {
                scope.launch {
                    message = runCatching {
                        val all = repo.allUsage()
                        withContext(Dispatchers.IO) { Export.usageCsv(context, all) }
                    }.fold({ "Saved to Documents/CalmBlock/$it" }, { "Export failed: ${it.message}" })
                }
            }
            message?.let { Note(it, Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun total(rows: List<UsageDayEntity>): Long = rows.sumOf { it.minutes.toLong() }
private fun times(n: Int): String = if (n == 1) "once" else "$n times"

private fun change(now: Long, before: Long?): String = when {
    before == null || before == 0L -> ""
    now >= before -> " · up ${Format.minutes(now - before)}"
    else -> " · down ${Format.minutes(before - now)}"
}

@Composable
private fun Summary(vararg lines: Pair<String, String>) {
    SectionLabel("SUMMARY")
    CalmCard(Modifier.padding(horizontal = Dim.screenPad)) {
        Column {
            lines.forEachIndexed { i, (label, value) ->
                ListRow(label, trailing = value)
                if (i < lines.lastIndex) Hairline()
            }
        }
    }
}

@Composable
private fun WeekBody(week: List<UsageDayEntity>, previous: List<UsageDayEntity>, weekEvents: List<BlockedEventEntity>, weekStart: LocalDate, today: LocalDate) {
    val byDay = week.groupBy { it.date }.mapValues { e -> total(e.value) }
    val daysSoFar = (weekStart.toEpochDay()..today.toEpochDay()).count()
    val busiest = byDay.maxByOrNull { it.value }
    Summary(
        "This week" to Format.minutes(total(week)) + change(total(week), total(previous)),
        "Per day" to Format.minutes(total(week) / daysSoFar.coerceAtLeast(1)),
        "Busiest day" to (busiest?.let { LocalDate.parse(it.key).format(DAY_NAME) + " " + Format.minutes(it.value) } ?: "None yet"),
        "Stopped by a block" to times(weekEvents.size),
    )
    SectionLabel("DAYS")
    CalmCard(Modifier.padding(horizontal = Dim.screenPad)) {
        Column {
            var d = weekStart
            var first = true
            while (!d.isAfter(today)) {
                if (!first) Hairline()
                first = false
                ListRow(d.format(DAY_NAME), trailing = Format.minutes(byDay[d.toString()] ?: 0L))
                d = d.plusDays(1)
            }
        }
    }
    AppTotals(week, previous)
    Blocked(weekEvents)
}

@Composable
private fun AppTotals(rows: List<UsageDayEntity>, previous: List<UsageDayEntity>) {
    val before = previous.groupBy { it.pkg }.mapValues { e -> total(e.value) }
    val totals = rows.groupBy { it.pkg }
        .map { (pkg, list) -> Triple(list.first().label, total(list), before[pkg]) }
        .sortedByDescending { it.second }
    SectionLabel("APPS")
    CalmCard(Modifier.padding(horizontal = Dim.screenPad)) {
        Column {
            if (totals.isEmpty()) Text("Nothing recorded yet", style = Type.meta)
            totals.forEachIndexed { i, (label, minutes, prev) ->
                ListRow(label, meta = change(minutes, prev).removePrefix(" · ").ifEmpty { null }, trailing = Format.minutes(minutes))
                if (i < totals.lastIndex) Hairline()
            }
        }
    }
}

@Composable
private fun Blocked(events: List<BlockedEventEntity>) {
    if (events.isEmpty()) return
    val counts = events.groupBy { it.label }.map { it.key to it.value.size }.sortedByDescending { it.second }
    SectionLabel("STOPPED BY A BLOCK")
    CalmCard(Modifier.padding(horizontal = Dim.screenPad)) {
        Column {
            counts.forEachIndexed { i, (label, n) ->
                ListRow(label, trailing = times(n))
                if (i < counts.lastIndex) Hairline()
            }
        }
    }
}

@Composable
private fun Weeks(rows: List<UsageDayEntity>, events: List<BlockedEventEntity>, today: LocalDate) {
    val byWeek = rows.groupBy { LocalDate.parse(it.date).with(DayOfWeek.MONDAY) }
    val eventsByWeek = events.groupBy { LocalDate.parse(it.date).with(DayOfWeek.MONDAY) }
    val weeks = byWeek.keys.sortedDescending()
    var open by remember { mutableStateOf<LocalDate?>(null) }
    SectionLabel("WEEKLY REPORTS")
    if (weeks.isEmpty()) Note("Nothing recorded yet.")
    weeks.forEach { start ->
        val list = byWeek[start].orEmpty()
        val previous = byWeek[start.minusWeeks(1)].orEmpty()
        val sum = total(list)
        val days = list.map { it.date }.distinct().size.coerceAtLeast(1)
        val thisWeek = start == today.with(DayOfWeek.MONDAY)
        val label = if (thisWeek) "This week so far" else "Week of " + start.format(SHORT_DATE)
        CalmCard(Modifier.padding(horizontal = Dim.screenPad, vertical = 6.dp), onClick = { open = if (open == start) null else start }) {
            Column {
                Text(label, style = Type.cardTitle)
                Spacer(Modifier.height(4.dp))
                Text(Format.minutes(sum) + change(sum, total(previous)) + " · " + Format.minutes(sum / days) + " a day", style = Type.meta)
                if (open == start) {
                    Spacer(Modifier.height(8.dp))
                    Hairline()
                    val byDay = list.groupBy { it.date }.mapValues { e -> total(e.value) }
                    byDay.keys.sorted().forEach { d -> ListRow(LocalDate.parse(d).format(DAY_NAME), trailing = Format.minutes(byDay[d] ?: 0L)) }
                    Hairline()
                    val before = previous.groupBy { it.pkg }.mapValues { e -> total(e.value) }
                    list.groupBy { it.pkg }
                        .map { (pkg, l) -> Triple(l.first().label, total(l), before[pkg]) }
                        .sortedByDescending { it.second }
                        .take(10)
                        .forEach { (name, minutes, prev) -> ListRow(name, meta = change(minutes, prev).removePrefix(" · ").ifEmpty { null }, trailing = Format.minutes(minutes)) }
                    val stops = eventsByWeek[start].orEmpty()
                    if (stops.isNotEmpty()) {
                        Hairline()
                        stops.groupBy { it.label }.map { it.key to it.value.size }.sortedByDescending { it.second }
                            .forEach { (name, n) -> ListRow("Stopped: $name", trailing = times(n)) }
                    }
                }
            }
        }
    }
}
