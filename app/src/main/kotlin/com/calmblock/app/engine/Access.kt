package com.calmblock.app.engine

import android.app.AppOpsManager
import android.content.Context
import android.provider.Settings
import com.calmblock.app.access.WatchService

object Access {
    fun accessibilityOn(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val mine = "${context.packageName}/${WatchService::class.java.name}"
        return enabled.split(':').any { it.equals(mine, ignoreCase = true) }
    }

    fun usageAccessOn(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
