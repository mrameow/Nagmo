package com.nagmo.app.data

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Serializable
enum class Priority(val label: String) {
    LOW("Chill"),
    MEDIUM("Normal"),
    HIGH("Urgent"),
}

/** Sticky-note colours. ARGB values are shared by the app UI and the widgets. */
@Serializable
enum class NoteColor(val label: String, val argb: Long, val darkArgb: Long) {
    YELLOW("Lemon", 0xFFFFE27A, 0xFF5C4A12),
    PINK("Bubblegum", 0xFFFFC2D1, 0xFF5E2636),
    MINT("Mint", 0xFFBDEFD8, 0xFF1F4A3A),
    BLUE("Sky", 0xFFC4DDFF, 0xFF1F3550),
    PURPLE("Lilac", 0xFFE0CCFF, 0xFF3A2A55),
    ORANGE("Peach", 0xFFFFD3B0, 0xFF5A3418),
}

@Serializable
enum class Repeat(val label: String) {
    NONE("Once"),
    DAILY("Every day"),
    WEEKDAYS("Weekdays"),
    WEEKLY("Every week"),
    MONTHLY("Every month");

    /** Moves [millis] to the next occurrence of this repeat rule. */
    fun next(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val t = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)
        val next = when (this) {
            NONE -> t
            DAILY -> t.plusDays(1)
            WEEKDAYS -> {
                var d = t.plusDays(1)
                while (d.dayOfWeek.value >= 6) d = d.plusDays(1)
                d
            }
            WEEKLY -> t.plusWeeks(1)
            MONTHLY -> t.plusMonths(1)
        }
        return next.toInstant().toEpochMilli()
    }
}

@Serializable
data class Subtask(
    val text: String,
    val done: Boolean = false,
)

@Serializable
data class Nag(
    val id: Int,
    /** The work itself. */
    val title: String,
    /** Specifics: free-form notes about the work. */
    val details: String = "",
    /** When to ring the alarm. */
    val remindAt: Long? = null,
    /** When the work needs to be finished. */
    val dueAt: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val color: NoteColor = NoteColor.YELLOW,
    val category: String = "",
    val repeat: Repeat = Repeat.NONE,
    /** Re-nag every N minutes after the alarm until marked done (0 = off). */
    val nagEveryMinutes: Int = 0,
    val subtasks: List<Subtask> = emptyList(),
    val pinned: Boolean = false,
    val done: Boolean = false,
    val completedAt: Long? = null,
    /** Next re-nag or snooze time, overriding [remindAt] when set. */
    val nextNagAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** The moment the next alarm for this nag should ring, if any. */
    val nextAlarmAt: Long?
        get() = if (done) null else nextNagAt ?: remindAt

    fun isOverdue(now: Long = System.currentTimeMillis()) = !done && dueAt != null && dueAt < now

    val subtaskProgress: Pair<Int, Int>
        get() = subtasks.count { it.done } to subtasks.size
}

/** A completion record used for stats and streaks. */
@Serializable
data class Completion(
    val nagId: Int,
    val title: String,
    val at: Long,
    val onTime: Boolean,
)

@Serializable
data class NagStore(
    val nextId: Int = 1,
    val nags: List<Nag> = emptyList(),
    val history: List<Completion> = emptyList(),
)
