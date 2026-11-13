package com.reelflow.platform.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reelflow.platform.data.model.FacebookPage
import com.reelflow.platform.data.model.JobStatus
import com.reelflow.platform.viewmodel.PlatformViewModel

@Composable
fun VideoLibraryScreen(
    viewModel: PlatformViewModel,
    onOpenVideo: (String) -> Unit,
    onOpenThumbnail: (String) -> Unit
) {
    val jobs by viewModel.jobs.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val context = LocalContext.current
    var filter by remember { mutableStateOf<JobStatus?>(null) }
    var selectMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showAssignDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val page = pages.firstOrNull { it.isEnabled }
            if (page == null) {
                viewModel.showMessage("Pehle koi page connect karo")
            } else {
                viewModel.importGalleryVideo(context, uri, page)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { launcher.launch("video/*") }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Import video")
            }
            OutlinedButton(onClick = { selectMode = !selectMode; selected = emptySet() }) {
                Text(if (selectMode) "Cancel select" else "Select")
            }
            if (selectMode && selected.isNotEmpty()) {
                Button(onClick = { showAssignDialog = true }) {
                    Text("Assign (${selected.size})")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = filter == null,
                    onClick = { filter = null },
                    label = { Text("All (${jobs.size})") }
                )
            }
            for (status in JobStatus.entries) {
                val count = jobs.count { it.status == status }
                if (count > 0) {
                    item {
                        FilterChip(
                            selected = filter == status,
                            onClick = { filter = status },
                            label = { Text("${status.name} ($count)") }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val shown = jobs.filter { filter == null || it.status == filter }
        if (shown.isEmpty()) {
            Text("No videos here yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(shown) { job ->
                    Row {
                        if (selectMode) {
                            Checkbox(
                                checked = selected.contains(job.id),
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + job.id else selected - job.id
                                }
                            )
                        }
                        JobCard(
                            job,
                            onClick = {
                                if (selectMode) {
                                    selected = if (selected.contains(job.id)) selected - job.id else selected + job.id
                                } else {
                                    onOpenVideo(job.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAssignDialog) {
        AssignPageDialog(
            pages = pages.filter { it.isEnabled },
            onDismiss = { showAssignDialog = false },
            onAssign = { page ->
                viewModel.bulkReassignJobs(selected, page)
                viewModel.showMessage("${selected.size} videos assigned to ${page.name}")
                selected = emptySet()
                selectMode = false
                showAssignDialog = false
            }
        )
    }
}

@Composable
fun AssignPageDialog(
    pages: List<FacebookPage>,
    onDismiss: () -> Unit,
    onAssign: (FacebookPage) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign to page") },
        text = {
            Column {
                if (pages.isEmpty()) {
                    Text("No enabled pages. Connect an account first.")
                } else {
                    pages.take(50).forEach { page ->
                        TextButton(onClick = { onAssign(page) }) {
                            Text(page.name)
                        }
                    }
                    if (pages.size > 50) Text("…and ${pages.size - 50} more pages")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
