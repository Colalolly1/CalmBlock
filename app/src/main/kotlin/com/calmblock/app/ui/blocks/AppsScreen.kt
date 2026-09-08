package com.calmblock.app.ui.blocks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.calmblock.app.core.Format
import com.calmblock.app.core.Rules
import com.calmblock.app.core.Status
import com.calmblock.app.data.ItemEntity
import com.calmblock.app.data.Repo
import com.calmblock.app.ui.Dim
import com.calmblock.app.ui.Hairline
import com.calmblock.app.ui.ListRow
import com.calmblock.app.ui.TopBar
import kotlinx.coroutines.launch

/** Every app with a launcher entry, with the ones already in the block marked. */
@Composable
fun AppsScreen(repo: Repo, id: Long, onDone: () -> Unit) {
    val block by repo.block(id).collectAsState(initial = null)
    val items by repo.items(id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Pair<String, String>?>(null) }
    val apps = remember {
        val protected = repo.policy.protectedPackages()
        repo.policy.launchableApps().filterKeys { it !in protected }.toList().sortedBy { it.second.lowercase() }
    }
    val b = block ?: return
    val now = repo.now()
    val status = Rules.status(b, now)
    val canRemove = Rules.canRemove(status)
    val forever = Rules.isForever(b)
    val added = items.filter { it.kind == ItemEntity.APP }.map { it.value }.toSet()

    pending?.let { (pkg, label) ->
        ConfirmAdd(
            what = label,
            forever = forever,
            untilText = (status as? Status.Active)?.until?.let { "in ${Format.remaining(it, now)}" },
            onConfirm = { scope.launch { repo.addApp(id, pkg, label); pending = null } },
            onCancel = { pending = null },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Add apps", onBack = onDone)
        LazyColumn(Modifier.padding(horizontal = Dim.screenPad)) {
            items(apps, key = { it.first }) { (pkg, label) ->
                val isIn = pkg in added
                ListRow(
                    title = label,
                    meta = if (isIn) "In this block" else null,
                    trailing = when {
                        !isIn -> "Add"
                        canRemove -> "Remove"
                        else -> null
                    },
                    onTrailing = {
                        when {
                            isIn -> scope.launch { repo.removeItem(id, ItemEntity.APP, pkg) }
                            // Adding to a block nothing can leave gets its own confirmation.
                            !canRemove -> pending = pkg to label
                            else -> scope.launch { repo.addApp(id, pkg, label) }
                        }
                    },
                )
                Hairline()
            }
        }
    }
}
