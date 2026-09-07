package com.calmblock.app.media

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.calmblock.app.engine.Reconciler
import com.calmblock.app.engine.Registry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Stops blocked apps making sound. The accessibility service only sees an app
 * when it is on screen; music and podcasts play from the background, so they
 * need their media session pausing directly.
 *
 * Notification access is what gives an app the right to read active media
 * sessions, which is why this is a notification listener. It also clears the
 * notifications of blocked apps, so a blocked podcast cannot sit in the shade
 * with a play button on it.
 */
class MediaGuard : NotificationListenerService() {

    private val main = Handler(Looper.getMainLooper())
    private var sessions: MediaSessionManager? = null
    private val watched = HashMap<MediaController, MediaController.Callback>()

    private val onSessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        rewatch(controllers ?: emptyList())
    }

    override fun onListenerConnected() {
        instance = this
        val manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        sessions = manager
        runCatching {
            manager?.addOnActiveSessionsChangedListener(onSessionsChanged, component())
            rewatch(manager?.getActiveSessions(component()) ?: emptyList())
        }.onFailure { Log.w(TAG, "could not read media sessions", it) }
        CoroutineScope(Dispatchers.IO).launch { Reconciler.run(applicationContext) }
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        runCatching { sessions?.removeOnActiveSessionsChangedListener(onSessionsChanged) }
        clearWatched()
        super.onListenerDisconnected()
    }

    /** A blocked app must not leave a play button sitting in the notification shade. */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        if (pkg !in Registry.blockedApps) return
        runCatching { cancelNotification(sbn.key) }
    }

    /** Watch exactly the sessions that exist now, and silence any that are blocked. */
    private fun rewatch(controllers: List<MediaController>) {
        clearWatched()
        controllers.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    if (state?.state == PlaybackState.STATE_PLAYING) silenceIfBlocked(controller)
                }
            }
            runCatching {
                controller.registerCallback(callback, main)
                watched[controller] = callback
            }
            silenceIfBlocked(controller)
        }
    }

    private fun clearWatched() {
        watched.forEach { (controller, callback) -> runCatching { controller.unregisterCallback(callback) } }
        watched.clear()
    }

    private fun silenceIfBlocked(controller: MediaController) {
        if (controller.packageName !in Registry.blockedApps) return
        runCatching {
            controller.transportControls.pause()
            controller.transportControls.stop()
        }.onFailure { Log.w(TAG, "could not stop ${controller.packageName}", it) }
    }

    /** Called after every reconcile: a block that just started may find something already playing. */
    private fun enforce() {
        val manager = sessions ?: return
        runCatching { manager.getActiveSessions(component()) }.getOrNull()?.let { rewatch(it) }
    }

    private fun component() = ComponentName(this, MediaGuard::class.java)

    companion object {
        private const val TAG = "CalmBlock"

        @Volatile private var instance: MediaGuard? = null

        fun enforceNow() {
            val s = instance ?: return
            // Twice: some players resume a moment after being paused.
            s.main.post { s.enforce() }
            s.main.postDelayed({ s.enforce() }, 700L)
        }
    }
}
