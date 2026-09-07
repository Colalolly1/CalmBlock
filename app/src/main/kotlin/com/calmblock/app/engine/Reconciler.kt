package com.calmblock.app.engine

import android.content.Context
import android.util.Log
import com.calmblock.app.access.WatchService
import com.calmblock.app.admin.PolicyEngine
import com.calmblock.app.core.Block
import com.calmblock.app.core.Mode
import com.calmblock.app.core.Rules
import com.calmblock.app.core.Status
import com.calmblock.app.data.AppDb
import com.calmblock.app.data.BlockEntity
import com.calmblock.app.data.ItemEntity
import com.calmblock.app.data.Prefs
import com.calmblock.app.media.MediaGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Re-derives what should be blocked from stored state and makes the device
 * match. Boot, alarms, the backstop, process start, the accessibility service
 * connecting, and every edit in the UI all call this and nothing else.
 */
object Reconciler {

    private val mutex = Mutex()

    suspend fun run(context: Context) = mutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching { reconcile(context.applicationContext) }
                .onFailure { Log.e("CalmBlock", "reconcile failed", it) }
        }
    }

    private suspend fun reconcile(context: Context) {
        val prefs = Prefs(context)
        val dao = AppDb.get(context).dao()
        val policy = PolicyEngine(context)
        val now = Clock.now(prefs)

        ensurePermanentList(dao, now)

        val blocks = dao.blocksNow().map { it.toBlock() }
        val items = dao.allItemsNow()
        val active = blocks.filter { Rules.status(it, now) is Status.Active }.map { it.id }.toSet()

        val apps = HashSet<String>()
        val domains = HashSet<String>()
        val labels = HashMap<String, String>()
        for (item in items) {
            if (item.blockId !in active) continue
            when (item.kind) {
                ItemEntity.APP -> { apps += item.value; labels[item.value] = item.label }
                ItemEntity.DOMAIN -> domains += item.value
            }
        }

        val browser = prefs.browser
        val anyDomain = items.any { it.kind == ItemEntity.DOMAIN }
        if (anyDomain) {
            val protected = policy.protectedPackages()
            for (pkg in policy.webCapablePackages() - protected) {
                if (pkg != browser) { apps += pkg; labels[pkg] = policy.label(pkg) }
            }
        }

        Registry.blockedApps = apps
        Registry.labels = labels
        Registry.domains = domains
        Registry.browser = browser
        Registry.adminActive = policy.isAdminActive
        Registry.launchers = policy.launcherPackages()
        WatchService.enforceNow()
        MediaGuard.enforceNow()

        runCatching { UsageTracker(context, policy).rollup(dao, now) }
            .onFailure { Log.w("CalmBlock", "usage rollup failed", it) }

        val next = blocks.mapNotNull { Rules.nextTransition(it, now) }.minOrNull()
        Alarms.schedule(context, next)
        prefs.lastReconcile = now
    }

    private suspend fun ensurePermanentList(dao: com.calmblock.app.data.BlockDao, now: Long) {
        if (dao.blocksNow().none { it.builtIn }) {
            dao.insert(BlockEntity(name = "Permanent list", mode = Mode.PERMANENT.name, builtIn = true, confirmedAt = now, createdAt = 0L))
        }
    }
}

fun BlockEntity.toBlock() = Block(
    id = id, name = name, mode = Mode.valueOf(mode), builtIn = builtIn, confirmedAt = confirmedAt,
    timedEndAt = timedEndAt, days = days, startMinute = startMinute, endMinute = endMinute,
    freezeMinutes = freezeMinutes, enabled = enabled, createdAt = createdAt,
)
