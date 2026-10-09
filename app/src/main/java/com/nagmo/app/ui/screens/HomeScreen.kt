package com.nagmo.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nagmo.app.alarm.NagMessages
import com.nagmo.app.data.Nag
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.Priority
import com.nagmo.app.data.Repeat
import com.nagmo.app.data.Settings
import com.nagmo.app.ui.components.Dot
import com.nagmo.app.ui.components.EmptyState
import com.nagmo.app.ui.components.Mascot
import com.nagmo.app.ui.components.Mood
import com.nagmo.app.ui.components.NagCheck
import com.nagmo.app.ui.components.SectionLabel
import com.nagmo.app.ui.theme.LocalNagmo
import com.nagmo.app.ui.theme.dot
import com.nagmo.app.util.TimeFormat
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Filter(val label: String) {
    TODO("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    DONE("Done"),
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

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
    val zone = ZoneId.systemDefault()
    val endOfToday = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val pending = NagRepository.pendingSorted(store.nags)
    val overdue = pending.filter { it.isOverdue(now) }
    val today = NagRepository.dueToday(store.nags)
    val categories = store.nags.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct().sorted()

    fun matches(n: Nag) = (category == null || n.category.trim() == category) &&
        (query.isBlank() || n.title.contains(query, true) || n.details.contains(query, true) ||
            n.category.contains(query, true))

    fun isLater(n: Nag) = (n.dueAt ?: n.nextAlarmAt ?: 0L) >= endOfToday

    // Sections shown for the current filter.
    val sections: List<Pair<String?, List<Nag>>> = when (filter) {
        Filter.TODO -> listOf(
            "Overdue" to overdue,
            "Today" to today.filterNot { it.isOverdue(now) },
            "Upcoming" to pending.filter { isLater(it) && !it.isOverdue(now) && it !in today },
            "Anytime" to pending.filter { it.dueAt == null && it.nextAlarmAt == null },
        )
        Filter.TODAY -> listOf(null to today)
        Filter.UPCOMING -> listOf(null to pending.filter { isLater(it) })
        Filter.OVERDUE -> listOf(null to overdue)
        Filter.DONE -> listOf(null to store.nags.filter { it.done }.sortedByDescending { it.completedAt ?: 0L })
    }.map { (title, list) -> title to list.filter(::matches) }.filter { it.second.isNotEmpty() }

    val mood = when {
        overdue.isNotEmpty() -> Mood.NAGGING
        pending.isEmpty() && store.nags.any { it.done } -> Mood.PARTY
        pending.isEmpty() -> Mood.SLEEPY
        else -> Mood.HAPPY
    }
    val summary = when {
        overdue.isNotEmpty() -> "${overdue.size} overdue · ${today.size} today"
        today.isNotEmpty() -> "${today.size} ${if (today.size == 1) "thing" else "things"} to do today"
        pending.isNotEmpty() -> "Nothing due today · ${pending.size} coming up"
        else -> NagMessages.emptyLine(settings.personality)
    }

    fun completeWithUndo(nag: Nag) {
        val before = nag
        NagRepository.complete(nag.id)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(NagMessages.doneCheer(settings.personality), "Undo", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) NagRepository.upsert(before)
        }
    }

    fun deleteWithUndo(nag: Nag) {
        NagRepository.delete(nag.id)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar("Deleted “${nag.title}”", "Undo", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) NagRepository.upsert(nag)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp),
            ) { Icon(Icons.Outlined.Add, contentDescription = "New nag") }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 104.dp,
                start = 20.dp,
                end = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                        Icon(if (searching) Icons.Outlined.Close else Icons.Outlined.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onStats) { Icon(Icons.Outlined.BarChart, contentDescription = "Progress") }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(greeting(), style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(4.dp))
                        Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Mascot(mood, 60.dp)
                }
            }
            item {
                AnimatedVisibility(searching) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(Filter.entries) { f ->
                        val count = when (f) {
                            Filter.TODO -> pending.size
                            Filter.TODAY -> today.size
                            Filter.OVERDUE -> overdue.size
                            else -> 0
                        }
                        Pill(
                            text = f.label,
                            count = count.takeIf { it > 0 },
                            selected = filter == f,
                            onClick = { onFilterChange(f) },
                        )
                    }
                }
            }
            if (categories.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { c ->
                            Pill(text = "#$c", count = null, selected = category == c, small = true,
                                onClick = { category = if (category == c) null else c })
                        }
                    }
                }
            }

            if (sections.isEmpty()) {
                item {
                    when {
                        query.isNotBlank() -> EmptyState(Mood.SLEEPY, "No matches", "Try a different word.")
                        filter == Filter.DONE -> EmptyState(Mood.SLEEPY, "Nothing finished yet", "Completed nags will show up here.")
                        filter == Filter.OVERDUE -> EmptyState(Mood.PARTY, "Nothing overdue", "You're on top of things.")
                        pending.isEmpty() -> EmptyState(Mood.SLEEPY, "All clear", "Tap + to add something to be nagged about.")
                        else -> EmptyState(Mood.HAPPY, "Nothing here", "Try another filter.")
                    }
                }
            }
            sections.forEach { (title, list) ->
                if (title != null) item(key = "h_$title") { SectionLabel(title, Modifier.animateItem()) }
                items(list, key = { it.id }) { nag ->
                    SwipeableNag(
                        nag = nag,
                        modifier = Modifier.animateItem(),
                        onSwipeDone = { if (nag.done) NagRepository.reopen(nag.id) else completeWithUndo(nag) },
                        onSwipeDelete = { deleteWithUndo(nag) },
                    ) {
                        NagRow(
                            nag = nag,
                            now = now,
                            onClick = { onOpen(nag.id) },
                            onToggle = { if (nag.done) NagRepository.reopen(nag.id) else completeWithUndo(nag) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, count: Int?, selected: Boolean, onClick: () -> Unit, small: Boolean = false) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant, label = "pill",
    )
    val fg = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = if (small) 12.dp else 14.dp, vertical = if (small) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text("$count", style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.6f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNag(
    nag: Nag,
    onSwipeDone: () -> Unit,
    onSwipeDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                // Spring back: repeating nags stay in the list after being completed.
                SwipeToDismissBoxValue.StartToEnd -> { onSwipeDone(); false }
                SwipeToDismissBoxValue.EndToStart -> { onSwipeDelete(); true }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            val dir = state.dismissDirection
            val color = when (dir) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                SwipeToDismissBoxValue.EndToStart -> LocalNagmo.current.danger
                else -> Color.Transparent
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (dir == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                when (dir) {
                    SwipeToDismissBoxValue.StartToEnd -> Icon(
                        if (nag.done) Icons.AutoMirrored.Outlined.Undo else Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    SwipeToDismissBoxValue.EndToStart -> Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = Color.White)
                    else -> {}
                }
            }
        },
    ) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NagRow(nag: Nag, now: Long, onClick: () -> Unit, onToggle: () -> Unit) {
    val context = LocalContext.current
    val extras = LocalNagmo.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val overdue = nag.isOverdue(now)

    val meta = buildAnnotatedString {
        val parts = mutableListOf<() -> Unit>()
        if (nag.done) {
            nag.completedAt?.let { parts += { append("Done " + TimeFormat.full(context, it)) } }
        } else {
            nag.nextAlarmAt?.let {
                parts += { append(if (nag.nextNagAt != null) "Next nag " + TimeFormat.countdown(it, now) else TimeFormat.full(context, it)) }
            }
            nag.dueAt?.let {
                parts += {
                    if (overdue) withStyle(SpanStyle(color = extras.danger)) { append("Overdue " + TimeFormat.countdown(it, now).removeSuffix(" ago")) }
                    else append("Due " + TimeFormat.full(context, it))
                }
            }
        }
        if (nag.repeat != Repeat.NONE) parts += { append(nag.repeat.label) }
        if (nag.subtasks.isNotEmpty()) parts += { val (d, t) = nag.subtaskProgress; append("$d/$t") }
        if (nag.category.isNotBlank()) parts += { append("#" + nag.category.trim()) }
        parts.forEachIndexed { i, p -> if (i > 0) append("  ·  "); p() }
    }

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, extras.hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            NagCheck(checked = nag.done, onToggle = onToggle, color = if (overdue) extras.danger else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(
                    nag.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (nag.done) muted else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (nag.done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (nag.pinned) Icon(Icons.Outlined.PushPin, contentDescription = "Pinned", tint = muted, modifier = Modifier.size(16.dp).padding(start = 2.dp))
            if (nag.priority == Priority.HIGH) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.Flag, contentDescription = "Urgent", tint = extras.danger, modifier = Modifier.size(16.dp))
            }
            nag.color.dot()?.let {
                Spacer(Modifier.width(10.dp))
                Dot(it, 9.dp)
            }
        }
    }
}
