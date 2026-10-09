package com.nagmo.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nagmo.app.data.Nag
import com.nagmo.app.data.Settings
import com.nagmo.app.ui.MainActivity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object AlarmScheduler {
    const val EXTRA_NAG_ID = "nag_id"
    const val ACTION_REMIND = "com.nagmo.app.action.REMIND"
    const val ACTION_DEADLINE = "com.nagmo.app.action.DEADLINE"
    const val ACTION_DIGEST = "com.nagmo.app.action.DIGEST"

    private const val KIND_REMIND = 0
    private const val KIND_DEADLINE = 1
    private const val DIGEST_REQUEST = Int.MAX_VALUE - 1

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager(context).canScheduleExactAlarms()

    private fun pending(context: Context, id: Int, kind: Int, action: String, flags: Int): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_NAG_ID, id)
        return PendingIntent.getBroadcast(context, id * 4 + kind, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(context: Context, nag: Nag) {
        val now = System.currentTimeMillis()

        // Main alarm (or the next re-nag / snooze).
        var at = nag.nextAlarmAt
        // An unanswered re-nag that was missed (e.g. phone was off) rings right away.
        if (at != null && at <= now && nag.nextNagAt != null && !nag.done) at = now + 5_000
        if (at != null && at > now) {
            setAlarmClock(context, at, pending(context, nag.id, KIND_REMIND, ACTION_REMIND, PendingIntent.FLAG_UPDATE_CURRENT)!!)
        } else {
            cancelKind(context, nag.id, KIND_REMIND, ACTION_REMIND)
        }

        // Deadline heads-up.
        val warn = Settings.current.deadlineWarningMinutes
        val warnAt = nag.dueAt?.minus(warn * 60_000L)
        if (!nag.done && warn > 0 && warnAt != null && warnAt > now) {
            setExact(context, warnAt, pending(context, nag.id, KIND_DEADLINE, ACTION_DEADLINE, PendingIntent.FLAG_UPDATE_CURRENT)!!)
        } else {
            cancelKind(context, nag.id, KIND_DEADLINE, ACTION_DEADLINE)
        }
    }

    fun cancel(context: Context, id: Int) {
        cancelKind(context, id, KIND_REMIND, ACTION_REMIND)
        cancelKind(context, id, KIND_DEADLINE, ACTION_DEADLINE)
    }

    private fun cancelKind(context: Context, id: Int, kind: Int, action: String) {
        pending(context, id, kind, action, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager(context).cancel(it)
            it.cancel()
        }
    }

    private fun setAlarmClock(context: Context, at: Long, operation: PendingIntent) {
        val am = alarmManager(context)
        if (canScheduleExact(context)) {
            val show = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), operation)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    private fun setExact(context: Context, at: Long, operation: PendingIntent) {
        val am = alarmManager(context)
        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    /** Schedules (or cancels) the morning digest according to the settings. */
    fun scheduleDigest(context: Context) {
        val s = Settings.current
        val intent = Intent(context, AlarmReceiver::class.java).setAction(ACTION_DIGEST)
        val pi = PendingIntent.getBroadcast(
            context, DIGEST_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = alarmManager(context)
        if (!s.morningDigest) {
            am.cancel(pi)
            return
        }
        val zone = ZoneId.systemDefault()
        var next = ZonedDateTime.of(LocalDate.now(zone), LocalTime.of(s.digestHour, s.digestMinute), zone)
        if (!next.isAfter(ZonedDateTime.now(zone))) next = next.plusDays(1)
        setExact(context, next.toInstant().toEpochMilli(), pi)
    }
}
