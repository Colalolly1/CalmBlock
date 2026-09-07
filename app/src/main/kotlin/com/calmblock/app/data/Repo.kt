package com.calmblock.app.data

import android.content.Context
import com.calmblock.app.admin.PolicyEngine
import com.calmblock.app.core.Block
import com.calmblock.app.core.Domains
import com.calmblock.app.core.Mode
import com.calmblock.app.core.Rules
import com.calmblock.app.core.Status
import com.calmblock.app.engine.Clock
import com.calmblock.app.engine.Reconciler
import com.calmblock.app.engine.toBlock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Every write the UI can make. Each one checks the block's status first and
 * refuses anything the rules forbid, then reconciles so the device matches.
 */
class Repo(private val context: Context) {

    private val dao = AppDb.get(context).dao()
    private val prefs = Prefs(context)
    val policy = PolicyEngine(context)

    fun now(): Long = Clock.now(prefs)

    val blocks: Flow<List<Block>> = dao.blocks().map { list -> list.map { it.toBlock() } }
    fun block(id: Long): Flow<Block?> = dao.block(id).map { it?.toBlock() }
    fun items(blockId: Long): Flow<List<ItemEntity>> = dao.items(blockId)
    val allItems: Flow<List<ItemEntity>> = dao.allItems()
    fun usage(from: String, to: String): Flow<List<UsageDayEntity>> = dao.usageBetween(from, to)
    suspend fun allUsage(): List<UsageDayEntity> = dao.allUsageNow()

    var browser: String?
        get() = prefs.browser
        set(v) { prefs.browser = v }

    fun events(from: String, to: String): Flow<List<BlockedEventEntity>> = dao.eventsBetween(from, to)

    suspend fun newBlock(mode: Mode): Long {
        val name = if (mode == Mode.PERMANENT) "New permanent block" else "New block"
        val id = dao.insert(BlockEntity(name = name, mode = mode.name, createdAt = now()))
        Reconciler.run(context)
        return id
    }

    suspend fun rename(id: Long, name: String) = editable(id) { dao.update(it.copy(name = name.trim().ifEmpty { "Block" })) }

    /** Only between Timed and Scheduled. Permanent is a separate kind of block, never a switch. */
    suspend fun setMode(id: Long, mode: Mode) = editable(id) {
        if (mode != Mode.PERMANENT && it.mode != Mode.PERMANENT.name) dao.update(it.copy(mode = mode.name, timedEndAt = null, enabled = false))
    }

    suspend fun setScheduleOn(id: Long, on: Boolean) = editable(id) {
        if (it.mode == Mode.SCHEDULED.name) dao.update(it.copy(enabled = on))
    }

    suspend fun setSchedule(id: Long, days: Int, startMinute: Int, endMinute: Int, freezeMinutes: Int) = editable(id) {
        dao.update(it.copy(days = days, startMinute = startMinute, endMinute = endMinute, freezeMinutes = freezeMinutes.coerceAtLeast(0)))
    }

    /** The one-way door. */
    suspend fun confirmPermanent(id: Long) = editable(id) {
        if (it.mode == Mode.PERMANENT.name) dao.update(it.copy(confirmedAt = now()))
    }

    suspend fun startTimed(id: Long, minutes: Int) = editable(id) {
        if (it.mode == Mode.TIMED.name && minutes > 0) dao.update(it.copy(timedEndAt = now() + minutes * Rules.MINUTE))
    }

    /** Only ever later, never earlier. */
    suspend fun extendTimed(id: Long, minutes: Int) {
        val e = dao.blockNow(id) ?: return
        val end = e.timedEndAt ?: return
        if (e.mode == Mode.TIMED.name && minutes > 0 && end > now()) dao.update(e.copy(timedEndAt = end + minutes * Rules.MINUTE))
        Reconciler.run(context)
    }

    suspend fun delete(id: Long) {
        val e = dao.blockNow(id) ?: return
        val b = e.toBlock()
        if (!Rules.canDelete(b, Rules.status(b, now()))) return
        dao.deleteItemsOf(id)
        dao.deleteUnconfirmed(id)
        Reconciler.run(context)
    }

    suspend fun addApp(blockId: Long, pkg: String, label: String) = addable(blockId) {
        dao.insertItem(ItemEntity(blockId, ItemEntity.APP, pkg, label, now()))
    }

    /** Returns false when the text holds no usable domain. */
    suspend fun addDomain(blockId: Long, text: String): Boolean {
        val domain = Domains.normalise(text) ?: return false
        addable(blockId) { dao.insertItem(ItemEntity(blockId, ItemEntity.DOMAIN, domain, domain, now())) }
        return true
    }

    suspend fun removeItem(blockId: Long, kind: String, value: String) {
        val e = dao.blockNow(blockId) ?: return
        val b = e.toBlock()
        if (!Rules.canRemove(Rules.status(b, now()))) return
        dao.deleteItem(blockId, kind, value)
        Reconciler.run(context)
    }

    suspend fun reconcile() = Reconciler.run(context)

    private suspend fun editable(id: Long, change: suspend (BlockEntity) -> Unit) {
        val e = dao.blockNow(id) ?: return
        val b = e.toBlock()
        if (b.builtIn || Rules.status(b, now()) != Status.Inactive) return
        change(e)
        Reconciler.run(context)
    }

    private suspend fun addable(id: Long, change: suspend (BlockEntity) -> Unit) {
        val e = dao.blockNow(id) ?: return
        val b = e.toBlock()
        if (!Rules.canAdd(b, Rules.status(b, now()))) return
        change(e)
        Reconciler.run(context)
    }
}
