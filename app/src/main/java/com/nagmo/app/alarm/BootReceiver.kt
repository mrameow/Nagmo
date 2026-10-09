package com.nagmo.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Settings

/** Alarms are wiped on reboot and skewed by clock changes, so re-register them. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Settings.init(context)
        NagRepository.init(context)
        NagRepository.rescheduleAll()
    }
}
