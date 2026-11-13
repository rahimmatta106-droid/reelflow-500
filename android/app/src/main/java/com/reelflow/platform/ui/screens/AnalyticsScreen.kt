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
import com.reelflow.platform.viewmodel.PlatformViewModel

/** Basic on-device analytics, computed from jobs + publish records. */
@Composable
fun AnalyticsScreen(viewModel: PlatformViewModel) {
    val jobs by viewModel.jobs.collectAsState()
    val summary = viewModel.analytics()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Analytics", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Counts from videos on this device. Live reach/views come from the publish record.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Total videos", "${summary.totalVideos}", Modifier.weight(1f))
                StatCard("Published", "${summary.published}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Scheduled", "${summary.scheduled}", Modifier.weight(1f))
                StatCard("Pending review", "${summary.pendingReview}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Rejected", "${summary.rejected}", Modifier.weight(1f))
                StatCard("Failed", "${summary.failed}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Total views", "${summary.totalViews}", Modifier.weight(1f))
                StatCard("Total reach", "${summary.totalReach}", Modifier.weight(1f))
            }
        }
        item { SectionTitle("Published by page") }
        if (summary.publishedByPage.isEmpty()) {
            item { Text("Nothing published yet.") }
        } else {
            items(summary.publishedByPage.entries.toList()) { (page, count) ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(page)
                        Text("$count")
                    }
                }
            }
        }
        item { SectionTitle("Recent jobs") }
        items(jobs.sortedByDescending { it.createdAt }.take(10)) { job ->
            JobCard(job, onClick = {})
        }
    }
}
