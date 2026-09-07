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
            pkg in GUARDED || pkg in Registry.launchers -> guardSettings(event)
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
    private fun guardSettings(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg in Registry.launchers) {
            guardLauncher(event)
            return
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return
        val text = StringBuilder()
        event.text.forEach { text.append(it).append(' ') }
        rootInActiveWindow?.let { collectText(it, text, 0) }
        if (!text.contains(APP_NAME, ignoreCase = true)) return
        val cls = event.className?.toString() ?: ""
        val dangerous = when {
            pkg != "com.android.settings" -> true
            cls.contains("InstalledAppDetails") || cls.contains("AppInfo") -> true
            APP_INFO_WORDS.any { text.contains(it, ignoreCase = true) } -> true
            Registry.adminActive && text.contains("Deactivate", ignoreCase = true) -> true
            text.contains(SERVICE_PAGE_MARKER, ignoreCase = true) -> true
            else -> false
        }
        if (dangerous) goHome()
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
        if (now - lastShown < 800L) return
        lastShown = now
        performGlobalAction(GLOBAL_ACTION_HOME)
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
        private const val SERVICE_PAGE_MARKER = "Shows the blocked screen"
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
