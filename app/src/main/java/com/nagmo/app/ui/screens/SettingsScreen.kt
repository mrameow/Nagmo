package com.nagmo.app.ui.screens

import android.Manifest
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nagmo.app.alarm.AlarmScheduler
import com.nagmo.app.alarm.Notifications
import com.nagmo.app.data.Nag
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Personality
import com.nagmo.app.data.Settings
import com.nagmo.app.ui.components.Mascot
import com.nagmo.app.ui.components.Mood
import com.nagmo.app.ui.components.SectionTitle
import com.nagmo.app.ui.components.TimePickerDialog
import com.nagmo.app.ui.theme.OverdueRed
import com.nagmo.app.widget.NagListWidget
import com.nagmo.app.widget.QuickAddWidget
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val s by Settings.state.collectAsStateWithLifecycle()
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumeTick++
        onPauseOrDispose { }
    }
    var pickDigestTime by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        resumeTick++
        NagRepository.refreshSurfaces()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // --- Permissions -------------------------------------------------
            // Re-read permission state every time the screen resumes.
            val notificationsOk = remember(resumeTick) { Notifications.canPost(context) }
            val exactOk = remember(resumeTick) { AlarmScheduler.canScheduleExact(context) }
            val fullScreenOk = remember(resumeTick) {
                Build.VERSION.SDK_INT < 34 ||
                    context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
            }
            if (!notificationsOk || !exactOk || !fullScreenOk) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Mascot(Mood.NAGGING, 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("I can't nag properly without these:", style = MaterialTheme.typography.titleMedium)
                        }
                        if (!notificationsOk) PermissionRow("Notifications", "So I can ring and show your list") {
                            if (Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else openAppNotificationSettings(context)
                        }
                        if (!exactOk && Build.VERSION.SDK_INT >= 31) PermissionRow("Alarms & reminders", "So I ring on the exact minute") {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                            )
                        }
                        if (!fullScreenOk && Build.VERSION.SDK_INT >= 34) PermissionRow("Full-screen alarms", "So I can pop up over the lock screen") {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
                            )
                        }
                    }
                }
            }

            // --- Personality -------------------------------------------------
            SectionTitle("🗣️", "Nagging personality")
            Personality.entries.forEach { p ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { Settings.update { it.copy(personality = p) }; NagRepository.refreshSurfaces() }
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(selected = s.personality == p, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(p.label, style = MaterialTheme.typography.bodyLarge)
                        Text(p.blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    Notifications.showAlarm(
                        context,
                        Nag(id = TEST_NAG_ID, title = "This is what a nag looks like", details = "Tap Done to make me stop 😌"),
                        1,
                    )
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("🔔  Try a test nag") }

            // --- Alarms ------------------------------------------------------
            SectionTitle("⏰", "Alarms")
            Text("Snooze length", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 30, 60).forEach { m ->
                    FilterChip(
                        selected = s.snoozeMinutes == m,
                        onClick = { Settings.update { it.copy(snoozeMinutes = m) } },
                        label = { Text("$m min") },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Warn me before a deadline", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Off", 15 to "15 min", 60 to "1 hour", 180 to "3 hours", 1440 to "1 day").forEach { (m, label) ->
                    FilterChip(
                        selected = s.deadlineWarningMinutes == m,
                        onClick = {
                            Settings.update { it.copy(deadlineWarningMinutes = m) }
                            NagRepository.rescheduleAll()
                        },
                        label = { Text(label) },
                    )
                }
            }
            SwitchRow("Vibrate", "Buzz along with the alarm sound", s.vibrate) { v ->
                Settings.update { it.copy(vibrate = v) }
            }

            // --- Daily -------------------------------------------------------
            SectionTitle("☀️", "Morning digest")
            SwitchRow("Morning rundown", "A daily list of what's due", s.morningDigest) { v ->
                Settings.update { it.copy(morningDigest = v) }
                AlarmScheduler.scheduleDigest(context)
            }
            if (s.morningDigest) {
                TextButton(onClick = { pickDigestTime = true }) {
                    Text("Every day at " + LocalTime.of(s.digestHour, s.digestMinute).format(DateTimeFormatter.ofPattern("h:mm a")))
                }
            }

            // --- Lock screen & widgets ----------------------------------------
            SectionTitle("🔒", "Lock screen")
            SwitchRow(
                "Today's list on lock screen",
                "A quiet notification that shows your nags on the lock screen, with an Add button",
                s.lockScreenSummary,
            ) { v ->
                Settings.update { it.copy(lockScreenSummary = v) }
                NagRepository.refreshSurfaces()
            }
            Text(
                "Tip: add the “Add nag” tile to Quick Settings (swipe down twice → edit ✏️) to add a nag straight from the lock screen. " +
                    "Nagmo's widgets can also be placed on the lock screen on devices that support lock-screen widgets.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            SectionTitle("🧩", "Home screen widgets")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pinWidget(context, NagListWidget::class.java) }, modifier = Modifier.weight(1f)) {
                    Text("Add Nag list")
                }
                OutlinedButton(onClick = { pinWidget(context, QuickAddWidget::class.java) }, modifier = Modifier.weight(1f)) {
                    Text("Add Quick nag")
                }
            }

            // --- Housekeeping -------------------------------------------------
            SectionTitle("🧹", "Housekeeping")
            OutlinedButton(onClick = { confirmClear = true }) { Text("Clear completed nags") }

            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Mascot(Mood.HAPPY, 72.dp)
                Text("Nagmo", style = MaterialTheme.typography.titleLarge)
                Text("Personalised nagging memo", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "We nag so you don't have to.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (pickDigestTime) {
        TimePickerDialog(
            initialHour = s.digestHour,
            initialMinute = s.digestMinute,
            onDismiss = { pickDigestTime = false },
            onPicked = { h, m ->
                Settings.update { it.copy(digestHour = h, digestMinute = m) }
                AlarmScheduler.scheduleDigest(context)
                pickDigestTime = false
            },
        )
    }
    if (confirmClear) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear completed?") },
            text = { Text("Finished nags will be removed. Your stats stay.") },
            confirmButton = {
                TextButton(onClick = { NagRepository.clearCompleted(); confirmClear = false }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

const val TEST_NAG_ID = 900_000

@Composable
private fun PermissionRow(title: String, why: String, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = OverdueRed)
            Text(why, style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = onFix) { Text("Allow") }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun openAppNotificationSettings(context: Context) {
    context.startActivity(
        Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
    )
}

private fun pinWidget(context: Context, provider: Class<*>) {
    val manager = AppWidgetManager.getInstance(context)
    if (manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(ComponentName(context, provider), null, null)
    } else {
        Toast.makeText(context, "Long-press your home screen → Widgets → Nagmo", Toast.LENGTH_LONG).show()
    }
}
