package com.reelflow.platform.data.model

import java.util.UUID

/** Job lifecycle. REJECTED is terminal: a rejected video is NEVER published. */
enum class JobStatus {
    DRAFT,
    AI_GENERATING,
    READY_FOR_REVIEW,
    APPROVED,
    REJECTED,
    SCHEDULED,
    PUBLISHING,
    PUBLISHED,
    FAILED
}

enum class TokenHealth {
    ACTIVE,
    EXPIRING_SOON,
    EXPIRED
}

/** Veo model choice. Standard = best quality, Fast = cheaper + quicker. */
enum class VeoModel(val modelId: String, val label: String, val approxCostPer8s: String) {
    STANDARD_30("veo-3.0-generate-001", "Veo 3.0 Standard", "\$6.00 / 8s"),
    FAST_30("veo-3.0-fast-generate-001", "Veo 3.0 Fast", "\$3.20 / 8s"),
    STANDARD_31("veo-3.1-generate-001", "Veo 3.1 Standard", "Veo 3.1 pricing"),
    FAST_31("veo-3.1-fast-generate-001", "Veo 3.1 Fast", "Veo 3.1 pricing");

    companion object {
        fun fromModelId(id: String): VeoModel =
            entries.firstOrNull { it.modelId == id } ?: STANDARD_30
    }
}

data class FacebookPage(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val pageId: String,
    val category: String = "",
    val niche: String = "",
    val followers: Int = 0,
    val isAuthorized: Boolean = true,
    val isEnabled: Boolean = true,
    val tokenHealth: TokenHealth = TokenHealth.ACTIVE,
    val tokenExpiryDays: Int = 45,
    val defaultTimezone: String = "America/New_York",
    val dailyPublishLimit: Int = 3,
    val postsPublishedToday: Int = 0,
    val customPromptGuidance: String = "",
    val defaultHashtags: List<String> = emptyList()
)

data class VideoReelItem(
    val id: String = UUID.randomUUID().toString(),
    val targetPageId: String,
    val targetPageName: String,
    /** Graph (numeric) Page ID used by the Meta API; falls back to targetPageId. */
    val targetGraphPageId: String? = null,
    val prompt: String,
    val negativePrompt: String = "",
    val durationSeconds: Int = 8,
    val aspectRatio: String = "9:16",
    val resolution: String = "1080p",
    val veoModel: VeoModel = VeoModel.STANDARD_30,
    val videoUrl: String? = null,
    val localUri: String? = null,
    val thumbnailUrl: String? = null,
    /** Local cover thumbnail: frame extracted from the video file (filesDir). */
    val localThumbnailPath: String? = null,
    /** Timestamp (microseconds) of the chosen thumbnail frame inside the video. */
    val thumbnailFrameUs: Long? = null,
    val status: JobStatus = JobStatus.DRAFT,
    val progressPercent: Int = 0,
    val title: String = "",
    val caption: String = "",
    val hashtags: List<String> = listOf("#Reels", "#Trending", "#Viral", "#Creator", "#Explore"),
    val alternativeTitles: List<String> = emptyList(),
    val scheduledDateTime: String? = null, // ISO-8601 local date-time
    val scheduledTimezone: String = "America/New_York",
    val reviewStatus: String = "Not Sent", // Not Sent, Pending, Approved, Rejected
    val reviewerComment: String = "",
    val publishRecord: PublishRecord? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class PublishRecord(
    val facebookReelId: String,
    val postUrl: String,
    val publishedAt: Long = System.currentTimeMillis(),
    val initialViews: Int = 0,
    val initialReach: Int = 0
)

data class GalleryMediaItem(
    val id: String = UUID.randomUUID().toString(),
    val filename: String,
    val durationText: String,
    val sizeText: String,
    val resolutionText: String,
    val previewThumbnail: String,
    val localPath: String
)

data class SystemLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // INFO, WARN, ERROR
    val tag: String,
    val message: String,
    val details: String? = null
)

/** Simple on-device analytics, computed from jobs + publish records. */
data class AnalyticsSummary(
    val totalVideos: Int = 0,
    val published: Int = 0,
    val scheduled: Int = 0,
    val pendingReview: Int = 0,
    val rejected: Int = 0,
    val failed: Int = 0,
    val totalViews: Int = 0,
    val totalReach: Int = 0,
    val publishedByPage: Map<String, Int> = emptyMap()
) {
    companion object {
        fun fromJobs(jobs: List<VideoReelItem>): AnalyticsSummary {
            val published = jobs.filter { it.status == JobStatus.PUBLISHED }
            return AnalyticsSummary(
                totalVideos = jobs.size,
                published = published.size,
                scheduled = jobs.count { it.status == JobStatus.SCHEDULED },
                pendingReview = jobs.count { it.status == JobStatus.READY_FOR_REVIEW },
                rejected = jobs.count { it.status == JobStatus.REJECTED },
                failed = jobs.count { it.status == JobStatus.FAILED },
                totalViews = published.sumOf { it.publishRecord?.initialViews ?: 0 },
                totalReach = published.sumOf { it.publishRecord?.initialReach ?: 0 },
                publishedByPage = published.groupingBy { it.targetPageName }.eachCount()
            )
        }
    }
}
