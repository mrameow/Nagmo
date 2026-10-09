package com.nagmo.app.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.nagmo.app.data.NagRepository
import com.nagmo.app.ui.MainActivity

/** Quick Settings tile: reachable from the lock screen's pull-down shade. */
class QuickAddTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        NagRepository.init(this)
        val count = NagRepository.dueToday().size
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (count == 0) "Add a nag" else "$count today"
            }
            updateTile()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACTION_ADD_NAG)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val open = Runnable {
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
        if (isLocked) unlockAndRun(open) else open.run()
    }
}
