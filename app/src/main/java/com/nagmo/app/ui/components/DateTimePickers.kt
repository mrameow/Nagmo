package com.nagmo.app.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Picks a date, then a time. [initial] is epoch millis (or null for "now + 1h").
 * Calls [onPicked] with the chosen epoch millis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerFlow(
    initial: Long?,
    onDismiss: () -> Unit,
    onPicked: (Long) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val start = remember {
        Instant.ofEpochMilli(initial ?: (System.currentTimeMillis() + 3_600_000L)).atZone(zone)
    }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }

    if (pickedDate == null) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = start.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val millis = dateState.selectedDateMillis
                    if (millis != null) {
                        pickedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        ) {
            DatePicker(state = dateState)
        }
    } else {
        TimePickerDialog(
            initialHour = start.hour,
            initialMinute = start.minute,
            onDismiss = onDismiss,
            onPicked = { h, m ->
                val dt = ZonedDateTime.of(pickedDate!!, LocalTime.of(h, m), zone)
                onPicked(dt.toInstant().toEpochMilli())
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onPicked: (hour: Int, minute: Int) -> Unit,
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "What time?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )
                TimePicker(state = state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onPicked(state.hour, state.minute) }) { Text("OK") }
                }
            }
        }
    }
}
