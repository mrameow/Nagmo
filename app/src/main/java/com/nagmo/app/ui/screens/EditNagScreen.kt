package com.nagmo.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
import com.nagmo.app.ui.components.DateTimePickerFlow
import com.nagmo.app.ui.components.Group
import com.nagmo.app.ui.components.GroupDivider
import com.nagmo.app.ui.components.Mascot
import com.nagmo.app.ui.components.Mood
import com.nagmo.app.ui.components.NagCheck
import com.nagmo.app.ui.components.SectionLabel
import com.nagmo.app.ui.components.SettingRow
import com.nagmo.app.ui.components.Swatch
import com.nagmo.app.ui.theme.LocalNagmo
import com.nagmo.app.ui.theme.dot
import com.nagmo.app.util.TimeFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

private val NAG_INTERVALS = listOf(0, 5, 10, 15, 30, 60)

private fun nagLabel(m: Int) = if (m == 0) "Off" else "Every $m min"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditNagScreen(nagId: Int?, prefill: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val extras = LocalNagmo.current
    val original: Nag? = remember(nagId) { nagId?.let { NagRepository.get(it) } }
    val prefillLines = remember(prefill) { prefill.trim().lines() }

    var title by remember { mutableStateOf(original?.title ?: prefillLines.firstOrNull().orEmpty()) }
    var details by remember { mutableStateOf(original?.details ?: prefillLines.drop(1).joinToString("\n").trim()) }
    var remindAt by remember { mutableStateOf(original?.remindAt) }
    var dueAt by remember { mutableStateOf(original?.dueAt) }
    var priority by remember { mutableStateOf(original?.priority ?: Priority.MEDIUM) }
    var color by remember { mutableStateOf(original?.color ?: NoteColor.NONE) }
    var category by remember { mutableStateOf(original?.category.orEmpty()) }
    var repeat by remember { mutableStateOf(original?.repeat ?: Repeat.NONE) }
    var nagEvery by remember { mutableStateOf(original?.nagEveryMinutes ?: 0) }
    var pinned by remember { mutableStateOf(original?.pinned ?: false) }
    val subtasks = remember { (original?.subtasks ?: emptyList()).toMutableStateList() }
    var newSubtask by remember { mutableStateOf("") }

    var sheet by remember { mutableStateOf<String?>(null) }   // "remind" | "due"
    var picker by remember { mutableStateOf<String?>(null) }  // "remind" | "due"
    var confirmDelete by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    fun addSubtask() {
        if (newSubtask.isNotBlank()) {
            subtasks.add(Subtask(newSubtask.trim()))
            newSubtask = ""
        }
    }

    fun save() {
        if (title.isBlank()) {
            titleError = true
            return
        }
        addSubtask()
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

    val now = System.currentTimeMillis()
    fun whenText(t: Long?) = t?.let { TimeFormat.full(context, it) } ?: "None"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Close") }
                },
                actions = {
                    Button(onClick = ::save, modifier = Modifier.padding(end = 12.dp)) { Text("Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // The work
            PlainField(
                value = title,
                onValueChange = { title = it; titleError = false },
                placeholder = if (titleError) "Give it a name first" else "What needs doing?",
                style = MaterialTheme.typography.headlineSmall,
                placeholderColor = if (titleError) extras.danger else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(10.dp))
            // Specifics
            PlainField(
                value = details,
                onValueChange = { details = it },
                placeholder = "Add specifics",
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                singleLine = false,
            )

            // Checklist
            Spacer(Modifier.height(12.dp))
            subtasks.forEachIndexed { i, st ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 0.dp)) {
                    NagCheck(checked = st.done, onToggle = { subtasks[i] = st.copy(done = !st.done) }, modifier = Modifier.padding(end = 4.dp))
                    Text(
                        st.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (st.done) TextDecoration.LineThrough else null,
                        color = if (st.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = { subtasks.removeAt(i) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove step", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                PlainField(
                    value = newSubtask,
                    onValueChange = { newSubtask = it },
                    placeholder = "Add a step",
                    style = MaterialTheme.typography.bodyLarge,
                    imeAction = ImeAction.Done,
                    onImeAction = ::addSubtask,
                    modifier = Modifier.weight(1f),
                )
            }

            // Schedule
            SectionLabel("Schedule")
            Group {
                SettingRow(
                    Icons.Outlined.Alarm, "Remind me",
                    value = whenText(remindAt),
                    valueColor = if (remindAt != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { sheet = "remind" },
                )
                GroupDivider()
                SettingRow(
                    Icons.Outlined.Event, "Deadline",
                    value = whenText(dueAt),
                    valueColor = when {
                        dueAt == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        dueAt!! < now -> extras.danger
                        else -> MaterialTheme.colorScheme.primary
                    },
                    onClick = { sheet = "due" },
                )
                GroupDivider()
                ChoiceRow(Icons.Outlined.NotificationsActive, "Keep nagging", NAG_INTERVALS, nagEvery, ::nagLabel) { nagEvery = it }
                GroupDivider()
                ChoiceRow(Icons.Outlined.Repeat, "Repeat", Repeat.entries, repeat, { if (it == Repeat.NONE) "Never" else it.label }) { repeat = it }
            }
            val r = remindAt
            val d = dueAt
            if (r != null && d != null && r > d) {
                Text(
                    "The reminder is after the deadline.",
                    color = extras.danger,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }

            // Organise
            SectionLabel("Organise")
            Group {
                ChoiceRow(Icons.Outlined.Flag, "Priority", Priority.entries, priority, { it.label }) { priority = it }
                GroupDivider()
                SettingRow(Icons.AutoMirrored.Outlined.Label, "Category") {
                    PlainField(
                        value = category,
                        onValueChange = { category = it },
                        placeholder = "None",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        ),
                        modifier = Modifier.width(140.dp),
                    )
                }
                GroupDivider()
                SettingRow(Icons.Outlined.Palette, "Colour", value = color.label)
                FlowRow(
                    Modifier.padding(start = 52.dp, end = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    NoteColor.entries.forEach { c -> Swatch(c.dot(), selected = color == c, onClick = { color = c }, size = 26.dp) }
                }
                GroupDivider()
                SettingRow(Icons.Outlined.PushPin, "Pin to top", onClick = { pinned = !pinned }) {
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }

            if (original != null) {
                Spacer(Modifier.height(24.dp))
                Group {
                    if (!original.done) {
                        SettingRow(Icons.Outlined.CheckCircle, "Mark as done", onClick = { NagRepository.complete(original.id); onClose() })
                        GroupDivider()
                    }
                    SettingRow(Icons.Outlined.DeleteOutline, "Delete", onClick = { confirmDelete = true })
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }

    sheet?.let { which ->
        WhenSheet(
            title = if (which == "remind") "Remind me" else "Deadline",
            presets = if (which == "remind") remindPresets() else deadlinePresets(),
            hasValue = (if (which == "remind") remindAt else dueAt) != null,
            onPreset = { t -> if (which == "remind") remindAt = t else dueAt = t; sheet = null },
            onCustom = { sheet = null; picker = which },
            onClear = { if (which == "remind") remindAt = null else dueAt = null; sheet = null },
            onDismiss = { sheet = null },
        )
    }

    when (picker) {
        "remind" -> DateTimePickerFlow(remindAt, onDismiss = { picker = null }, onPicked = { remindAt = it; picker = null })
        "due" -> DateTimePickerFlow(dueAt ?: remindAt?.plus(3_600_000L), onDismiss = { picker = null }, onPicked = { dueAt = it; picker = null })
    }

    if (confirmDelete && original != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Mascot(Mood.SLEEPY, 56.dp, animate = false) },
            title = { Text("Delete this nag?") },
            text = { Text("“${original.title}” will be removed.") },
            confirmButton = {
                TextButton(onClick = { NagRepository.delete(original.id); onClose() }) { Text("Delete", color = extras.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** Borderless text field for a clean, document-like editor. */
@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    placeholderColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
) {
    val textStyle = if (style.color == androidx.compose.ui.graphics.Color.Unspecified) style.copy(color = MaterialTheme.colorScheme.onSurface) else style
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        singleLine = singleLine,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, style = textStyle.copy(color = placeholderColor))
                inner()
            }
        },
    )
}

@Composable
private fun <T> ChoiceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        SettingRow(icon, title, value = label(selected), onClick = { open = true })
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhenSheet(
    title: String,
    presets: List<Pair<String, () -> Long>>,
    hasValue: Boolean,
    onPreset: (Long) -> Unit,
    onCustom: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            presets.forEach { (label, at) ->
                val t = remember { at() }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPreset(t) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(TimeFormat.full(context, t), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = LocalNagmo.current.hairline)
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onCustom).padding(horizontal = 24.dp, vertical = 16.dp),
            ) { Text("Pick date & time…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary) }
            if (hasValue) {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onClear).padding(horizontal = 24.dp, vertical = 16.dp),
                ) { Text("Remove", style = MaterialTheme.typography.bodyLarge, color = LocalNagmo.current.danger) }
            }
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

private fun remindPresets(): List<Pair<String, () -> Long>> {
    val hour = ZonedDateTime.now().hour
    return buildList<Pair<String, () -> Long>> {
        add("In 15 minutes" to { roundedFromNow(15) })
        add("In 1 hour" to { roundedFromNow(60) })
        if (hour < 19) add("This evening" to { at(0, 20) })
        add("Tomorrow morning" to { at(1, 9) })
        add("Next week" to { at(7, 9) })
    }
}

private fun deadlinePresets(): List<Pair<String, () -> Long>> {
    val hour = ZonedDateTime.now().hour
    return buildList<Pair<String, () -> Long>> {
        if (hour < 17) add("End of workday" to { at(0, 17) })
        add("Tonight" to { at(0, 23, 59) })
        add("Tomorrow" to { at(1, 23, 59) })
        add("In a week" to { at(7, 23, 59) })
    }
}
