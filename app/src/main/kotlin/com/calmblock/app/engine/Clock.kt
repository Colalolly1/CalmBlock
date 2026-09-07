package com.calmblock.app.engine

import com.calmblock.app.data.Prefs

/** Wall time that never runs backwards. A clock set into the past cannot end a block early. */
object Clock {
    fun now(prefs: Prefs): Long {
        val wall = System.currentTimeMillis()
        val seen = prefs.lastSeen
        val now = if (wall > seen) wall else seen
        if (now != seen) prefs.lastSeen = now
        return now
    }
}
