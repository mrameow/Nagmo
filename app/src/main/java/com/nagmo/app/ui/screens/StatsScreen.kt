package com.nagmo.app.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nagmo.app.data.Completion
import com.nagmo.app.data.NagRepository
import com.nagmo.app.ui.components.Mascot
import com.nagmo.app.ui.components.Mood
import com.nagmo.app.ui.theme.NagMint
import com.nagmo.app.ui.theme.NagPink
import com.nagmo.app.ui.theme.NagYellow
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
        topBar = {
            TopAppBar(
                title = { Text("Your progress") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val mood = when {
                overdue > 0 -> Mood.NAGGING
                streak >= 3 || doneToday >= 3 -> Mood.PARTY
                history.isEmpty() -> Mood.SLEEPY
                else -> Mood.HAPPY
            }
            Mascot(mood, 120.dp)
            Text(
                when {
                    overdue > 0 -> "Nice numbers… but $overdue thing${if (overdue == 1) " is" else "s are"} overdue 👀"
                    streak >= 7 -> "$streak-day streak! I barely need to nag you anymore 🥹"
                    streak >= 3 -> "$streak days in a row. Unstoppable! 🔥"
                    history.isEmpty() -> "Finish your first nag to start a streak"
                    else -> "Keep going, I believe in you ✨"
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("🔥", "$streak", "day streak", NagPink, Modifier.weight(1f))
                StatTile("🏆", "$best", "best streak", NagYellow, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("✅", "$doneToday", "done today", NagMint, Modifier.weight(1f))
                StatTile("📅", "$doneWeek", "this week", NagMint, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("⏱️", "$onTime%", "on time", NagYellow, Modifier.weight(1f))
                StatTile("📝", "$pending", "still to do", NagPink, Modifier.weight(1f))
            }

            Text(
                "Last 7 days",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
            )
            val week = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val counts = week.map { d -> history.count { it.day(zone) == d } }
            val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth().height(160.dp).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    week.forEachIndexed { i, d ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text("${counts[i]}", style = MaterialTheme.typography.labelMedium)
                            Box(
                                Modifier
                                    .width(18.dp)
                                    .fillMaxHeight(0.75f * counts[i] / max + 0.02f)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (d == today) MaterialTheme.colorScheme.primary else NagYellow),
                            )
                            Text(
                                d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            if (history.isNotEmpty()) {
                Text(
                    "Recently conquered",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                )
                history.takeLast(10).reversed().forEach { c ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (c.onTime) "✅" else "🐢")
                        Spacer(Modifier.width(10.dp))
                        Text(c.title, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(
                            TimeFormat.full(context, c.at),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color, contentColor = com.nagmo.app.ui.theme.Ink),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
