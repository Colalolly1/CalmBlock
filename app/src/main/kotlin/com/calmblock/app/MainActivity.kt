package com.calmblock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.calmblock.app.data.Repo
import com.calmblock.app.ui.BottomTabBar
import com.calmblock.app.ui.EinkRefresh
import com.calmblock.app.ui.Ink
import com.calmblock.app.ui.TabItem
import com.calmblock.app.ui.blocks.AppsScreen
import com.calmblock.app.ui.blocks.BlockScreen
import com.calmblock.app.ui.blocks.BlocksScreen
import com.calmblock.app.ui.settings.SettingsScreen
import com.calmblock.app.ui.usage.UsageScreen
import kotlinx.coroutines.launch

private enum class Tab { BLOCKS, USAGE, SETTINGS }

private sealed interface Overlay {
    data class Block(val id: Long) : Overlay
    data class Apps(val id: Long) : Overlay
}

private val EinkColors = lightColorScheme(
    primary = Color.Black, onPrimary = Color.White, background = Color.White, onBackground = Color.Black,
    surface = Color.White, onSurface = Color.Black, outline = Color(0xFFDDDDDD),
)

/** Single activity. Navigation is a tab plus an optional overlay, no nav library. */
class MainActivity : ComponentActivity() {

    private lateinit var repo: Repo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = Repo(this)
        setContent { MaterialTheme(colorScheme = EinkColors) { Shell() } }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { repo.reconcile() }
    }

    @Composable
    private fun Shell() {
        var tab by remember { mutableStateOf(Tab.BLOCKS) }
        var overlay by remember { mutableStateOf<Overlay?>(null) }
        val scope = lifecycleScope

        when (val ov = overlay) {
            is Overlay.Block -> {
                BlockScreen(
                    repo = repo,
                    id = ov.id,
                    onBack = { overlay = null; flash() },
                    onPickApps = { overlay = Overlay.Apps(ov.id); flash() },
                )
                return
            }
            is Overlay.Apps -> {
                AppsScreen(repo = repo, id = ov.id, onDone = { overlay = Overlay.Block(ov.id); flash() })
                return
            }
            null -> Unit
        }

        Column(Modifier.fillMaxSize().background(Ink.bg).statusBarsPadding()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    Tab.BLOCKS -> BlocksScreen(
                        repo = repo,
                        onOpen = { overlay = Overlay.Block(it); flash() },
                        onNew = { mode -> scope.launch { val id = repo.newBlock(mode); overlay = Overlay.Block(id); flash() } },
                    )
                    Tab.USAGE -> UsageScreen(repo)
                    Tab.SETTINGS -> SettingsScreen(repo)
                }
            }
            BottomTabBar(
                listOf(
                    TabItem("Blocks", tab == Tab.BLOCKS) { tab = Tab.BLOCKS; flash() },
                    TabItem("Usage", tab == Tab.USAGE) { tab = Tab.USAGE; flash() },
                    TabItem("Settings", tab == Tab.SETTINGS) { tab = Tab.SETTINGS; flash() },
                ),
            )
        }
    }

    private fun flash() = EinkRefresh.fullRefresh(this)
}
