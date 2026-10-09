package com.nagmo.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nagmo.app.alarm.NagMessages
import com.nagmo.app.data.Nag
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Priority
import com.nagmo.app.data.Repeat
import com.nagmo.app.data.Settings
import com.nagmo.app.ui.components.EmptyState
import com.nagmo.app.ui.components.Mascot
import com.nagmo.app.ui.components.Mood
import com.nagmo.app.ui.components.NagCheck
import com.nagmo.app.ui.components.Tag
import com.nagmo.app.ui.theme.OverdueRed
import com.nagmo.app.ui.theme.onSurface
import com.nagmo.app.ui.theme.surface
import com.nagmo.app.util.TimeFormat
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class Filter(val label: String) {
    TODO("To do"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    DONE("Done"),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    filter: Filter,
    onFilterChange: (Filter) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Int) -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
) {
    val store by NagRepository.store.collectAsStateWithLifecycle()
    val settings by Settings.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val now = System.currentTimeMillis()
    val pending = NagRepository.pendingSorted(store.nags)
    val overdueCount = pending.count { it.isOverdue(now) }
    val todayList = NagRepository.dueToday(store.nags)
    val categories = store.nags.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct().sorted()

    val zone = ZoneId.systemDefault()
    val endOfToday = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val shown = when (filter) {
        Filter.TODO -> pending
        Filter.TODAY -> todayList
        Filter.UPCOMING -> pending.filter { (it.dueAt ?: it.nextAlarmAt ?: 0L) >= endOfToday }
        Filter.OVERDUE -> pending.filter { it.isOverdue(now) }
        Filter.DONE -> store.nags.filter { it.done }.sortedByDescending { it.completedAt ?: 0L }
    }.filter { n ->
        (category == null || n.category.trim() == category) &&
            (query.isBlank() || n.title.contains(query, true) || n.details.contains(query, true) ||
                n.category.contains(query, true))
    }

    val mood = when {
        overdueCount > 0 -> Mood.NAGGING
        pending.isEmpty() && store.nags.any { it.done } -> Mood.PARTY
        pending.isEmpty() -> Mood.SLEEPY
        else -> Mood.HAPPY
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nag me") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Mascot(mood, 64.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Nagmo", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            when {
                                overdueCount > 0 -> "$overdueCount overdue. I'm watching you 👀"
                                todayList.isNotEmpty() -> "${todayList.size} thing${if (todayList.size == 1) "" else "s"} on today's list"
                                else -> "We nag so you don't have to."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                        Icon(if (searching) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onStats) { Icon(Icons.Outlined.EmojiEvents, contentDescription = "Stats") }
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
                }
            }
            if (searching) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search your nags") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Filter.entries) { f ->
                        val count = when (f) {
                            Filter.TODO -> pending.size
                            Filter.TODAY -> todayList.size
                            Filter.OVERDUE -> overdueCount
                            else -> 0
                        }
                        FilterChip(
                            selected = filter == f,
                            onClick = { onFilterChange(f) },
                            label = { Text(if (count > 0) "${f.label} · $count" else f.label) },
                        )
                    }
                }
            }
            if (categories.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { c ->
                            FilterChip(
                                selected = category == c,
                                onClick = { category = if (category == c) null else c },
                                label = { Text("#$c") },
                            )
                        }
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    when {
                        query.isNotBlank() -> EmptyState(Mood.SLEEPY, "No matches", "Nagmo looked everywhere. Even under the fridge.")
                        filter == Filter.DONE -> EmptyState(Mood.SLEEPY, "Nothing done yet", "Tick something off and I'll throw a party 🎉")
                        filter == Filter.OVERDUE -> EmptyState(Mood.PARTY, "Nothing overdue!", "Look at you, keeping up with life ✨")
                        pending.isEmpty() -> EmptyState(Mood.SLEEPY, "No nags yet", NagMessages.emptyLine(settings.personality) + "\nTap “Nag me” to add one.")
                        else -> EmptyState(Mood.HAPPY, "Nothing here", "Try another filter")
                    }
                }
            }
            items(shown, key = { it.id }) { nag ->
                NagCard(
                    nag = nag,
                    now = now,
                    modifier = Modifier.animateItem(),
                    onClick = { onOpen(nag.id) },
                    onToggleDone = {
                        if (nag.done) {
                            NagRepository.reopen(nag.id)
                        } else {
                            val before = nag
                            NagRepository.complete(nag.id)
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                val result = snackbar.showSnackbar(
                                    message = NagMessages.doneCheer(settings.personality),
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) NagRepository.upsert(before)
                            }
                        }
                    },
                    onToggleSubtask = { i -> NagRepository.toggleSubtask(nag.id, i) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NagCard(
    nag: Nag,
    now: Long,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onToggleSubtask: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bg = nag.color.surface()
    val fg = nag.color.onSurface()
    val tilt = ((nag.id * 37) % 5 - 2) * 0.35f
    val overdue = nag.isOverdue(now)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { rotationZ = tilt }
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            NagCheck(checked = nag.done, onToggle = onToggleDone, tint = if (overdue) OverdueRed else fg)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        nag.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (nag.done) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (nag.pinned) Icon(Icons.Filled.PushPin, contentDescription = "Pinned", modifier = Modifier.size(18.dp))
                    if (nag.priority == Priority.HIGH) Text(" 🔥")
                }
                if (nag.details.isNotBlank()) {
                    Text(
                        nag.details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = fg.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                val visibleSubtasks = nag.subtasks.withIndex().filter { !nag.done }.take(3)
                visibleSubtasks.forEach { (i, st) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp).clickable { onToggleSubtask(i) },
                    ) {
                        Text(if (st.done) "☑" else "☐", color = fg)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            st.text,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (st.done) TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val chipBg = fg.copy(alpha = 0.10f)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (nag.done) {
                        nag.completedAt?.let { Tag("✅ " + TimeFormat.full(context, it), chipBg, fg) }
                    } else {
                        nag.nextAlarmAt?.let {
                            val label = if (nag.nextNagAt != null) "📢 next nag " else "⏰ "
                            Tag(label + if (it > now) TimeFormat.countdown(it, now) else TimeFormat.full(context, it), chipBg, fg)
                        }
                        nag.dueAt?.let {
                            if (overdue) Tag("⚠ overdue " + TimeFormat.countdown(it, now).removeSuffix(" ago"), OverdueRed, Color.White)
                            else Tag("🏁 " + TimeFormat.full(context, it), chipBg, fg)
                        }
                    }
                    if (nag.repeat != Repeat.NONE) Tag("🔁 " + nag.repeat.label, chipBg, fg)
                    if (nag.nagEveryMinutes > 0) Tag("📢 every ${nag.nagEveryMinutes}m", chipBg, fg)
                    if (nag.subtasks.isNotEmpty()) {
                        val (d, t) = nag.subtaskProgress
                        Tag("☑ $d/$t", chipBg, fg)
                    }
                    if (nag.category.isNotBlank()) Tag("#" + nag.category.trim(), chipBg, fg)
                }
            }
        }
    }
}
