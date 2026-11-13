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
import com.reelflow.platform.ui.navigation.Screen
import com.reelflow.platform.viewmodel.PlatformViewModel

@Composable
fun DashboardScreen(viewModel: PlatformViewModel, onNavigate: (String) -> Unit) {
    val jobs by viewModel.jobs.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val summary = viewModel.analytics()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("ReelFlow 500", style = MaterialTheme.typography.headlineMedium)
            Text(
                "${pages.size} pages • ${jobs.size} videos • ${summary.published} published",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Videos", "${summary.totalVideos}", Modifier.weight(1f))
                StatCard("Pages", "${pages.size}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Scheduled", "${summary.scheduled}", Modifier.weight(1f))
                StatCard("Published", "${summary.published}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Pending review", "${summary.pendingReview}", Modifier.weight(1f))
                StatCard("Total views", "${summary.totalViews}", Modifier.weight(1f))
            }
        }
        item {
            SectionTitle("Quick actions")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onNavigate(Screen.Create.route) }, modifier = Modifier.weight(1f)) {
                    Text("Create video")
                }
                Button(onClick = { onNavigate(Screen.Review.route) }, modifier = Modifier.weight(1f)) {
                    Text("Review (${summary.pendingReview})")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onNavigate(Screen.Schedule.route) }, modifier = Modifier.weight(1f)) {
                    Text("Schedule")
                }
                OutlinedButton(onClick = { onNavigate(Screen.Pages.route) }, modifier = Modifier.weight(1f)) {
                    Text("Pages")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onNavigate(Screen.Analytics.route) }, modifier = Modifier.weight(1f)) {
                    Text("Analytics")
                }
                OutlinedButton(onClick = { onNavigate(Screen.Settings.route) }, modifier = Modifier.weight(1f)) {
                    Text("Settings")
                }
            }
        }
        item { SectionTitle("Recent videos") }
        val recent = jobs.sortedByDescending { it.createdAt }.take(5)
        if (recent.isEmpty()) {
            item { Text("No videos yet — create one from the Create tab.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(recent) { job ->
                JobCard(job, onClick = { onNavigate(Screen.VideoDetail.routeFor(job.id)) })
            }
        }
        item {
            val rejected = jobs.count { it.status == JobStatus.REJECTED }
            if (rejected > 0) {
                Text(
                    "$rejected rejected video(s) will never publish.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
