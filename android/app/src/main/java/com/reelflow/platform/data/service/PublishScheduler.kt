package com.reelflow.platform.data.service

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.reelflow.platform.data.model.VideoReelItem
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Schedules a job to publish at its saved time via WorkManager.
 *
 * WorkManager survives app restarts and device reboots (its own queue is
 * persisted), so a scheduled Reel goes out even if the app was closed.
 * Each job gets one unique work item ("publish_<jobId>") — re-scheduling
 * replaces the old one, cancelling removes it.
 */
object PublishScheduler {

    const val KEY_JOB_ID = "job_id"

    /** Delay in ms until [scheduledDateTimeIso] in [timezoneId]; 0 = due now. */
    fun delayUntil(scheduledDateTimeIso: String?, timezoneId: String?): Long {
        if (scheduledDateTimeIso.isNullOrBlank()) return 0L
        return try {
            val zone = runCatching { ZoneId.of(timezoneId?.substringBefore(" ") ?: "UTC") }
                .getOrDefault(ZoneId.of("UTC"))
            val target = LocalDateTime.parse(scheduledDateTimeIso, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
            (target - System.currentTimeMillis()).coerceAtLeast(0L)
        } catch (e: Exception) {
            0L
        }
    }

    /** Enqueue (or re-enqueue) the publish work for a scheduled job. */
    fun enqueue(context: Context, job: VideoReelItem) {
        val delay = delayUntil(job.scheduledDateTime, job.scheduledTimezone)
        val request = OneTimeWorkRequestBuilder<ScheduleWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(KEY_JOB_ID, job.id).build())
            .addTag("reelflow_publish")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(job.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Cancel the scheduled publish for a job (e.g. user unscheduled it). */
    fun cancel(context: Context, jobId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(jobId))
    }

    private fun workName(jobId: String) = "publish_$jobId"
}
