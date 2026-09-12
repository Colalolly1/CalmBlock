package com.calmblock.app.access

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.calmblock.app.core.Domains
import com.calmblock.app.engine.Reconciler
import com.calmblock.app.engine.Registry
import com.calmblock.app.ui.blocked.BlockedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The enforcement layer. Three jobs. When a blocked app comes to the front,
 * cover it. In the browser, read the address bar and cover blocked domains.
 * In Settings, keep the user off the pages that could undo the app.
 */
class WatchService : AccessibilityService() {

    private var lastShown = 0L
    private var lastKey = ""
    private val main = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        instance = this
        CoroutineScope(Dispatchers.IO).launch { Reconciler.run(applicationContext) }
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        when {
            pkg in Registry.blockedApps -> {
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) show(BlockedActivity.KIND_APP, Registry.labels[pkg] ?: pkg)
            }
            pkg == Registry.browser -> checkBrowser(event)
            pkg in GUARDED -> guardSettings(event)
            pkg in Registry.launchers -> guardLauncher(event)
        }
    }

    override fun onInterrupt() = Unit

    /** Called after every reconcile: a block that just started may find its app already open. */
    private fun enforce() {
        val root = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString() ?: return
        if (pkg in Registry.blockedApps) show(BlockedActivity.KIND_APP, Registry.labels[pkg] ?: pkg)
    }

    private fun checkBrowser(event: AccessibilityEvent) {
        val domains = Registry.domains
        if (domains.isEmpty()) return
        val root = rootInActiveWindow ?: event.source ?: return
        val text = addressBarText(root) ?: return
        val host = Domains.normalise(text) ?: return
        if (!Domains.isBlocked(host, domains)) return
        performGlobalAction(GLOBAL_ACTION_BACK)
        show(BlockedActivity.KIND_SITE, host)
    }

    private fun addressBarText(root: AccessibilityNodeInfo): String? {
        for (id in ADDRESS_BAR_IDS) {
            val t = root.findAccessibilityNodeInfosByViewId(id).firstOrNull()?.text?.toString()
            if (!t.isNullOrBlank()) return t
        }
        return firstEditText(root, 0)?.text?.toString()
    }

    private fun firstEditText(node: AccessibilityNodeInfo, depth: Int): AccessibilityNodeInfo? {
        if (depth > 12) return null
        if (node.className?.toString()?.endsWith("EditText") == true) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            firstEditText(child, depth + 1)?.let { return it }
        }
        return null
    }

    /**
     * Settings pages that mention this app and could remove it: app info
     * (uninstall, force stop, clear data), the device admin page once admin is
     * active, this service's own toggle page, and the package installer's
     * uninstall dialog. The pages needed to set the app up are left alone.
     */
    /**
     * Settings pages that are about this app are off limits once every
     * permission is granted: app info, its accessibility switch, its
     * notification and usage access switches, the device admin page. Pages
     * that merely list it among other apps are left alone, so the rest of
     * Settings stays usable. While a permission is still missing, nothing is
     * guarded, so it can be granted.
     */
    private fun guardSettings(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return
        if (!Registry.setupComplete) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: ""
        val root = rootInActiveWindow
        val text = StringBuilder()
        event.text.forEach { text.append(it).append(' ') }
        root?.let { collectText(it, text, 0) }
        val dangerous = if (pkg == "com.android.settings") aboutThisApp(root, cls, text) else text.contains(APP_NAME, ignoreCase = true)
        if (dangerous) goHome()
    }

    /** True when the page's subject is this app, as opposed to a list that happens to include it. */
    private fun aboutThisApp(root: AccessibilityNodeInfo?, cls: String, text: CharSequence): Boolean {
        if (root != null) {
            for (id in HEADING_IDS) {
                val heading = root.findAccessibilityNodeInfosByViewId(id).firstOrNull()?.text?.toString()?.trim() ?: continue
                if (heading.equals(APP_NAME, ignoreCase = true)) return true
            }
        }
        if (!text.contains(APP_NAME, ignoreCase = true)) return false
        return when {
            PER_APP_SCREENS.any { cls.contains(it, ignoreCase = true) } -> true
            text.contains("Use $APP_NAME", ignoreCase = true) -> true
            APP_INFO_WORDS.any { text.contains(it, ignoreCase = true) } -> true
            Registry.adminActive && text.contains("Deactivate", ignoreCase = true) -> true
            else -> false
        }
    }

    /**
     * Only the long-press on this app's own row counts. The app list always
     * contains the name, so the whole window must never be searched here.
     */
    private fun guardLauncher(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) return
        val pressed = StringBuilder()
        event.text.forEach { pressed.append(it).append(' ') }
        event.source?.let { collectText(it, pressed, 0) }
        val words = pressed.toString().trim()
        if (!words.equals(APP_NAME, ignoreCase = true) && !words.contains(APP_NAME, ignoreCase = true)) return
        if (words.length > APP_NAME.length + 24) return
        listOf(0L, 120L, 350L).forEach { delay -> main.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, delay) }
    }

    private fun collectText(node: AccessibilityNodeInfo, into: StringBuilder, depth: Int) {
        if (depth > 10) return
        node.text?.let { into.append(it).append(' ') }
        node.contentDescription?.let { into.append(it).append(' ') }
        for (i in 0 until node.childCount) node.getChild(i)?.let { collectText(it, into, depth + 1) }
    }

    private fun goBack() {
        val now = System.currentTimeMillis()
        if (now - lastShown < 800L) return
        lastShown = now
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun goHome() {
        val now = System.currentTimeMillis()
        if (now - lastShown < 400L) return
        lastShown = now
        listOf(0L, 150L, 400L).forEach { delay ->
            main.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, delay)
        }
    }

    private fun show(kind: String, label: String) {
        val now = System.currentTimeMillis()
        val key = "$kind:$label"
        if (key == lastKey && now - lastShown < 1500L) return
        lastKey = key
        lastShown = now
        startActivity(
            Intent(this, BlockedActivity::class.java)
                .putExtra(BlockedActivity.EXTRA_KIND, kind)
                .putExtra(BlockedActivity.EXTRA_LABEL, label)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }

    companion object {
        private const val APP_NAME = "CalmBlock"
        /** The page heading on this ROM's Settings: the toolbar title, and the per-app header. */
        private val HEADING_IDS = listOf(
            "com.android.settings:id/entity_header_title",
            "com.android.settings:id/toolbar_title",
        )

        /** Screens that are always about one app. Lists of apps are deliberately absent. */
        private val PER_APP_SCREENS = listOf(
            "InstalledAppDetails",
            "AppInfoDashboard",
            "NotificationAccessDetails",
            "UsageAccessDetails",
            "ToggleAccessibilityServicePreferenceFragment",
            "AccessibilityDetailsSettings",
            "DeviceAdminAdd",
        )
        private val APP_INFO_WORDS = listOf("Uninstall", "Force stop", "Clear storage", "Clear data", "Clear cache")
        private val GUARDED = setOf("com.android.settings", "com.android.packageinstaller", "com.google.android.packageinstaller")
        private val ADDRESS_BAR_IDS = listOf(
            "com.duckduckgo.mobile.android:id/omnibarTextInput",
            "com.android.chrome:id/url_bar",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
        )

        @Volatile private var instance: WatchService? = null

        fun enforceNow() {
            val s = instance ?: return
            s.main.post { s.enforce() }
        }
    }
}
