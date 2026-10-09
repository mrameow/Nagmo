package com.nagmo.app.data

import android.content.Context
import android.util.Log
import com.nagmo.app.alarm.AlarmScheduler
import com.nagmo.app.alarm.NagCounter
import com.nagmo.app.alarm.Notifications
import com.nagmo.app.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Single source of truth for nags. Stored as a small JSON file; every change
 * re-schedules alarms and refreshes widgets and the lock-screen summary.
 */
object NagRepository {
    private const val TAG = "NagRepository"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private lateinit var appContext: Context
    private lateinit var file: File
    private val lock = Any()

    private val _store = MutableStateFlow(NagStore())
    val store: StateFlow<NagStore> = _store.asStateFlow()

    val nags: List<Nag> get() = _store.value.nags

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        file = File(appContext.filesDir, "nags.json")
        _store.value = load()
    }

    private fun load(): NagStore = try {
        if (file.exists()) json.decodeFromString(NagStore.serializer(), file.readText()) else NagStore()
    } catch (e: Exception) {
        Log.e(TAG, "Could not read nags, starting fresh", e)
        file.copyTo(File(appContext.filesDir, "nags.broken.json"), overwrite = true)
        NagStore()
    }

    private fun save(store: NagStore) {
        val tmp = File(appContext.filesDir, "nags.json.tmp")
        tmp.writeText(json.encodeToString(NagStore.serializer(), store))
        tmp.renameTo(file)
    }

    private fun update(changedIds: Collection<Int>, transform: (NagStore) -> NagStore): NagStore {
        val before: NagStore
        val after: NagStore
        synchronized(lock) {
            before = _store.value
            after = transform(before)
            save(after)
            _store.value = after
        }
        for (id in changedIds) {
            val nag = after.nags.firstOrNull { it.id == id }
            if (nag == null) AlarmScheduler.cancel(appContext, id) else AlarmScheduler.schedule(appContext, nag)
        }
        refreshSurfaces()
        return after
    }

    fun refreshSurfaces() {
        WidgetUpdater.updateAll(appContext)
        Notifications.updateLockScreenSummary(appContext)
    }

    fun get(id: Int): Nag? = nags.firstOrNull { it.id == id }

    /** Inserts a new nag (id <= 0) or replaces an existing one. Returns the saved nag. */
    fun upsert(nag: Nag): Nag {
        var saved = nag
        val id = if (nag.id > 0) nag.id else _store.value.nextId
        update(listOf(id)) { s ->
            if (nag.id > 0) {
                saved = nag
                s.copy(nags = s.nags.map { if (it.id == nag.id) nag else it })
            } else {
                saved = nag.copy(id = s.nextId, createdAt = System.currentTimeMillis())
                s.copy(nextId = s.nextId + 1, nags = s.nags + saved)
            }
        }
        return saved
    }

    fun delete(id: Int) {
        NagCounter.reset(appContext, id)
        Notifications.cancelAlarm(appContext, id)
        update(listOf(id)) { s -> s.copy(nags = s.nags.filterNot { it.id == id }) }
    }

    /**
     * Marks a nag as done. Repeating nags roll forward to their next occurrence
     * instead of being completed for good.
     */
    fun complete(id: Int) {
        NagCounter.reset(appContext, id)
        Notifications.cancelAlarm(appContext, id)
        val now = System.currentTimeMillis()
        update(listOf(id)) { s ->
            val nag = s.nags.firstOrNull { it.id == id } ?: return@update s
            val record = Completion(id, nag.title, now, onTime = nag.dueAt == null || now <= nag.dueAt)
            val updated = if (nag.repeat != Repeat.NONE && (nag.remindAt != null || nag.dueAt != null)) {
                rollForward(nag, now)
            } else {
                nag.copy(done = true, completedAt = now, nextNagAt = null)
            }
            s.copy(
                nags = s.nags.map { if (it.id == id) updated else it },
                history = (s.history + record).takeLast(2000),
            )
        }
    }

    private fun rollForward(nag: Nag, now: Long): Nag {
        var remind = nag.remindAt
        var due = nag.dueAt
        do {
            remind = remind?.let { nag.repeat.next(it) }
            due = due?.let { nag.repeat.next(it) }
        } while ((remind ?: due ?: Long.MAX_VALUE) <= now)
        return nag.copy(
            remindAt = remind,
            dueAt = due,
            nextNagAt = null,
            subtasks = nag.subtasks.map { it.copy(done = false) },
        )
    }

    fun reopen(id: Int) {
        update(listOf(id)) { s ->
            s.copy(nags = s.nags.map { if (it.id == id) it.copy(done = false, completedAt = null) else it })
        }
    }

    fun snooze(id: Int, minutes: Int) {
        Notifications.cancelAlarm(appContext, id)
        val at = System.currentTimeMillis() + minutes * 60_000L
        update(listOf(id)) { s ->
            s.copy(nags = s.nags.map { if (it.id == id) it.copy(nextNagAt = at) else it })
        }
    }

    /** Called when an alarm rang: schedules the next nag (if nagging is on). */
    fun onAlarmRang(id: Int) {
        val now = System.currentTimeMillis()
        update(listOf(id)) { s ->
            s.copy(nags = s.nags.map { nag ->
                if (nag.id != id) return@map nag
                when {
                    nag.nagEveryMinutes > 0 -> nag.copy(nextNagAt = now + nag.nagEveryMinutes * 60_000L)
                    // Repeating reminders keep ringing on schedule even if never ticked off.
                    nag.repeat != Repeat.NONE && nag.remindAt != null && nag.remindAt <= now ->
                        nag.copy(nextNagAt = null, remindAt = nextFuture(nag.repeat, nag.remindAt, now))
                    else -> nag.copy(nextNagAt = null)
                }
            })
        }
    }

    private fun nextFuture(repeat: Repeat, from: Long, now: Long): Long {
        var t = from
        while (t <= now) t = repeat.next(t)
        return t
    }

    /** Stops nagging about a nag without completing it. */
    fun stopNagging(id: Int) {
        NagCounter.reset(appContext, id)
        Notifications.cancelAlarm(appContext, id)
        update(listOf(id)) { s ->
            s.copy(nags = s.nags.map { if (it.id == id) it.copy(nextNagAt = null) else it })
        }
    }

    fun toggleSubtask(id: Int, index: Int) {
        update(listOf(id)) { s ->
            s.copy(nags = s.nags.map { nag ->
                if (nag.id != id) nag else nag.copy(subtasks = nag.subtasks.mapIndexed { i, st ->
                    if (i == index) st.copy(done = !st.done) else st
                })
            })
        }
    }

    fun togglePin(id: Int) {
        update(listOf(id)) { s ->
            s.copy(nags = s.nags.map { if (it.id == id) it.copy(pinned = !it.pinned) else it })
        }
    }

    fun clearCompleted() {
        val ids = nags.filter { it.done }.map { it.id }
        update(ids) { s -> s.copy(nags = s.nags.filterNot { it.done }) }
    }

    /** Re-registers every alarm, e.g. after a reboot or time-zone change. */
    fun rescheduleAll() {
        nags.forEach { AlarmScheduler.schedule(appContext, it) }
        AlarmScheduler.scheduleDigest(appContext)
        refreshSurfaces()
    }

    /** Pending nags in the order the user should care about them. */
    fun pendingSorted(list: List<Nag> = nags): List<Nag> =
        list.filter { !it.done }.sortedWith(
            compareByDescending<Nag> { it.pinned }
                .thenByDescending { it.isOverdue() }
                .thenBy { it.dueAt ?: it.nextAlarmAt ?: Long.MAX_VALUE }
                .thenByDescending { it.priority.ordinal }
        )

    fun dueToday(list: List<Nag> = nags, zone: ZoneId = ZoneId.systemDefault()): List<Nag> {
        val endOfToday = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return pendingSorted(list).filter { n ->
            n.isOverdue() || (n.dueAt ?: Long.MAX_VALUE) < endOfToday ||
                (n.nextAlarmAt ?: Long.MAX_VALUE) < endOfToday
        }
    }
}
