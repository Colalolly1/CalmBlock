package com.calmblock.app.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.calmblock.app.data.ItemEntity
import com.calmblock.app.data.Repo
import com.calmblock.app.engine.Access
import com.calmblock.app.ui.CalmCard
import com.calmblock.app.ui.Dim
import com.calmblock.app.ui.Hairline
import com.calmblock.app.ui.ListRow
import com.calmblock.app.ui.Note
import com.calmblock.app.ui.OutlineButton
import com.calmblock.app.ui.SectionLabel
import com.calmblock.app.ui.TopBar
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(repo: Repo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val items by repo.allItems.collectAsState(initial = emptyList())
    var browser by remember { mutableStateOf(repo.browser) }
    var tick by remember { mutableStateOf(0) }
    val anyDomain = items.any { it.kind == ItemEntity.DOMAIN }
    val adminActive = remember(tick) { repo.policy.isAdminActive }
    val accessibilityOn = remember(tick) { Access.accessibilityOn(context) }
    val usageOn = remember(tick) { Access.usageAccessOn(context) }
    val browsers = remember(tick) {
        val protected = repo.policy.protectedPackages()
        (repo.policy.webCapablePackages() - protected).associateWith { repo.policy.label(it) }.toList().sortedBy { it.second }
    }

    fun open(intent: Intent) = runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Settings")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SectionLabel("SETUP")
            CalmCard(Modifier.padding(horizontal = Dim.screenPad)) {
                Column {
                    ListRow("Uninstall protection", meta = "Device admin", trailing = if (adminActive) "On" else "Off")
                    Hairline()
                    ListRow("Blocking service", meta = "Accessibility", trailing = if (accessibilityOn) "On" else "Off")
                    Hairline()
                    ListRow("Usage access", trailing = if (usageOn) "On" else "Off")
                    Hairline()
                    ListRow("Android", trailing = Build.VERSION.RELEASE)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (!adminActive) {
                Note("Without this the app can be uninstalled from the phone.")
                OutlineButton("Turn on uninstall protection", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad)) {
                    open(
                        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, repo.policy.admin)
                            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Stops CalmBlock being uninstalled from the phone."),
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            if (!accessibilityOn) {
                Note("Without this nothing is blocked. Find CalmBlock in the list and turn it on.")
                OutlineButton("Turn on blocking", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad)) {
                    open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                Spacer(Modifier.height(10.dp))
            }
            if (!usageOn) {
                Note("Without this the Usage tab stays empty.")
                OutlineButton("Turn on usage access", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad)) {
                    open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                Spacer(Modifier.height(10.dp))
            }

            SectionLabel("BROWSER")
            Note(
                if (browser == null) "Choose the browser that website blocks apply to. Any other app that opens web pages will be blocked."
                else if (anyDomain) "Fixed while any website is blocked."
                else "Can be changed while no websites are blocked.",
            )
            CalmCard(Modifier.padding(horizontal = Dim.screenPad)) {
                Column {
                    if (browsers.isEmpty()) ListRow("No browser installed")
                    browsers.forEachIndexed { i, (pkg, label) ->
                        val selected = pkg == browser
                        val locked = browser != null && anyDomain
                        ListRow(
                            title = label,
                            meta = pkg,
                            trailing = if (selected) "Chosen" else if (locked) null else "Choose",
                            onTrailing = if (selected || locked) null else ({
                                browser = pkg
                                repo.browser = pkg
                                scope.launch { repo.reconcile() }
                            }),
                        )
                        if (i < browsers.lastIndex) Hairline()
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlineButton("Refresh", Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad)) { tick++; scope.launch { repo.reconcile() } }
            Spacer(Modifier.height(32.dp))
        }
    }
}
