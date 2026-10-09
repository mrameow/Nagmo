package com.nagmo.app.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.nagmo.app.alarm.AlarmScheduler
import com.nagmo.app.alarm.NagMessages
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Settings
import com.nagmo.app.ui.MainActivity

/** Invisible trampoline for taps on widget list items. */
class WidgetActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NagRepository.init(this)
        Settings.init(this)
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_NAG_ID, -1)
        when (intent.getStringExtra(EXTRA_ACTION)) {
            ACTION_DONE -> {
                if (id > 0) {
                    NagRepository.complete(id)
                    Toast.makeText(this, NagMessages.doneCheer(Settings.current.personality), Toast.LENGTH_SHORT).show()
                }
            }
            else -> startActivity(
                Intent(this, MainActivity::class.java)
                    .setAction(MainActivity.ACTION_OPEN_NAG)
                    .putExtra(AlarmScheduler.EXTRA_NAG_ID, id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_ACTION = "widget_action"
        const val ACTION_OPEN = "open"
        const val ACTION_DONE = "done"
    }
}
