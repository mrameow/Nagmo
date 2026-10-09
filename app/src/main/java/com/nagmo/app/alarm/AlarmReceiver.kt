package com.nagmo.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Settings

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NagRepository.init(context)
        Settings.init(context)
        when (intent.action) {
            AlarmScheduler.ACTION_REMIND -> {
                val id = intent.getIntExtra(AlarmScheduler.EXTRA_NAG_ID, -1)
                val nag = NagRepository.get(id) ?: return
                if (nag.done) return
                Notifications.showAlarm(context, nag, NagCounter.increment(context, id))
                NagRepository.onAlarmRang(id)
            }
            AlarmScheduler.ACTION_DEADLINE -> {
                val id = intent.getIntExtra(AlarmScheduler.EXTRA_NAG_ID, -1)
                val nag = NagRepository.get(id) ?: return
                if (!nag.done) Notifications.showDeadlineWarning(context, nag)
            }
            AlarmScheduler.ACTION_DIGEST -> {
                Notifications.showDigest(context)
                AlarmScheduler.scheduleDigest(context)
            }
        }
    }
}

/** Counts how many times in a row a nag has rung, so re-nags can escalate. */
object NagCounter {
    private fun prefs(context: Context) = context.getSharedPreferences("nag_counter", Context.MODE_PRIVATE)

    fun increment(context: Context, id: Int): Int {
        val n = prefs(context).getInt(id.toString(), 0) + 1
        prefs(context).edit().putInt(id.toString(), n).apply()
        return n
    }

    fun reset(context: Context, id: Int) {
        prefs(context).edit().remove(id.toString()).apply()
    }
}
