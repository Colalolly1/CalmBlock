package com.calmblock.app.engine

/**
 * What the accessibility service needs at event time, kept in memory so it
 * never touches the database on the main thread. Written by [Reconciler].
 */
object Registry {
    @Volatile var blockedApps: Set<String> = emptySet()
    @Volatile var labels: Map<String, String> = emptyMap()
    @Volatile var domains: Set<String> = emptySet()
    @Volatile var browser: String? = null
    @Volatile var adminActive: Boolean = false
    @Volatile var launchers: Set<String> = emptySet()
}
