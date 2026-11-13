package com.reelflow.platform.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reelflow.platform.data.model.JobStatus
import com.reelflow.platform.data.model.VideoReelItem
import com.reelflow.platform.viewmodel.PlatformViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Scheduling Calendar: approved videos get a publish date/time;
 * scheduled videos show their time and can be cancelled.
 * Only APPROVED videos can be scheduled.
 */
@Composable
fun ScheduleScreen(viewModel: PlatformViewModel) {
    val jobs by viewModel.jobs.collectAsState()
    val approved = jobs.filter { it.status == JobStatus.APPROVED }
    val scheduled = jobs.filter { it.status == JobStatus.SCHEDULED }
        .sortedBy { it.scheduledDateTime }

    var schedulingJob by remember { mutableStateOf<VideoReelItem?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Scheduling", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Approved videos can be scheduled. WorkManager publishes them at that time, even if the app is closed.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { SectionTitle("Scheduled (${scheduled.size})") }
        if (scheduled.isEmpty()) {
            item { Text("Nothing scheduled yet.") }
        } else {
            items(scheduled) { job ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        JobCard(job, onClick = {})
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.cancelSchedule(job.id) }) {
                                Text("Cancel schedule")
                            }
                        }
                    }
                }
            }
        }
        item { SectionTitle("Approved — ready to schedule (${approved.size})") }
        if (approved.isEmpty()) {
            item { Text("No approved videos waiting.") }
        } else {
            items(approved) { job ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        JobCard(job, onClick = {})
                        Button(onClick = { schedulingJob = job }) { Text("Pick date & time") }
                    }
                }
            }
        }
    }

    schedulingJob?.let { job ->
        SchedulePickerDialog(
            job = job,
            onDismiss = { schedulingJob = null },
            onConfirm = { dateTimeIso ->
                viewModel.scheduleJob(job.id, dateTimeIso, job.scheduledTimezone)
                schedulingJob = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchedulePickerDialog(
    job: VideoReelItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val timeState = rememberTimePickerState(initialHour = 19, initialMinute = 40, is24Hour = true)
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule: ${job.title.ifBlank { "video" }}") },
        text = {
            Column {
                if (!showTimePicker) {
                    DatePicker(state = dateState)
                    TextButton(onClick = { showTimePicker = true }) { Text("Next: pick time") }
                } else {
                    TimePicker(state = timeState)
                    TextButton(onClick = { showTimePicker = false }) { Text("Back: pick date") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dateMillis = dateState.selectedDateMillis ?: System.currentTimeMillis()
                val date = LocalDate.ofEpochDay(dateMillis / 86400000)
                val time = LocalTime.of(timeState.hour, timeState.minute)
                val iso = LocalDateTime.of(date, time).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                onConfirm(iso)
            }) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
