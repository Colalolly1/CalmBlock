package com.calmblock.app.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.telecom.TelecomManager

/** What the app knows about the phone: the admin role, and which packages are what. */
class PolicyEngine(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val pm: PackageManager = context.packageManager

    val admin = ComponentName(context, BlockAdminReceiver::class.java)

    /** Device admin is what removes the Uninstall button. */
    val isAdminActive: Boolean get() = dpm.isAdminActive(admin)

    /** Packages the blocked screen must never cover. */
    fun protectedPackages(): Set<String> {
        val out = HashSet<String>()
        out += context.packageName
        out += listOf("android", "com.android.settings", "com.android.systemui", "com.android.phone", "com.android.server.telecom", "com.android.packageinstaller", "com.google.android.packageinstaller")
        resolveAll(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)).forEach { out += it }
        (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.defaultDialerPackage?.let { out += it }
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')?.takeIf { it.isNotBlank() }?.let { out += it }
        return out
    }

    fun launcherPackages(): Set<String> = resolveAll(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))

    /** Settings answers the home query too, via its boot-time fallback activity. */
    fun settingsPackages(): Set<String> =
        setOf("com.android.settings", "com.android.packageinstaller", "com.google.android.packageinstaller")

    /** Anything that will open a plain http link. */
    fun webCapablePackages(): Set<String> =
        resolveAll(Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com")).addCategory(Intent.CATEGORY_BROWSABLE))

    /** Apps with a launcher entry, as package to label. */
    fun launchableApps(): Map<String, String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .associate { it.activityInfo.packageName to it.loadLabel(pm).toString() }
    }

    fun label(pkg: String): String = runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private fun resolveAll(intent: Intent): Set<String> =
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).map { it.activityInfo.packageName }.toSet()
}
