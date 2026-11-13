package com.reelflow.platform.data.service

import android.content.Context
import com.google.gson.Gson
import com.reelflow.platform.data.model.VideoReelItem

/**
 * On-device persistence for video jobs (plain SharedPreferences + Gson).
 *
 * Jobs must survive app restarts so scheduled publishing (WorkManager) can
 * fire even if the app was closed. No server, no account — just local JSON.
 * Tokens are NEVER stored here (they live in the encrypted SecureTokenStore).
 */
class JobStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveJobs(jobs: List<VideoReelItem>) {
        prefs.edit().putString(KEY_JOBS, gson.toJson(jobs)).apply()
    }

    fun loadJobs(): List<VideoReelItem> {
        val json = prefs.getString(KEY_JOBS, null) ?: return emptyList()
        return try {
            val arr = gson.fromJson(json, Array<VideoReelItem>::class.java)
            arr?.toList().orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadJob(jobId: String): VideoReelItem? =
        loadJobs().find { it.id == jobId }

    fun updateJob(updated: VideoReelItem) {
        val jobs = loadJobs().map { if (it.id == updated.id) updated else it }
        saveJobs(jobs)
    }

    companion object {
        private const val PREFS_NAME = "reelflow_jobs"
        private const val KEY_JOBS = "jobs_v1"
    }
}
