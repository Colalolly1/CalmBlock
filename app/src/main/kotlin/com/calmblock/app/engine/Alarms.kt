package com.calmblock.app.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object Alarms {
    private const val TRANSITION = 1
    private const val BACKSTOP = 2
    private const val BACKSTOP_INTERVAL = 15L * 60L * 1000L

    fun schedule(context: Context, nextTransition: Long?) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val transition = pending(context, TRANSITION)
        am.cancel(transition)
        if (nextTransition != null) {
            val at = nextTransition + 1000L
            if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, transition)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, transition)
        }
        val backstop = pending(context, BACKSTOP)
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + BACKSTOP_INTERVAL, BACKSTOP_INTERVAL, backstop)
    }

    private fun pending(context: Context, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, code, Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
