package com.nagmo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nagmo.app.data.Completion
import com.nagmo.app.data.NagRepository
import com.nagmo.app.ui.components.Group
import com.nagmo.app.ui.components.GroupDivider
import com.nagmo.app.ui.components.Mascot
import com.nagmo.app.ui.components.Mood
import com.nagmo.app.ui.components.SectionLabel
import com.nagmo.app.ui.theme.LocalNagmo
import com.nagmo.app.util.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private fun Completion.day(zone: ZoneId): LocalDate = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

/** Consecutive days (ending today, or yesterday if nothing yet today) with at least one completion. */
private fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
    var d = if (today in days) today else today.minusDays(1)
    var streak = 0
    while (d in days) {
        streak++
        d = d.minusDays(1)
    }
    return streak
}

private fun bestStreak(days: Set<LocalDate>): Int {
    var best = 0
    for (d in days) {
        if (d.minusDays(1) in days) continue
        var len = 0
        var x = d
        while (x in days) { len++; x = x.plusDays(1) }
        if (len > best) best = len
    }
    return best
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store by NagRepository.store.collectAsStateWithLifecycle()
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val history = store.history
    val days = history.map { it.day(zone) }.toSet()
    val streak = currentStreak(days, today)
    val best = bestStreak(days)
    val doneToday = history.count { it.day(zone) == today }
    val doneWeek = history.count { !it.day(zone).isBefore(today.minusDays(6)) }
    val onTime = if (history.isEmpty()) 0 else history.count { it.onTime } * 100 / history.size
    val pending = store.nags.count { !it.done }
    val overdue = store.nags.count { it.isOverdue() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("$streak", style = MaterialTheme.typography.displaySmall)
                    Text(
                        when {
                            overdue > 0 -> "day streak · $overdue overdue"
                            streak >= 7 -> "day streak. Impressive."
                            streak >= 3 -> "day streak. Keep it going."
                            history.isEmpty() -> "day streak. Finish a nag to start one."
                            else -> "day streak"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Mascot(
                    when {
                        overdue > 0 -> Mood.NAGGING
                        streak >= 3 || doneToday >= 3 -> Mood.PARTY
                        history.isEmpty() -> Mood.SLEEPY
                        else -> Mood.HAPPY
                    },
                    64.dp,
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("$doneToday", "Today", Modifier.weight(1f))
                Stat("$doneWeek", "This week", Modifier.weight(1f))
                Stat("$best", "Best streak", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("$onTime%", "On time", Modifier.weight(1f))
                Stat("$pending", "To do", Modifier.weight(1f))
                Stat("${history.size}", "All time", Modifier.weight(1f))
            }

            SectionLabel("Last 7 days")
            val week = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val counts = week.map { d -> history.count { it.day(zone) == d } }
            val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
            Group {
                Row(
                    Modifier.fillMaxWidth().height(170.dp).padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    week.forEachIndexed { i, d ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            if (counts[i] > 0) {
                                Text("${counts[i]}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                            }
                            Box(
                                Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(0.7f * counts[i] / max + 0.03f)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        if (d == today) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    ),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (d == today) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (history.isNotEmpty()) {
                SectionLabel("Recently finished")
                Group {
                    val recent = history.takeLast(8).reversed()
                    recent.forEachIndexed { i, c ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(c.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                (if (c.onTime) "" else "Late · ") + TimeFormat.full(context, c.at),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (c.onTime) MaterialTheme.colorScheme.onSurfaceVariant else LocalNagmo.current.danger,
                            )
                        }
                        if (i < recent.lastIndex) GroupDivider()
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, LocalNagmo.current.hairline),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
