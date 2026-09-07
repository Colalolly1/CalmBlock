package com.calmblock.app.ui.blocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.calmblock.app.core.Block
import com.calmblock.app.core.Format
import com.calmblock.app.core.Mode
import com.calmblock.app.core.Rules
import com.calmblock.app.core.Status
import com.calmblock.app.data.ItemEntity
import com.calmblock.app.data.Repo
import com.calmblock.app.ui.CalmCard
import com.calmblock.app.ui.Chip
import com.calmblock.app.ui.Dim
import com.calmblock.app.ui.FieldRow
import com.calmblock.app.ui.Hairline
import com.calmblock.app.ui.ListRow
import com.calmblock.app.ui.Note
import com.calmblock.app.ui.OutlineButton
import com.calmblock.app.ui.PrimaryButton
import com.calmblock.app.ui.SectionLabel
import com.calmblock.app.ui.TopBar
import com.calmblock.app.ui.theme.Type
import java.time.DayOfWeek
import kotlinx.coroutines.launch

private val DURATIONS = listOf(60 to "1h", 120 to "2h", 240 to "4h", 480 to "8h", 1440 to "1 day", 4320 to "3 days", 10080 to "7 days")
private val FREEZES = listOf(0 to "None", 60 to "1h", 120 to "2h", 240 to "4h", 480 to "8h", 720 to "12h")

@Composable
fun BlockScreen(repo: Repo, id: Long, onBack: () -> Unit, onPickApps: () -> Unit) {
    val block by repo.block(id).collectAsState(initial = null)
    val items by repo.items(id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val b = block ?: return
    val now = repo.now()
    val status = Rules.status(b, now)
    val editable = Rules.isEditable(status) && !b.builtIn
    val canAdd = Rules.canAdd(b, status)
    val canRemove = Rules.canRemove(status)

    var name by remember(b.id) { mutableStateOf(b.name) }
    var domainText by remember { mutableStateOf("") }
    var domainError by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf<String?>(null) }
    var chosenMinutes by remember { mutableStateOf(120) }

    Column(Modifier.fillMaxSize()) {
        TopBar(b.name, onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Note(reason(b, status, now), Modifier.padding(top = 10.dp))

            if (!b.builtIn) {
                SectionLabel("NAME")
                FieldRow(
                    label = "", value = name, enabled = editable,
                    onValueChange = { name = it; scope.launch { repo.rename(id, it) } },
                    modifier = Modifier.padding(horizontal = Dim.screenPad),
                )
            }

            when (b.mode) {
                Mode.PERMANENT -> if (!b.builtIn && b.confirmedAt == null) {
                    SectionLabel("PERMANENT")
                    if (confirming != "permanent") {
                        Note("Once confirmed, nothing in this block can be removed, paused, or unblocked. Ever.")
                        PrimaryButton("Block permanently", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad, vertical = 8.dp), enabled = items.isNotEmpty()) { confirming = "permanent" }
                    } else {
                        Note("This cannot be undone. There is no way to unblock these apps or sites, not even by uninstalling.")
                        PrimaryButton("Yes, block permanently", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad, vertical = 8.dp)) {
                            scope.launch { repo.confirmPermanent(id); confirming = null }
                        }
                        OutlineButton("Not now", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad)) { confirming = null }
                    }
                }
                Mode.TIMED, Mode.SCHEDULED -> {
                    SectionLabel("MODE")
                    Row(Modifier.padding(horizontal = Dim.screenPad), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Timed", b.mode == Mode.TIMED, editable) { scope.launch { repo.setMode(id, Mode.TIMED) } }
                        Chip("Scheduled", b.mode == Mode.SCHEDULED, editable) { scope.launch { repo.setMode(id, Mode.SCHEDULED) } }
                    }
                    if (b.mode == Mode.TIMED) {
                        SectionLabel(if (status is Status.Active) "EXTEND BY" else "DURATION")
                        MinutePicker(DURATIONS, chosenMinutes, enabled = true) { chosenMinutes = it; confirming = null }
                        Spacer(Modifier.height(12.dp))
                        val label = Format.minutes(chosenMinutes.toLong())
                        if (status is Status.Active) {
                            if (confirming != "extend") {
                                OutlineButton("Extend by $label", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad), enabled = chosenMinutes > 0) { confirming = "extend" }
                            } else {
                                Note("Add $label to the timer? It cannot be taken back off.")
                                PrimaryButton("Yes, extend by $label", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad, vertical = 8.dp)) {
                                    scope.launch { repo.extendTimed(id, chosenMinutes); confirming = null }
                                }
                                OutlineButton("Not now", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad)) { confirming = null }
                            }
                        } else {
                            if (confirming != "start") {
                                Note("Nothing starts until you confirm on the next step.")
                                PrimaryButton("Start for $label", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad, vertical = 8.dp), enabled = items.isNotEmpty() && chosenMinutes > 0) { confirming = "start" }
                            } else {
                                Note("Start now for $label? Nothing in this block can be changed until it ends. It can be extended, never shortened.")
                                PrimaryButton("Yes, start for $label", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad, vertical = 8.dp)) {
                                    scope.launch { repo.startTimed(id, chosenMinutes); confirming = null }
                                }
                                OutlineButton("Not now", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad)) { confirming = null }
                            }
                        }
                    } else {
                        ScheduleEditor(b, editable) { days, start, end, freeze -> scope.launch { repo.setSchedule(id, days, start, end, freeze) } }
                        Spacer(Modifier.height(12.dp))
                        if (!b.enabled) {
                            if (confirming != "schedule") {
                                Note("The schedule does nothing until you turn it on.")
                                PrimaryButton("Turn on schedule", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad, vertical = 8.dp), enabled = items.isNotEmpty() && b.days != 0) { confirming = "schedule" }
                            } else {
                                Note(scheduleSummary(b) + " From then on it runs on its own, and it can only be changed while it is not active or frozen.")
                                PrimaryButton("Yes, turn on", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad, vertical = 8.dp)) {
                                    scope.launch { repo.setScheduleOn(id, true); confirming = null }
                                }
                                OutlineButton("Not now", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad)) { confirming = null }
                            }
                        } else if (editable) {
                            OutlineButton("Turn off schedule", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad)) { scope.launch { repo.setScheduleOn(id, false) } }
                        }
                    }
                }
            }

            SectionLabel("APPS")
            CalmCard(Modifier.padding(horizontal = Dim.screenPad)) {
                Column {
                    val apps = items.filter { it.kind == ItemEntity.APP }
                    if (apps.isEmpty()) Text("No apps yet", style = Type.meta)
                    apps.forEachIndexed { i, item ->
                        ListRow(
                            title = item.label,
                            trailing = if (canRemove) "Remove" else null,
                            onTrailing = if (canRemove) ({ scope.launch { repo.removeItem(id, item.kind, item.value) } }) else null,
                        )
                        if (i < apps.lastIndex) Hairline()
                    }
                }
            }
            if (canAdd) OutlineButton("Add apps", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad, vertical = 10.dp), onClick = onPickApps)

            SectionLabel("WEBSITES")
            CalmCard(Modifier.padding(horizontal = Dim.screenPad)) {
                Column {
                    val sites = items.filter { it.kind == ItemEntity.DOMAIN }
                    if (sites.isEmpty()) Text("No websites yet", style = Type.meta)
                    sites.forEachIndexed { i, item ->
                        ListRow(
                            title = item.value,
                            trailing = if (canRemove) "Remove" else null,
                            onTrailing = if (canRemove) ({ scope.launch { repo.removeItem(id, item.kind, item.value) } }) else null,
                        )
                        if (i < sites.lastIndex) Hairline()
                    }
                }
            }
            if (canAdd) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.padding(horizontal = Dim.screenPad), verticalAlignment = Alignment.Bottom) {
                    FieldRow(
                        label = if (domainError) "Not a website address" else "Website",
                        value = domainText,
                        onValueChange = { domainText = it; domainError = false },
                        placeholder = "example.com",
                        keyboard = KeyboardType.Uri,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    PrimaryButton("Block") {
                        scope.launch {
                            val ok = repo.addDomain(id, domainText)
                            if (ok) domainText = "" else domainError = true
                        }
                    }
                }
            }

            if (Rules.canDelete(b, status)) {
                Spacer(Modifier.height(28.dp))
                OutlineButton("Delete block", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad)) { scope.launch { repo.delete(id); onBack() } }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Preset chips plus a Custom chip that reveals hours and minutes fields. Value is in minutes. */
@Composable
private fun MinutePicker(presets: List<Pair<Int, String>>, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val isPreset = presets.any { it.first == value }
    var custom by remember { mutableStateOf(!isPreset) }
    var hoursText by remember { mutableStateOf(if (isPreset) "" else (value / 60).toString()) }
    var minutesText by remember { mutableStateOf(if (isPreset) "" else (value % 60).toString()) }

    fun applyCustom() {
        val h = hoursText.toIntOrNull() ?: 0
        val m = minutesText.toIntOrNull() ?: 0
        onChange(h * 60 + m)
    }

    Column(Modifier.padding(horizontal = Dim.screenPad), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (presets + (-1 to "Custom")).chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (v, label) ->
                    val selected = if (v == -1) custom else !custom && value == v
                    Chip(label, selected, enabled) {
                        if (v == -1) { custom = true; applyCustom() } else { custom = false; onChange(v) }
                    }
                }
            }
        }
        if (custom) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField("Hours", hoursText, enabled, Modifier.weight(1f)) { hoursText = it; applyCustom() }
                NumberField("Minutes", minutesText, enabled, Modifier.weight(1f)) { minutesText = it; applyCustom() }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, enabled: Boolean, modifier: Modifier, onChange: (String) -> Unit) {
    FieldRow(label, value, enabled = enabled, keyboard = KeyboardType.Number, placeholder = "0", modifier = modifier, onValueChange = { t ->
        onChange(t.filter { it.isDigit() }.take(3))
    })
}

/** Days, then From and Until as separate hour and minute boxes so the number keyboard is enough. */
@Composable
private fun ScheduleEditor(b: Block, editable: Boolean, onChange: (Int, Int, Int, Int) -> Unit) {
    var startH by remember(b.id) { mutableStateOf((b.startMinute / 60).toString()) }
    var startM by remember(b.id) { mutableStateOf("%02d".format(b.startMinute % 60)) }
    var endH by remember(b.id) { mutableStateOf((b.endMinute / 60).toString()) }
    var endM by remember(b.id) { mutableStateOf("%02d".format(b.endMinute % 60)) }

    fun save() {
        val sh = startH.toIntOrNull()?.coerceIn(0, 23) ?: return
        val sm = startM.toIntOrNull()?.coerceIn(0, 59) ?: return
        val eh = endH.toIntOrNull()?.coerceIn(0, 23) ?: return
        val em = endM.toIntOrNull()?.coerceIn(0, 59) ?: return
        onChange(b.days, sh * 60 + sm, eh * 60 + em, b.freezeMinutes)
    }

    SectionLabel("DAYS")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DayOfWeek.entries.forEach { day ->
            Chip(
                label = day.name.take(2).lowercase().replaceFirstChar { it.uppercase() },
                selected = Rules.dayEnabled(b.days, day),
                enabled = editable,
                modifier = Modifier.weight(1f),
                horizontalPadding = 2.dp,
            ) {
                onChange(Rules.toggleDay(b.days, day), b.startMinute, b.endMinute, b.freezeMinutes)
            }
        }
    }

    SectionLabel("FROM")
    Row(Modifier.padding(horizontal = Dim.screenPad), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NumberField("Hour", startH, editable, Modifier.weight(1f)) { startH = it.take(2); save() }
        NumberField("Minute", startM, editable, Modifier.weight(1f)) { startM = it.take(2); save() }
    }
    SectionLabel("UNTIL")
    Row(Modifier.padding(horizontal = Dim.screenPad), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NumberField("Hour", endH, editable, Modifier.weight(1f)) { endH = it.take(2); save() }
        NumberField("Minute", endM, editable, Modifier.weight(1f)) { endM = it.take(2); save() }
    }
    Note(scheduleSummary(b))

    SectionLabel("FREEZE BEFORE START")
    Note("Once inside the freeze window nothing can be changed until the block ends.")
    Spacer(Modifier.height(6.dp))
    MinutePicker(FREEZES, b.freezeMinutes, editable) { onChange(b.days, b.startMinute, b.endMinute, it) }
}

fun scheduleSummary(b: Block): String {
    val days = DayOfWeek.entries.filter { Rules.dayEnabled(b.days, it) }
    val dayText = when {
        days.isEmpty() -> "No days chosen."
        days.size == 7 -> "Every day"
        else -> days.joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
    }
    if (days.isEmpty()) return dayText
    val crosses = b.endMinute <= b.startMinute
    return "$dayText, ${Format.clock(b.startMinute)} to ${Format.clock(b.endMinute)}${if (crosses) " the next morning" else ""}."
}

private fun reason(b: Block, status: Status, now: Long): String {
    if (b.builtIn) return "Always on. Anything added here is blocked from then on, around the clock, and can never be removed."
    return when (status) {
        is Status.Active -> when {
            status.until == null -> "Permanent. Cannot be changed. Apps and websites can still be added."
            b.mode == Mode.TIMED -> "Running. Ends in ${Format.remaining(status.until, now)}. Cannot be changed."
            else -> "Active until ${Format.moment(status.until, now)}. Cannot be changed."
        }
        is Status.Frozen -> "Frozen until ${Format.moment(status.until, now)}. Cannot be changed."
        Status.Inactive -> when {
            b.mode == Mode.PERMANENT -> "Not yet confirmed. Everything can be changed until it is."
            b.mode == Mode.SCHEDULED && !b.enabled -> "Schedule is off. Everything can be changed."
            else -> "Not active. Everything can be changed."
        }
    }
}
