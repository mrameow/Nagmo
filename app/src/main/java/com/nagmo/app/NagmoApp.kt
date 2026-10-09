package com.nagmo.app

import android.app.Application
import com.nagmo.app.alarm.AlarmScheduler
import com.nagmo.app.alarm.Notifications
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Settings

class NagmoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Settings.init(this)
        NagRepository.init(this)
        Notifications.createChannels(this)
        AlarmScheduler.scheduleDigest(this)
    }
}
