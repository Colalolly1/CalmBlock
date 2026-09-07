package com.calmblock.app.data

import android.content.Context

/** The handful of scalar settings that do not belong in a table. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("calmblock", Context.MODE_PRIVATE)

    var browser: String?
        get() = sp.getString("browser", null)
        set(v) = sp.edit().putString("browser", v).apply()

    /** The latest wall-clock time ever observed. Time is not allowed to run backwards. */
    var lastSeen: Long
        get() = sp.getLong("lastSeen", 0L)
        set(v) = sp.edit().putLong("lastSeen", v).apply()

    var lastQuote: Int
        get() = sp.getInt("lastQuote", -1)
        set(v) = sp.edit().putInt("lastQuote", v).apply()

    var lastReconcile: Long
        get() = sp.getLong("lastReconcile", 0L)
        set(v) = sp.edit().putLong("lastReconcile", v).apply()
}
