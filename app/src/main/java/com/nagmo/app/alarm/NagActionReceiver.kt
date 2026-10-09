package com.nagmo.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Settings

/** Handles the Done / Snooze / Stop buttons on nag notifications. */
class NagActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NagRepository.init(context)
        Settings.init(context)
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_NAG_ID, -1)
        if (id < 0) return
        when (intent.action) {
            ACTION_DONE -> {
                NagRepository.complete(id)
                Toast.makeText(context, NagMessages.doneCheer(Settings.current.personality), Toast.LENGTH_SHORT).show()
            }
            ACTION_SNOOZE -> NagRepository.snooze(id, Settings.current.snoozeMinutes)
            ACTION_STOP -> {
                NagRepository.stopNagging(id)
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.nagmo.app.action.DONE"
        const val ACTION_SNOOZE = "com.nagmo.app.action.SNOOZE"
        const val ACTION_STOP = "com.nagmo.app.action.STOP"
    }
}
