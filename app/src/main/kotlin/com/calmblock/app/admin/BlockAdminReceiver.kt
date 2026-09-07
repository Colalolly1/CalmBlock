package com.calmblock.app.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class BlockAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Turning this off lets the blocks be removed. That is the thing you asked not to be able to do."
}
