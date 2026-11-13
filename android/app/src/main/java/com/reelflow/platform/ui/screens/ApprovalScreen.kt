package com.reelflow.platform.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reelflow.platform.data.model.JobStatus
import com.reelflow.platform.viewmodel.PlatformViewModel

/**
 * Approval screen: video + thumbnail + title + caption + hashtags +
 * target page together. Approve / Reject. A rejected video never publishes.
 */
@Composable
fun ApprovalScreen(viewModel: PlatformViewModel, onOpenThumbnail: (String) -> Unit) {
    val jobs by viewModel.jobs.collectAsState()
    val pending = jobs.filter { it.status == JobStatus.READY_FOR_REVIEW }
    val decided = jobs.filter { it.status == JobStatus.APPROVED || it.status == JobStatus.REJECTED }
        .sortedByDescending { it.createdAt }.take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Review & Approval", style = MaterialTheme.typography.headlineMedium)
            Text(
                "${pending.size} video(s) waiting for approval. Nothing publishes without your approval.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (pending.isEmpty()) {
            item { Text("Nothing waiting — new videos appear here when ready.") }
        } else {
            items(pending) { job ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        JobCard(job, onClick = { onOpenThumbnail(job.id) })
                        if (job.title.isNotBlank()) Text("Title: ${job.title}")
                        if (job.caption.isNotBlank()) Text("Caption: ${job.caption}")
                        if (job.hashtags.isNotEmpty()) Text(job.hashtags.joinToString(" "))
                        Text("Page: ${job.targetPageName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.approveJob(job.id) }) { Text("Approve") }
                            OutlinedButton(onClick = { viewModel.rejectJob(job.id) }) { Text("Reject") }
                            TextButton(onClick = { onOpenThumbnail(job.id) }) { Text("Thumbnail") }
                        }
                    }
                }
            }
        }
        if (decided.isNotEmpty()) {
            item { SectionTitle("Recently decided") }
            items(decided) { job ->
                JobCard(job, onClick = {})
            }
        }
    }
}
