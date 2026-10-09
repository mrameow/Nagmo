package com.nagmo.app.ui

import android.app.KeyguardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nagmo.app.alarm.AlarmScheduler
import com.nagmo.app.alarm.NagMessages
import com.nagmo.app.alarm.Notifications
import com.nagmo.app.data.Nag
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Settings
import com.nagmo.app.ui.components.Mascot
import com.nagmo.app.ui.components.Mood
import com.nagmo.app.ui.theme.NagmoTheme
import com.nagmo.app.ui.theme.SyncSystemBars
import com.nagmo.app.ui.theme.LocalNagmo
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nagmo.app.util.TimeFormat
import kotlinx.coroutines.delay

/** Full-screen "alarm" shown over the lock screen when a nag rings. */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        NagRepository.init(this)
        Settings.init(this)
        render()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render()
    }

    private fun render() {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_NAG_ID, -1)
        val count = intent.getIntExtra(EXTRA_NAG_COUNT, 1)
        val nag = NagRepository.get(id)
            ?: Nag(id = id, title = "This is what a nag looks like", details = "Tap Done to make me stop 😌")
        val title = NagMessages.alarmTitle(Settings.current.personality, count)
        setContent {
            NagmoTheme {
                SyncSystemBars()
                AlarmScreen(
                    nag = nag,
                    headline = title,
                    snoozeMinutes = Settings.current.snoozeMinutes,
                    onDone = {
                        Notifications.cancelAlarm(this, id)
                        if (NagRepository.get(id) != null) NagRepository.complete(id)
                        finishAndRemoveTask()
                    },
                    onSnooze = {
                        Notifications.cancelAlarm(this, id)
                        if (NagRepository.get(id) != null) NagRepository.snooze(id, Settings.current.snoozeMinutes)
                        finishAndRemoveTask()
                    },
                    onStop = {
                        Notifications.cancelAlarm(this, id)
                        if (NagRepository.get(id) != null) NagRepository.stopNagging(id)
                        finishAndRemoveTask()
                    },
                    onOpen = {
                        Notifications.cancelAlarm(this, id)
                        val km = getSystemService(KeyguardManager::class.java)
                        val open = {
                            startActivity(
                                android.content.Intent(this, MainActivity::class.java)
                                    .setAction(MainActivity.ACTION_OPEN_NAG)
                                    .putExtra(AlarmScheduler.EXTRA_NAG_ID, id)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                            finishAndRemoveTask()
                        }
                        if (km.isKeyguardLocked) {
                            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                                override fun onDismissSucceeded() = open()
                            })
                        } else {
                            open()
                        }
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_NAG_COUNT = "nag_count"
    }
}

@Composable
private fun AlarmScreen(
    nag: Nag,
    headline: String,
    snoozeMinutes: Int,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    onOpen: () -> Unit,
) {
    val context = LocalContext.current
    val extras = LocalNagmo.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(32.dp))
            Text(TimeFormat.time(context, now), style = MaterialTheme.typography.displaySmall, color = muted)
            Spacer(Modifier.height(40.dp))
            Mascot(Mood.NAGGING, 112.dp)
            Spacer(Modifier.height(28.dp))
            Text(headline.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(nag.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            nag.dueAt?.let {
                Text(
                    (if (it < now) "Was due " else "Due ") + TimeFormat.relative(context, it),
                    color = if (it < now) extras.danger else muted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (nag.details.isNotBlank()) {
                Text(
                    nag.details,
                    color = muted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 5,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            if (nag.subtasks.isNotEmpty()) {
                val (d, t) = nag.subtaskProgress
                Text("$d of $t steps done", color = muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
            }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("Done", style = MaterialTheme.typography.titleMedium) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(18.dp)) {
                    Text("Snooze $snoozeMinutes min", color = MaterialTheme.colorScheme.onSurface)
                }
                OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(18.dp)) {
                    Text("Open", color = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (nag.nagEveryMinutes > 0) {
                TextButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                    Text("Stop nagging for now", color = muted)
                }
            }
        }
    }
}
