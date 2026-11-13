package com.reelflow.platform.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.reelflow.platform.viewmodel.PlatformViewModel
import java.io.File

/**
 * Thumbnail Editor: shows candidate frames extracted from the video
 * (25% / 50% / 75% of duration). Tap a frame to make it the cover.
 * The middle frame is the default cover.
 */
@Composable
fun ThumbnailEditorScreen(viewModel: PlatformViewModel, jobId: String, onBack: () -> Unit) {
    val jobs by viewModel.jobs.collectAsState()
    val job = jobs.find { it.id == jobId }
    var frames by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(jobId) {
        val j = jobs.find { it.id == jobId }
        if (j != null) {
            frames = viewModel.extractThumbnailFrames(j)
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Thumbnail Editor", style = MaterialTheme.typography.headlineMedium)
        if (job == null) {
            Text("Video not found.")
            Button(onClick = onBack) { Text("Back") }
            return@Column
        }
        Text(job.title.ifBlank { job.prompt.take(60) })
        Text("Page: ${job.targetPageName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        SectionTitle("Current cover")
        job.localThumbnailPath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = "Current cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(180.dp, 320.dp).clip(RoundedCornerShape(12.dp))
            )
        } ?: Text("No cover yet — extract frames below.")
        Spacer(Modifier.height(12.dp))
        SectionTitle("Pick a frame (tap to set as cover)")
        if (loading) {
            CircularProgressIndicator()
        } else if (frames.isEmpty()) {
            Text("No frames extracted — the video file may be remote or unreadable.")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(frames) { frame ->
                    val selected = job.localThumbnailPath == frame
                    AsyncImage(
                        model = File(frame),
                        contentDescription = "Candidate frame",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp, 213.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = if (selected) Color.Green else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.setThumbnail(job, frame)
                                viewModel.showMessage("Cover thumbnail set")
                            }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}
