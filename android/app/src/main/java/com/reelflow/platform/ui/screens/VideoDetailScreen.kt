package com.reelflow.platform.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.reelflow.platform.data.model.JobStatus
import com.reelflow.platform.viewmodel.PlatformViewModel

/** Video detail: playback + editable title/caption/hashtags + page + actions. */
@Composable
fun VideoDetailScreen(
    viewModel: PlatformViewModel,
    jobId: String,
    onOpenThumbnail: (String) -> Unit,
    onBack: () -> Unit
) {
    val jobs by viewModel.jobs.collectAsState()
    val job = jobs.find { it.id == jobId }
    val context = LocalContext.current

    if (job == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Video not found.")
            Button(onClick = onBack) { Text("Back") }
        }
        return
    }

    var title by remember(job.id) { mutableStateOf(job.title) }
    var caption by remember(job.id) { mutableStateOf(job.caption) }
    var hashtags by remember(job.id) { mutableStateOf(job.hashtags.joinToString(" ")) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val source = job.localUri ?: job.videoUrl
            if (!source.isNullOrBlank()) {
                setMediaItem(MediaItem.fromUri(source))
                prepare()
            }
        }
    }
    DisposableEffect(job.id) {
        onDispose { player.release() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(job.title.ifBlank { "Video" }, style = MaterialTheme.typography.headlineSmall)
            Row {
                StatusChip(job.status)
                Spacer(Modifier.width(8.dp))
                Text(job.targetPageName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            if (job.localUri != null || job.videoUrl != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(420.dp)
                )
            } else {
                Text("No video file yet — still generating or failed.")
            }
        }
        item {
            SectionTitle("Details")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4
            )
            OutlinedTextField(
                value = hashtags,
                onValueChange = { hashtags = it },
                label = { Text("Hashtags (space separated)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                viewModel.updateJob(
                    job.copy(
                        title = title,
                        caption = caption,
                        hashtags = hashtags.split(" ", "\n").map { it.trim() }.filter { it.isNotBlank() }
                    )
                )
                viewModel.showMessage("Details saved")
            }) { Text("Save details") }
        }
        item {
            SectionTitle("Thumbnail")
            OutlinedButton(onClick = { onOpenThumbnail(job.id) }) { Text("Change thumbnail") }
            job.publishRecord?.let { record ->
                Text("Published: ${record.postUrl}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!job.scheduledDateTime.isNullOrBlank()) {
                Text("Scheduled: ${job.scheduledDateTime} (${job.scheduledTimezone})")
            }
        }
        item {
            SectionTitle("Actions")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (job.status == JobStatus.READY_FOR_REVIEW) {
                    Button(onClick = { viewModel.approveJob(job.id) }) { Text("Approve") }
                    OutlinedButton(onClick = { viewModel.rejectJob(job.id) }) { Text("Reject") }
                }
                if (job.status == JobStatus.APPROVED || job.status == JobStatus.FAILED) {
                    Button(onClick = { viewModel.publishVideo(job.id) }) { Text("Publish now") }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete video") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this video?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteJob(job.id)
                    showDeleteConfirm = false
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}
