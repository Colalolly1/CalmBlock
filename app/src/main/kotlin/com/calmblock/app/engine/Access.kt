package com.calmblock.app.engine

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.calmblock.app.access.WatchService
import com.calmblock.app.media.MediaGuard

object Access {
    fun accessibilityOn(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val mine = "${context.packageName}/${WatchService::class.java.name}"
        return enabled.split(':').any { it.equals(mine, ignoreCase = true) }
    }

    /** Notification access is what lets the app read and pause media sessions. */
    fun notificationAccessOn(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        val mine = ComponentName(context, MediaGuard::class.java).flattenToString()
        val short = ComponentName(context, MediaGuard::class.java).flattenToShortString()
        return enabled.split(':').any { it.equals(mine, ignoreCase = true) || it.equals(short, ignoreCase = true) }
    }

    fun usageAccessOn(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
