package com.reelflow.platform.data.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reelflow.platform.data.model.JobStatus

/**
 * Fires at a job's scheduled time and publishes its Reel through the
 * existing Meta Graph API path (same 3-phase upload as a manual publish).
 *
 * Safety: a REJECTED job is never published, and a job that is no longer
 * SCHEDULED (unscheduled / already published) is skipped. On failure the
 * job is marked FAILED in the local store so the user can re-schedule it —
 * the worker never retries in a loop.
 */
class ScheduleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(PublishScheduler.KEY_JOB_ID) ?: return Result.success()
        val store = JobStore(applicationContext)
        val job = store.loadJob(jobId) ?: return Result.success()

        // Hard guards — rejected jobs never publish, ever.
        if (job.status == JobStatus.REJECTED) return Result.success()
        if (job.status != JobStatus.SCHEDULED) return Result.success()

        val publisher = RealMetaPublishingService(applicationContext)
        val videoSource = job.localUri ?: job.videoUrl.orEmpty()
        if (videoSource.isBlank()) {
            store.updateJob(job.copy(status = JobStatus.FAILED))
            return Result.success()
        }
        val graphPageId = job.targetGraphPageId?.takeIf { it.isNotBlank() } ?: job.targetPageId
        val result = publisher.uploadAndPublishReel(
            pageId = graphPageId,
            videoUriOrUrl = videoSource,
            title = job.title,
            caption = job.caption,
            hashtags = job.hashtags,
            idempotencyKey = "reelflow_${job.id}"
        )
        return result.fold(
            onSuccess = { record ->
                store.updateJob(job.copy(status = JobStatus.PUBLISHED, publishRecord = record))
                Result.success()
            },
            onFailure = {
                store.updateJob(job.copy(status = JobStatus.FAILED))
                Result.success()
            }
        )
    }
}
