package com.nagmo.app.util

import android.content.Context
import android.text.format.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormat {
    private fun zoned(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())

    fun time(context: Context, millis: Long): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return zoned(millis).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    fun date(millis: Long): String {
        val z = zoned(millis)
        val today = LocalDate.now()
        return when (z.toLocalDate()) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> {
                val pattern = if (z.year == today.year) "EEE, d MMM" else "d MMM yyyy"
                z.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
            }
        }
    }

    /** e.g. "today at 5:00 PM", "tomorrow at 9:00 AM", "Fri, 12 Oct at 8:00 AM". */
    fun relative(context: Context, millis: Long): String {
        val d = date(millis)
        val prefix = if (d == "Today" || d == "Tomorrow" || d == "Yesterday") d.lowercase() else d
        return "$prefix at ${time(context, millis)}"
    }

    fun full(context: Context, millis: Long) = "${date(millis)}, ${time(context, millis)}"

    /** "in 25 min", "in 3 h", "2 days ago" … */
    fun countdown(millis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = millis - now
        val abs = kotlin.math.abs(diff)
        val minutes = abs / 60_000
        val text = when {
            minutes < 1 -> return if (diff >= 0) "now" else "just now"
            minutes < 60 -> "$minutes min"
            minutes < 60 * 24 -> "${minutes / 60} h"
            else -> "${minutes / (60 * 24)} d"
        }
        return if (diff >= 0) "in $text" else "$text ago"
    }
}
