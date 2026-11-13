package com.reelflow.platform.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reelflow.platform.data.model.JobStatus
import com.reelflow.platform.data.model.VideoReelItem
import com.reelflow.platform.ui.theme.*
import java.io.File

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Slate800)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 12.sp, color = Slate400)
        }
    }
}

@Composable
fun StatusChip(status: JobStatus) {
    val (label, color) = when (status) {
        JobStatus.DRAFT -> "Draft" to Slate400
        JobStatus.AI_GENERATING -> "Generating" to AccentSky
        JobStatus.READY_FOR_REVIEW -> "Review" to AmberWarning
        JobStatus.APPROVED -> "Approved" to EmeraldGreen
        JobStatus.REJECTED -> "Rejected" to RoseError
        JobStatus.SCHEDULED -> "Scheduled" to PrimaryBlue
        JobStatus.PUBLISHING -> "Publishing" to AccentSky
        JobStatus.PUBLISHED -> "Published" to EmeraldGreen
        JobStatus.FAILED -> "Failed" to RoseError
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** Video row: thumbnail + title + page + status. */
@Composable
fun JobCard(
    job: VideoReelItem,
    onClick: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Slate800)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val thumbModel: Any? = job.localThumbnailPath?.let { File(it) }
                ?: job.thumbnailUrl
            AsyncImage(
                model = thumbModel,
                contentDescription = "Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp, 96.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    job.title.ifBlank { job.prompt.take(60) },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Text(job.targetPageName, fontSize = 12.sp, color = Slate400)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(job.status)
                    if (!job.scheduledDateTime.isNullOrBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(job.scheduledDateTime, fontSize = 11.sp, color = Slate400)
                    }
                }
            }
            Row { trailing() }
        }
    }
}
