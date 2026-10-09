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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nagmo.app.alarm.AlarmScheduler
import com.nagmo.app.alarm.Notifications
import com.nagmo.app.data.Accent
import com.nagmo.app.data.Nag
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Personality
import com.nagmo.app.data.Settings
import com.nagmo.app.data.ThemeMode
import com.nagmo.app.ui.components.Group
import com.nagmo.app.ui.components.GroupDivider
import com.nagmo.app.ui.components.Mascot
import com.nagmo.app.ui.components.Mood
import com.nagmo.app.ui.components.PillTabs
import com.nagmo.app.ui.components.SectionLabel
import com.nagmo.app.ui.components.SettingRow
import com.nagmo.app.ui.components.Swatch
import com.nagmo.app.ui.components.TimePickerDialog
import com.nagmo.app.ui.theme.LocalNagmo
import com.nagmo.app.util.TimeFormat
import com.nagmo.app.widget.NagListWidget
import com.nagmo.app.widget.QuickAddWidget
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val extras = LocalNagmo.current
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // Permissions: only shown when something is missing.
            val notificationsOk = remember(resumeTick) { Notifications.canPost(context) }
            val exactOk = remember(resumeTick) { AlarmScheduler.canScheduleExact(context) }
            val fullScreenOk = remember(resumeTick) {
                Build.VERSION.SDK_INT < 34 ||
                    context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
            }
            if (!notificationsOk || !exactOk || !fullScreenOk) {
                SectionLabel("Needs your attention")
                Group {
                    if (!notificationsOk) {
                        SettingRow(Icons.Outlined.Notifications, "Allow notifications", subtitle = "Needed to ring and show your list",
                            value = "Allow", valueColor = MaterialTheme.colorScheme.primary,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    openAppNotificationSettings(context)
                                }
                            })
                    }
                    if (!exactOk && Build.VERSION.SDK_INT >= 31) {
                        SettingRow(Icons.Outlined.Alarm, "Allow exact alarms", subtitle = "Ring on the exact minute",
                            value = "Allow", valueColor = MaterialTheme.colorScheme.primary,
                            onClick = {
                                context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                            })
                    }
                    if (!fullScreenOk && Build.VERSION.SDK_INT >= 34) {
                        SettingRow(Icons.Outlined.Fullscreen, "Allow full-screen alarms", subtitle = "Show over the lock screen",
                            value = "Allow", valueColor = MaterialTheme.colorScheme.primary,
                            onClick = {
                                context.startActivity(Intent(AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")))
                            })
                    }
                }
            }

            // Appearance
            SectionLabel("Appearance")
            Group {
                Column(Modifier.padding(16.dp)) {
                    Text("Theme", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(10.dp))
                    PillTabs(ThemeMode.entries, s.themeMode, { it.label }, { m -> Settings.update { it.copy(themeMode = m) } }, Modifier.fillMaxWidth())
                }
                GroupDivider()
                Column(Modifier.padding(16.dp)) {
                    Text("Accent", style = MaterialTheme.typography.bodyLarge)
                    Text(s.accent.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Accent.entries
                            .filter { it != Accent.DYNAMIC || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
                            .forEach { a ->
                                Swatch(
                                    color = Color(if (extras.dark) a.dark else a.light),
                                    selected = s.accent == a,
                                    onClick = {
                                        Settings.update { it.copy(accent = a) }
                                        NagRepository.refreshSurfaces()
                                    },
                                    size = 30.dp,
                                )
                            }
                    }
                }
            }

            // Nagging
            SectionLabel("Nagging")
            Group {
                MenuRow(Icons.Outlined.RecordVoiceOver, "Personality", Personality.entries, s.personality, { it.label }, subtitle = s.personality.blurb) { p ->
                    Settings.update { it.copy(personality = p) }
                    NagRepository.refreshSurfaces()
                }
                GroupDivider()
                MenuRow(Icons.Outlined.Snooze, "Snooze", listOf(5, 10, 15, 30, 60), s.snoozeMinutes, { "$it min" }) { m ->
                    Settings.update { it.copy(snoozeMinutes = m) }
                }
                GroupDivider()
                MenuRow(Icons.Outlined.Event, "Deadline warning", listOf(0, 15, 60, 180, 1440), s.deadlineWarningMinutes, ::warnLabel) { m ->
                    Settings.update { it.copy(deadlineWarningMinutes = m) }
                    NagRepository.rescheduleAll()
                }
                GroupDivider()
                SettingRow(Icons.Outlined.Vibration, "Vibrate", onClick = { Settings.update { it.copy(vibrate = !it.vibrate) } }) {
                    Switch(checked = s.vibrate, onCheckedChange = { v -> Settings.update { it.copy(vibrate = v) } })
                }
                GroupDivider()
                SettingRow(Icons.Outlined.NotificationsActive, "Test the alarm", onClick = {
                    Notifications.showAlarm(context, Nag(id = TEST_NAG_ID, title = "This is what a nag looks like", details = "Tap Done to make it stop."), 1)
                })
            }

            // Daily
            SectionLabel("Daily")
            Group {
                SettingRow(Icons.Outlined.WbSunny, "Morning summary", subtitle = "A short list of what's due",
                    onClick = {
                        Settings.update { it.copy(morningDigest = !it.morningDigest) }
                        AlarmScheduler.scheduleDigest(context)
                    }) {
                    Switch(checked = s.morningDigest, onCheckedChange = { v ->
                        Settings.update { it.copy(morningDigest = v) }
                        AlarmScheduler.scheduleDigest(context)
                    })
                }
                if (s.morningDigest) {
                    GroupDivider()
                    val millis = LocalDate.now().atTime(LocalTime.of(s.digestHour, s.digestMinute))
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    SettingRow(Icons.Outlined.Schedule, "Time", value = TimeFormat.time(context, millis), onClick = { pickDigestTime = true })
                }
            }

            // Lock screen & widgets
            SectionLabel("Lock screen & widgets")
            Group {
                SettingRow(Icons.Outlined.LockOpen, "Today on lock screen", subtitle = "A quiet notification listing today's nags",
                    onClick = {
                        Settings.update { it.copy(lockScreenSummary = !it.lockScreenSummary) }
                        NagRepository.refreshSurfaces()
                    }) {
                    Switch(checked = s.lockScreenSummary, onCheckedChange = { v ->
                        Settings.update { it.copy(lockScreenSummary = v) }
                        NagRepository.refreshSurfaces()
                    })
                }
                GroupDivider()
                SettingRow(Icons.Outlined.ViewAgenda, "Add list widget", onClick = { pinWidget(context, NagListWidget::class.java) })
                GroupDivider()
                SettingRow(Icons.Outlined.Widgets, "Add quick widget", onClick = { pinWidget(context, QuickAddWidget::class.java) })
            }
            Text(
                "Add the “Add nag” tile to Quick Settings to create nags from the lock screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )

            SectionLabel("Data")
            Group {
                SettingRow(Icons.Outlined.CleaningServices, "Clear completed", onClick = { confirmClear = true })
            }

            Spacer(Modifier.height(40.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Mascot(Mood.HAPPY, 44.dp)
                Spacer(Modifier.height(10.dp))
                Text("Nagmo", style = MaterialTheme.typography.titleMedium)
                Text(
                    "We nag so you don't have to.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(40.dp))
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
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear completed?") },
            text = { Text("Finished nags will be removed. Your stats are kept.") },
            confirmButton = {
                TextButton(onClick = { NagRepository.clearCompleted(); confirmClear = false }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

const val TEST_NAG_ID = 900_000

private fun warnLabel(m: Int) = when (m) {
    0 -> "Off"
    in 1..59 -> "$m min before"
    1440 -> "1 day before"
    else -> "${m / 60} h before"
}

@Composable
private fun <T> MenuRow(
    icon: ImageVector,
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    subtitle: String? = null,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        SettingRow(icon, title, subtitle = subtitle, value = label(selected), onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.width(200.dp)) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(label(o), color = if (o == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                    onClick = { onSelect(o); open = false },
                )
            }
        }
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
        Toast.makeText(context, "Long-press your home screen, then Widgets → Nagmo", Toast.LENGTH_LONG).show()
    }
}
