package com.calmblock.app.ui.blocks

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calmblock.app.core.Block
import com.calmblock.app.core.Format
import com.calmblock.app.core.Mode
import com.calmblock.app.core.Rules
import com.calmblock.app.core.Status
import com.calmblock.app.data.ItemEntity
import com.calmblock.app.data.Repo
import com.calmblock.app.ui.CalmCard
import com.calmblock.app.ui.Dim
import com.calmblock.app.ui.Hairline
import com.calmblock.app.ui.Note
import com.calmblock.app.ui.OutlineButton
import com.calmblock.app.ui.PrimaryButton
import com.calmblock.app.ui.SectionLabel
import com.calmblock.app.ui.TopBar
import com.calmblock.app.ui.theme.Type
import java.time.ZoneId

/** Timed and scheduled blocks first. Permanent ones live below a rule of their own. */
@Composable
fun BlocksScreen(repo: Repo, onOpen: (Long) -> Unit, onNew: (Mode) -> Unit) {
    val blocks by repo.blocks.collectAsState(initial = emptyList())
    val items by repo.allItems.collectAsState(initial = emptyList())
    val now = repo.now()
    val ordinary = blocks.filter { it.mode != Mode.PERMANENT }
    val permanent = blocks.filter { it.mode == Mode.PERMANENT }

    Column(Modifier.fillMaxSize()) {
        TopBar("Blocks")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(6.dp))
            if (ordinary.isEmpty()) Note("No timed or scheduled blocks yet.")
            ordinary.forEach { BlockCard(it, items.filter { i -> i.blockId == it.id }, now, onOpen) }
            PrimaryButton("New block", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad, vertical = 12.dp)) { onNew(Mode.TIMED) }

            Spacer(Modifier.height(20.dp))
            Hairline()
            SectionLabel("PERMANENT")
            Note("Blocks here can never be undone. Nothing above can become one by accident.")
            Spacer(Modifier.height(6.dp))
            permanent.forEach { BlockCard(it, items.filter { i -> i.blockId == it.id }, now, onOpen) }
            OutlineButton("New permanent block", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad, vertical = 12.dp)) { onNew(Mode.PERMANENT) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BlockCard(block: Block, items: List<ItemEntity>, now: Long, onOpen: (Long) -> Unit) {
    CalmCard(Modifier.padding(horizontal = Dim.screenPad, vertical = 6.dp), onClick = { onOpen(block.id) }) {
        Column {
            Text(block.name, style = Type.cardTitle)
            Spacer(Modifier.height(4.dp))
            Text(statusLine(block, now) + " · " + countLine(items), style = Type.meta)
        }
    }
}

fun countLine(items: List<ItemEntity>): String {
    val apps = items.count { it.kind == ItemEntity.APP }
    val sites = items.count { it.kind == ItemEntity.DOMAIN }
    if (items.isEmpty()) return "Empty"
    val parts = ArrayList<String>()
    if (apps > 0) parts += if (apps == 1) "1 app" else "$apps apps"
    if (sites > 0) parts += if (sites == 1) "1 site" else "$sites sites"
    return parts.joinToString(", ")
}

fun statusLine(block: Block, now: Long): String {
    if (block.builtIn) return "Always on"
    return when (val s = Rules.status(block, now)) {
        is Status.Active -> when {
            s.until == null -> "Permanent"
            block.mode == Mode.TIMED -> "Ends in ${Format.remaining(s.until, now)}"
            else -> "Active until ${Format.moment(s.until, now)}"
        }
        is Status.Frozen -> "Starts ${Format.moment(nextStart(block, now), now)} · frozen"
        Status.Inactive -> when (block.mode) {
            Mode.PERMANENT -> "Not yet confirmed"
            Mode.TIMED -> "Not running"
            Mode.SCHEDULED -> {
                if (!block.enabled) return "Schedule off"
                val next = Rules.windows(block, now, ZoneId.systemDefault()).firstOrNull { it.start > now }
                if (next == null) "No days chosen" else "Starts ${Format.moment(next.start, now)}"
            }
        }
    }
}

fun nextStart(block: Block, now: Long): Long =
    Rules.windows(block, now, ZoneId.systemDefault()).firstOrNull { it.start > now }?.start ?: now
