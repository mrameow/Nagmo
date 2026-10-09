package com.nagmo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nagmo.app.data.Nag
import com.nagmo.app.data.NagRepository
import com.nagmo.app.data.NoteColor
import com.nagmo.app.data.Priority
import com.nagmo.app.data.Repeat
import com.nagmo.app.data.Subtask
import com.nagmo.app.ui.components.ColorDot
import com.nagmo.app.ui.components.DateTimePickerFlow
import com.nagmo.app.ui.components.Mascot
import com.nagmo.app.ui.components.Mood
import com.nagmo.app.ui.components.SectionTitle
import com.nagmo.app.ui.theme.OverdueRed
import com.nagmo.app.ui.theme.surface
import com.nagmo.app.util.TimeFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

private val NAG_INTERVALS = listOf(0, 5, 10, 15, 30, 60)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditNagScreen(nagId: Int?, prefill: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val original: Nag? = remember(nagId) { nagId?.let { NagRepository.get(it) } }
    val prefillLines = remember(prefill) { prefill.trim().lines() }

    var title by remember { mutableStateOf(original?.title ?: prefillLines.firstOrNull().orEmpty()) }
    var details by remember {
        mutableStateOf(original?.details ?: prefillLines.drop(1).joinToString("\n").trim())
    }
    var remindAt by remember { mutableStateOf(original?.remindAt) }
    var dueAt by remember { mutableStateOf(original?.dueAt) }
    var priority by remember { mutableStateOf(original?.priority ?: Priority.MEDIUM) }
    var color by remember { mutableStateOf(original?.color ?: NoteColor.entries[(NagRepository.nags.size) % NoteColor.entries.size]) }
    var category by remember { mutableStateOf(original?.category.orEmpty()) }
    var repeat by remember { mutableStateOf(original?.repeat ?: Repeat.NONE) }
    var nagEvery by remember { mutableStateOf(original?.nagEveryMinutes ?: 0) }
    var pinned by remember { mutableStateOf(original?.pinned ?: false) }
    val subtasks = remember { (original?.subtasks ?: emptyList()).toMutableStateList() }
    var newSubtask by remember { mutableStateOf("") }

    var picking by remember { mutableStateOf<String?>(null) } // "remind" or "due"
    var confirmDelete by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    fun save() {
        if (title.isBlank()) {
            titleError = true
            return
        }
        if (newSubtask.isNotBlank()) {
            subtasks.add(Subtask(newSubtask.trim()))
            newSubtask = ""
        }
        val base = original ?: Nag(id = 0, title = "")
        val remindChanged = base.remindAt != remindAt
        NagRepository.upsert(
            base.copy(
                title = title.trim(),
                details = details.trim(),
                remindAt = remindAt,
                dueAt = dueAt,
                priority = priority,
                color = color,
                category = category.trim(),
                repeat = repeat,
                nagEveryMinutes = nagEvery,
                pinned = pinned,
                subtasks = subtasks.toList(),
                nextNagAt = if (remindChanged || nagEvery == 0) null else base.nextNagAt,
            )
        )
        onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (original == null) "New nag" else "Edit nag") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TextButton(onClick = ::save) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = color.surface()),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mascot(if (nagEvery > 0) Mood.NAGGING else Mood.HAPPY, 56.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (original == null) "What should I nag you about?" else "Changing plans? I'll adapt.",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(12.dp))

            // 1. The work
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = false },
                label = { Text("The work") },
                placeholder = { Text("e.g. Finish the history essay") },
                isError = titleError,
                supportingText = { if (titleError) Text("I can't nag you about nothing!") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )

            // 2. Reminder time (alarm)
            SectionTitle("⏰", "Remind me at")
            DateTimeRow(
                value = remindAt,
                emptyText = "No alarm",
                onPick = { picking = "remind" },
                onClear = { remindAt = null },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickPresets().forEach { (label, millis) ->
                    AssistChip(onClick = { remindAt = millis() }, label = { Text(label) })
                }
            }

            // 3. Deadline
            SectionTitle("🏁", "Must be finished by")
            DateTimeRow(
                value = dueAt,
                emptyText = "No deadline",
                onPick = { picking = "due" },
                onClear = { dueAt = null },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                deadlinePresets().forEach { (label, millis) ->
                    AssistChip(onClick = { dueAt = millis() }, label = { Text(label) })
                }
            }
            val r = remindAt
            val d = dueAt
            if (r != null && d != null && r > d) {
                Text(
                    "Heads-up: the alarm is after the deadline 🙈",
                    color = OverdueRed,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            // Nagging
            SectionTitle("📢", "Keep nagging until it's done")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NAG_INTERVALS.forEach { m ->
                    FilterChip(
                        selected = nagEvery == m,
                        onClick = { nagEvery = m },
                        label = { Text(if (m == 0) "Just once" else "Every $m min") },
                    )
                }
            }

            SectionTitle("🔁", "Repeat")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Repeat.entries.forEach { rp ->
                    FilterChip(selected = repeat == rp, onClick = { repeat = rp }, label = { Text(rp.label) })
                }
            }

            // 4. Specifics
            SectionTitle("📝", "Specifics")
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                placeholder = { Text("Details, links, where, who, how…") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(8.dp))
            Text("Checklist", style = MaterialTheme.typography.labelLarge)
            subtasks.forEachIndexed { i, st ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = st.done, onCheckedChange = { subtasks[i] = st.copy(done = it) })
                    Text(
                        st.text,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (st.done) TextDecoration.LineThrough else null,
                    )
                    IconButton(onClick = { subtasks.removeAt(i) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove step")
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newSubtask,
                    onValueChange = { newSubtask = it },
                    placeholder = { Text("Add a step") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newSubtask.isNotBlank()) { subtasks.add(Subtask(newSubtask.trim())); newSubtask = "" }
                    }),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                )
                IconButton(onClick = {
                    if (newSubtask.isNotBlank()) { subtasks.add(Subtask(newSubtask.trim())); newSubtask = "" }
                }) { Icon(Icons.Filled.Add, contentDescription = "Add step") }
            }

            SectionTitle("🔥", "Priority")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label) })
                }
            }

            SectionTitle("🎨", "Note colour")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NoteColor.entries.forEach { c ->
                    ColorDot(c.surface(), selected = color == c, onClick = { color = c })
                }
            }

            SectionTitle("🏷️", "Category")
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                placeholder = { Text("School, Work, Home…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            val existing = remember { NagRepository.nags.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct() }
            if (existing.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    existing.forEach { c -> AssistChip(onClick = { category = c }, label = { Text("#$c") }) }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📌  Pin to top", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = pinned, onCheckedChange = { pinned = it })
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (original == null) "Start nagging me" else "Save changes")
            }
            if (original != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!original.done) {
                        OutlinedButton(onClick = { NagRepository.complete(original.id); onClose() }, modifier = Modifier.weight(1f)) {
                            Text("Mark done ✓")
                        }
                    }
                    OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    when (picking) {
        "remind" -> DateTimePickerFlow(
            initial = remindAt,
            onDismiss = { picking = null },
            onPicked = { remindAt = it; picking = null },
        )
        "due" -> DateTimePickerFlow(
            initial = dueAt ?: remindAt?.plus(3_600_000L),
            onDismiss = { picking = null },
            onPicked = { dueAt = it; picking = null },
        )
    }

    if (confirmDelete && original != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Mascot(Mood.SLEEPY, 64.dp, animate = false) },
            title = { Text("Delete this nag?") },
            text = { Text("“${original.title}” will be gone for good. I won't nag about it ever again.") },
            confirmButton = {
                TextButton(onClick = { NagRepository.delete(original.id); onClose() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep it") } },
        )
    }
}

@Composable
private fun DateTimeRow(value: Long?, emptyText: String, onPick: () -> Unit, onClear: () -> Unit) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onPick, modifier = Modifier.weight(1f)) {
            Text(
                if (value == null) "$emptyText · tap to set"
                else TimeFormat.full(context, value) + "  (" + TimeFormat.countdown(value, now) + ")",
                color = if (value != null && value < now) OverdueRed else MaterialTheme.colorScheme.primary,
            )
        }
        if (value != null) {
            IconButton(onClick = onClear) { Icon(Icons.Filled.Close, contentDescription = "Clear") }
        }
    }
}

private fun at(daysFromToday: Long, hour: Int, minute: Int = 0): Long {
    val zone = ZoneId.systemDefault()
    return ZonedDateTime.of(LocalDate.now(zone).plusDays(daysFromToday), LocalTime.of(hour, minute), zone)
        .toInstant().toEpochMilli()
}

private fun roundedFromNow(minutes: Long): Long {
    val t = System.currentTimeMillis() + minutes * 60_000L
    return t - t % 60_000L
}

private fun quickPresets(): List<Pair<String, () -> Long>> {
    val hour = ZonedDateTime.now().hour
    return buildList<Pair<String, () -> Long>> {
        add("In 15 min" to { roundedFromNow(15) })
        add("In 1 hour" to { roundedFromNow(60) })
        if (hour < 19) add("Tonight 8 PM" to { at(0, 20) })
        add("Tomorrow 9 AM" to { at(1, 9) })
    }
}

private fun deadlinePresets(): List<Pair<String, () -> Long>> {
    val hour = ZonedDateTime.now().hour
    return buildList<Pair<String, () -> Long>> {
        if (hour < 17) add("Today 5 PM" to { at(0, 17) })
        add("Tonight 11:59 PM" to { at(0, 23, 59) })
        add("Tomorrow" to { at(1, 23, 59) })
        add("In a week" to { at(7, 23, 59) })
    }
}
