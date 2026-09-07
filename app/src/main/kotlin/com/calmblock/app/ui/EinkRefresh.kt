package com.calmblock.app.ui

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/** Flashes black then white over the window to clear e-ink ghosting between screens. Same as CalmMoney. */
object EinkRefresh {
    private const val FLASH_MS = 50L

    fun fullRefresh(activity: Activity) {
        val decor = activity.window?.decorView as? ViewGroup ?: return
        val overlay = View(activity).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        decor.addView(overlay)
        overlay.postDelayed({
            overlay.setBackgroundColor(Color.WHITE)
            overlay.postDelayed({ decor.removeView(overlay) }, FLASH_MS)
        }, FLASH_MS)
    }
}
