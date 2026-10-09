package com.nagmo.app.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nagmo.app.R
import com.nagmo.app.data.Nag
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Settings
import com.nagmo.app.ui.AlarmActivity
import com.nagmo.app.ui.MainActivity
import com.nagmo.app.util.TimeFormat
import com.nagmo.app.widget.WidgetColors

object Notifications {
    private const val CHANNEL_ALARM = "nag_alarm"
    private const val CHANNEL_ALARM_NO_VIBRATE = "nag_alarm_quiet"
    private const val CHANNEL_DEADLINE = "nag_deadline"
    private const val CHANNEL_DIGEST = "nag_digest"
    private const val CHANNEL_SUMMARY = "nag_summary"

    private const val DEADLINE_OFFSET = 500_000
    private const val ID_DIGEST = 999_998
    private const val ID_SUMMARY = 999_999

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val alarmAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val pattern = longArrayOf(0, 400, 200, 400, 200, 800)

        nm.createNotificationChannels(listOf(
            NotificationChannel(CHANNEL_ALARM, context.getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_alarm_desc)
                setSound(alarmSound, alarmAudio)
                enableVibration(true)
                vibrationPattern = pattern
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            },
            NotificationChannel(CHANNEL_ALARM_NO_VIBRATE, context.getString(R.string.channel_alarm_quiet), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_alarm_quiet_desc)
                setSound(alarmSound, alarmAudio)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(CHANNEL_DEADLINE, context.getString(R.string.channel_deadline), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_deadline_desc)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(CHANNEL_DIGEST, context.getString(R.string.channel_digest), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_digest_desc)
            },
            NotificationChannel(CHANNEL_SUMMARY, context.getString(R.string.channel_summary), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.channel_summary_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        ))
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun post(context: Context, id: Int, notification: Notification) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun actionIntent(context: Context, action: String, id: Int): PendingIntent {
        val intent = Intent(context, NagActionReceiver::class.java)
            .setAction(action)
            .putExtra(AlarmScheduler.EXTRA_NAG_ID, id)
        return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun openNagIntent(context: Context, id: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_NAG)
            .putExtra(AlarmScheduler.EXTRA_NAG_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun addNagIntent(context: Context, requestCode: Int = 0): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_ADD_NAG)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 7_000 + requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun appIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** The alarm itself: rings (insistently) and pops a full-screen nag when locked. */
    fun showAlarm(context: Context, nag: Nag, nagCount: Int) {
        val settings = Settings.current
        val fullScreen = PendingIntent.getActivity(
            context, nag.id,
            Intent(context, AlarmActivity::class.java)
                .putExtra(AlarmScheduler.EXTRA_NAG_ID, nag.id)
                .putExtra(AlarmActivity.EXTRA_NAG_COUNT, nagCount)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = NagMessages.alarmTitle(settings.personality, nagCount)
        val body = buildString {
            append(nag.title)
            nag.dueAt?.let { append("\nDue ").append(TimeFormat.relative(context, it)) }
            if (nag.details.isNotBlank()) append("\n\n").append(nag.details)
        }
        val builder = NotificationCompat.Builder(context, if (settings.vibrate) CHANNEL_ALARM else CHANNEL_ALARM_NO_VIBRATE)
            .setSmallIcon(R.drawable.ic_stat_nagmo)
            .setColor(WidgetColors.accent(context))
            .setContentTitle(title)
            .setContentText(nag.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setTimeoutAfter(5 * 60_000L)
            .setOngoing(true)
            .addAction(R.drawable.ic_check, context.getString(R.string.action_done), actionIntent(context, NagActionReceiver.ACTION_DONE, nag.id))
            .addAction(R.drawable.ic_snooze, context.getString(R.string.action_snooze, settings.snoozeMinutes), actionIntent(context, NagActionReceiver.ACTION_SNOOZE, nag.id))
        if (nag.nagEveryMinutes > 0) {
            builder.addAction(R.drawable.ic_stop, context.getString(R.string.action_stop), actionIntent(context, NagActionReceiver.ACTION_STOP, nag.id))
        }
        val notification = builder.build().apply { flags = flags or Notification.FLAG_INSISTENT }
        post(context, nag.id, notification)
    }

    fun cancelAlarm(context: Context, id: Int) {
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(id)
        nm.cancel(id + DEADLINE_OFFSET)
    }

    fun showDeadlineWarning(context: Context, nag: Nag) {
        val due = nag.dueAt ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_DEADLINE)
            .setSmallIcon(R.drawable.ic_stat_nagmo)
            .setColor(WidgetColors.accent(context))
            .setContentTitle(NagMessages.deadlineTitle(Settings.current.personality, TimeFormat.relative(context, due)))
            .setContentText(nag.title)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openNagIntent(context, nag.id))
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, context.getString(R.string.action_done), actionIntent(context, NagActionReceiver.ACTION_DONE, nag.id))
            .build()
        post(context, nag.id + DEADLINE_OFFSET, notification)
    }

    fun showDigest(context: Context) {
        val today = NagRepository.dueToday()
        val personality = Settings.current.personality
        val style = NotificationCompat.InboxStyle()
        today.take(6).forEach { style.addLine(lineFor(context, it)) }
        if (today.size > 6) style.setSummaryText("+${today.size - 6} more")
        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(R.drawable.ic_stat_nagmo)
            .setContentTitle(NagMessages.digestTitle(personality, today.size))
            .setContentText(if (today.isEmpty()) NagMessages.emptyLine(personality) else today.joinToString(" · ") { it.title })
            .setStyle(if (today.isEmpty()) null else style)
            .setContentIntent(appIntent(context))
            .setAutoCancel(true)
            .addAction(R.drawable.ic_add, context.getString(R.string.action_add), addNagIntent(context, 1))
            .build()
        post(context, ID_DIGEST, notification)
    }

    private fun lineFor(context: Context, nag: Nag): String {
        val time = when {
            nag.isOverdue() -> "overdue"
            nag.dueAt != null -> "due " + TimeFormat.time(context, nag.dueAt)
            nag.nextAlarmAt != null -> TimeFormat.time(context, nag.nextAlarmAt!!)
            else -> ""
        }
        return if (time.isEmpty()) nag.title else "${nag.title}  ·  $time"
    }

    /**
     * A quiet, persistent notification listing today's nags. It is public so it
     * shows in full on the lock screen, acting as Nagmo's lock-screen widget on
     * phones that don't support real lock-screen widgets.
     */
    fun updateLockScreenSummary(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        if (!Settings.current.lockScreenSummary) {
            nm.cancel(ID_SUMMARY)
            return
        }
        val today = NagRepository.dueToday()
        val pending = NagRepository.pendingSorted()
        val shown = today.ifEmpty { pending.take(3) }
        val title = when {
            today.isNotEmpty() -> context.resources.getQuantityString(R.plurals.summary_today, today.size, today.size)
            pending.isNotEmpty() -> context.getString(R.string.summary_upcoming)
            else -> NagMessages.emptyLine(Settings.current.personality)
        }
        val style = NotificationCompat.InboxStyle()
        shown.take(5).forEach { style.addLine(lineFor(context, it)) }
        if (shown.size > 5) style.setSummaryText("+${shown.size - 5} more")

        val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARY)
            .setSmallIcon(R.drawable.ic_stat_nagmo)
            .setContentTitle(title)
            .setContentText(shown.joinToString(" · ") { it.title }.ifEmpty { context.getString(R.string.summary_tap_to_add) })
            .setStyle(if (shown.isEmpty()) null else style)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(appIntent(context))
            .addAction(R.drawable.ic_add, context.getString(R.string.action_add), addNagIntent(context, 2))
            .build()
        post(context, ID_SUMMARY, notification)
    }
}
