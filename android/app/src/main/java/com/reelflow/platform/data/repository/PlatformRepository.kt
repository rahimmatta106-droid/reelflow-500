package com.reelflow.platform.data.repository

import android.content.Context
import com.reelflow.platform.data.interfaces.MetaGraphPublishingService
import com.reelflow.platform.data.model.*
import com.reelflow.platform.data.service.JobStore
import com.reelflow.platform.data.service.MockPublishingService
import com.reelflow.platform.data.service.PublishScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Publishing backend selector: simulated vs real Meta Graph API. */
enum class PublishingMode { MOCK, REAL }

/**
 * Single source of truth for pages + video jobs.
 *
 * Local-first: jobs persist on device (JobStore). Pages come from connected
 * Facebook accounts (/me/accounts); in MOCK mode 500 sample pages are shown
 * so the screens can be tried without any account connected.
 */
class PlatformRepository {
    private val _pages = MutableStateFlow<List<FacebookPage>>(emptyList())
    val pages: StateFlow<List<FacebookPage>> = _pages.asStateFlow()

    private val _jobs = MutableStateFlow<List<VideoReelItem>>(emptyList())
    val jobs: StateFlow<List<VideoReelItem>> = _jobs.asStateFlow()

    private val _logs = MutableStateFlow<List<SystemLogEntry>>(emptyList())
    val logs: StateFlow<List<SystemLogEntry>> = _logs.asStateFlow()

    private val _publishingMode = MutableStateFlow(PublishingMode.MOCK)
    val publishingMode: StateFlow<PublishingMode> = _publishingMode.asStateFlow()

    private var realPublisher: MetaGraphPublishingService? = null
    private var jobStore: JobStore? = null
    private var appContext: Context? = null
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun bindScope(externalScope: CoroutineScope) {
        scope = externalScope
    }

    /** Load persisted jobs and re-enqueue every SCHEDULED job after restart. */
    fun attachPersistence(context: Context) {
        appContext = context.applicationContext
        val store = JobStore(appContext!!)
        jobStore = store
        val saved = store.loadJobs()
        if (saved.isNotEmpty()) {
            _jobs.value = saved
            addLog("JOB_STORE", "Loaded ${saved.size} saved videos from device")
        }
        _jobs.value.filter { it.status == JobStatus.SCHEDULED }.forEach { job ->
            PublishScheduler.enqueue(appContext!!, job)
        }
    }

    fun refreshFromDisk() {
        val saved = jobStore?.loadJobs() ?: return
        _jobs.value = saved
    }

    private fun persist() {
        jobStore?.saveJobs(_jobs.value)
    }

    fun attachRealPublisher(service: MetaGraphPublishingService) {
        realPublisher = service
        addLog("REAL_PUBLISH", "Real Meta Graph API publisher attached (v21.0, direct from device)")
    }

    fun setPublishingMode(mode: PublishingMode) {
        _publishingMode.value = mode
        if (mode == PublishingMode.MOCK && _pages.value.isEmpty()) {
            _pages.value = MockPublishingService.generateSampleFacebookPages()
            addLog("PAGES", "Demo mode: 500 sample pages loaded")
        }
        if (mode == PublishingMode.REAL && _pages.value.any { it.id.startsWith("page_") }) {
            // Drop demo pages when switching to real publishing.
            _pages.value = _pages.value.filterNot { it.id.startsWith("page_") }
        }
        addLog("PUBLISH_MODE", "Publishing mode set to $mode")
    }

    // ------------------------------------------------------------------
    // Pages
    // ------------------------------------------------------------------

    /** Pages returned by a Facebook login (/me/accounts). Replaces demo pages. */
    fun upsertPagesFromLogin(pages: List<FacebookPage>) {
        val existing = _pages.value.filterNot { it.id.startsWith("page_") }
        val byGraphId = existing.associateBy { it.pageId }.toMutableMap()
        for (p in pages) byGraphId[p.pageId] = p
        _pages.value = byGraphId.values.toList()
        addLog("PAGES", "${pages.size} pages connected from Facebook login")
    }

    fun addPage(page: FacebookPage) {
        _pages.value = _pages.value + page
        addLog("PAGE_ADD", "Page added: ${page.name}")
    }

    fun removePage(pageId: String) {
        _pages.value = _pages.value.filterNot { it.id == pageId }
        addLog("PAGE_REMOVE", "Page removed: $pageId")
    }

    fun updatePageStatus(pageId: String, isEnabled: Boolean) {
        _pages.value = _pages.value.map { p ->
            if (p.id == pageId) p.copy(isEnabled = isEnabled) else p
        }
        addLog("PAGE_TOGGLE", "Page $pageId toggled to enabled=$isEnabled")
    }

    fun bulkUpdatePages(pageIds: Set<String>, enable: Boolean) {
        _pages.value = _pages.value.map { p ->
            if (pageIds.contains(p.id)) p.copy(isEnabled = enable) else p
        }
        addLog("PAGES_BULK", "Bulk updated ${pageIds.size} pages to enabled=$enable")
    }

    fun verifyPageToken(page: FacebookPage, onResult: (Result<TokenHealth>) -> Unit) {
        val publisher = realPublisher
        if (publisher == null) {
            onResult(Result.failure(Exception("Real publisher not attached")))
            return
        }
        scope.launch(Dispatchers.IO) {
            val result = publisher.verifyPageToken(page.pageId)
            scope.launch(Dispatchers.Main) {
                result.onSuccess { health ->
                    _pages.value = _pages.value.map {
                        if (it.id == page.id) it.copy(tokenHealth = health) else it
                    }
                }
                onResult(result)
            }
        }
    }

    // ------------------------------------------------------------------
    // Jobs (videos)
    // ------------------------------------------------------------------

    fun addJob(job: VideoReelItem) {
        _jobs.value = listOf(job) + _jobs.value
        persist()
        addLog("JOB_CREATED", "New video for ${job.targetPageName}: ${job.title.ifBlank { job.prompt.take(40) }}")
    }

    fun updateJob(job: VideoReelItem) {
        _jobs.value = _jobs.value.map { j -> if (j.id == job.id) job else j }
        persist()
    }

    fun deleteJob(jobId: String) {
        val job = _jobs.value.find { it.id == jobId } ?: return
        appContext?.let { PublishScheduler.cancel(it, jobId) }
        _jobs.value = _jobs.value.filterNot { it.id == jobId }
        persist()
        addLog("JOB_DELETE", "Video deleted: ${job.title.ifBlank { jobId }}")
    }

    /** Move one video to a different target Page (single assignment). */
    fun reassignJob(jobId: String, page: FacebookPage) {
        _jobs.value = _jobs.value.map { j ->
            if (j.id == jobId) {
                j.copy(
                    targetPageId = page.id,
                    targetPageName = page.name,
                    targetGraphPageId = page.pageId
                )
            } else j
        }
        persist()
        addLog("ASSIGN", "Video $jobId assigned to page ${page.name}")
    }

    /** Move several videos to one target Page at once (bulk assignment). */
    fun bulkReassignJobs(jobIds: Set<String>, page: FacebookPage) {
        if (jobIds.isEmpty()) return
        _jobs.value = _jobs.value.map { j ->
            if (jobIds.contains(j.id)) {
                j.copy(
                    targetPageId = page.id,
                    targetPageName = page.name,
                    targetGraphPageId = page.pageId
                )
            } else j
        }
        persist()
        addLog("ASSIGN_BULK", "${jobIds.size} videos assigned to page ${page.name}")
    }

    /** Approve a video in review. Scheduling is a separate step. */
    fun approveJob(jobId: String, comment: String = "") {
        _jobs.value = _jobs.value.map { j ->
            if (j.id == jobId) {
                j.copy(status = JobStatus.APPROVED, reviewStatus = "Approved", reviewerComment = comment)
            } else j
        }
        persist()
        addLog("REVIEW", "Video $jobId APPROVED")
    }

    /** Reject a video. A rejected video is NEVER published, ever. */
    fun rejectJob(jobId: String, comment: String = "") {
        val job = _jobs.value.find { it.id == jobId } ?: return
        appContext?.let { PublishScheduler.cancel(it, jobId) }
        _jobs.value = _jobs.value.map { j ->
            if (j.id == jobId) {
                j.copy(
                    status = JobStatus.REJECTED,
                    reviewStatus = "Rejected",
                    reviewerComment = comment,
                    scheduledDateTime = null
                )
            } else j
        }
        persist()
        addLog("REVIEW", "Video $jobId REJECTED — will never publish")
    }

    /**
     * Schedule an APPROVED job for a date/time (ISO-8601 local date-time).
     * Enqueues WorkManager so it publishes at that time even if the app
     * was closed. Only APPROVED jobs can be scheduled.
     */
    fun scheduleJob(jobId: String, dateTimeIso: String, timezone: String): Boolean {
        val job = _jobs.value.find { it.id == jobId } ?: return false
        if (job.status != JobStatus.APPROVED) {
            addLog("SCHEDULE_BLOCKED", "Video $jobId must be APPROVED before scheduling (is ${job.status})", "WARN")
            return false
        }
        val updated = job.copy(
            scheduledDateTime = dateTimeIso,
            scheduledTimezone = timezone,
            status = JobStatus.SCHEDULED
        )
        _jobs.value = _jobs.value.map { if (it.id == jobId) updated else it }
        persist()
        appContext?.let { PublishScheduler.enqueue(it, updated) }
        addLog("SCHEDULE", "Video $jobId scheduled for $dateTimeIso ($timezone)")
        return true
    }

    /** Take a job off the schedule (back to APPROVED) and cancel its work. */
    fun cancelSchedule(jobId: String) {
        val job = _jobs.value.find { it.id == jobId } ?: return
        if (job.status != JobStatus.SCHEDULED) return
        _jobs.value = _jobs.value.map {
            if (it.id == jobId) it.copy(status = JobStatus.APPROVED, scheduledDateTime = null) else it
        }
        persist()
        appContext?.let { PublishScheduler.cancel(it, jobId) }
        addLog("SCHEDULE_CANCEL", "Video $jobId unscheduled (back to APPROVED)")
    }

    /** Publish gate: REJECTED never publishes; drafts/generating/publishing/published refused. */
    private fun isPublishable(job: VideoReelItem): Boolean {
        if (job.status == JobStatus.REJECTED) {
            addLog("PUBLISH_BLOCKED", "Rejected video ${job.id} will NEVER publish.", "WARN")
            return false
        }
        if (job.status in setOf(JobStatus.DRAFT, JobStatus.AI_GENERATING, JobStatus.PUBLISHING, JobStatus.PUBLISHED)) {
            addLog("PUBLISH_BLOCKED", "Video ${job.id} cannot publish from status ${job.status}.", "WARN")
            return false
        }
        return true
    }

    /** Publishes a video using the active [PublishingMode]. */
    fun publishVideo(jobId: String) {
        val job = _jobs.value.find { it.id == jobId } ?: return
        if (!isPublishable(job)) return
        val publisher = realPublisher
        if (_publishingMode.value != PublishingMode.REAL || publisher == null) {
            publishVideoMock(jobId)
            return
        }
        val page = _pages.value.find { it.id == job.targetPageId }
        val graphPageId = job.targetGraphPageId?.takeIf { it.isNotBlank() }
            ?: page?.pageId ?: job.targetPageId

        _jobs.value = _jobs.value.map {
            if (it.id == jobId) it.copy(status = JobStatus.PUBLISHING) else it
        }
        persist()
        addLog("REAL_PUBLISH", "Uploading video to page $graphPageId via Meta Graph API (3-phase)...")

        scope.launch(Dispatchers.IO) {
            val result = publisher.uploadAndPublishReel(
                pageId = graphPageId,
                videoUriOrUrl = job.localUri ?: job.videoUrl.orEmpty(),
                title = job.title,
                caption = job.caption,
                hashtags = job.hashtags,
                idempotencyKey = "reelflow500_${job.id}"
            )
            scope.launch(Dispatchers.Main) {
                result
                    .onSuccess { record ->
                        _jobs.value = _jobs.value.map {
                            if (it.id == jobId) it.copy(status = JobStatus.PUBLISHED, publishRecord = record) else it
                        }
                        persist()
                        addLog("REAL_PUBLISH", "SUCCESS: video live at ${record.postUrl}")
                    }
                    .onFailure { e ->
                        _jobs.value = _jobs.value.map {
                            if (it.id == jobId) it.copy(status = JobStatus.FAILED) else it
                        }
                        persist()
                        addLog("REAL_PUBLISH", "FAILED: ${e.message}", level = "ERROR")
                    }
            }
        }
    }

    private fun publishVideoMock(jobId: String) {
        val job = _jobs.value.find { it.id == jobId } ?: return
        if (!isPublishable(job)) return
        val mockReelId = "reel_${System.currentTimeMillis()}"
        _jobs.value = _jobs.value.map { j ->
            if (j.id == jobId) {
                j.copy(
                    status = JobStatus.PUBLISHED,
                    publishRecord = PublishRecord(
                        facebookReelId = mockReelId,
                        postUrl = "https://facebook.com/reel/$mockReelId",
                        initialViews = 0,
                        initialReach = 0
                    )
                )
            } else j
        }
        persist()
        addLog("MOCK_PUBLISH", "Simulated publishing of video $jobId (demo mode)")
    }

    fun analytics(): AnalyticsSummary = AnalyticsSummary.fromJobs(_jobs.value)

    fun addLog(tag: String, message: String, level: String = "INFO") {
        val entry = SystemLogEntry(level = level, tag = tag, message = message)
        _logs.value = listOf(entry) + _logs.value.take(199)
    }
}
