package com.reelflow.platform.data.service

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Cover thumbnails for videos — extracted frames, no paid service.
 *
 * Each video gets up to 3 candidate frames (25% / 50% / 75% of duration)
 * saved as JPEGs under filesDir/thumbnails. The middle frame is the
 * default cover; the user can pick a different frame in the Review screen.
 * Remote (http) sample videos are skipped — their thumbnailUrl is used.
 */
class ThumbnailService(private val context: Context) {

    private val thumbDir: File
        get() = File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }

    /** Extract up to 3 candidate frames. Returns local JPEG paths (may be empty). */
    suspend fun extractFrames(videoPath: String): List<String> = withContext(Dispatchers.IO) {
        val file = File(videoPath)
        if (!file.exists()) return@withContext emptyList()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            if (durationMs <= 0L) return@withContext emptyList()
            val fractions = listOf(0.25, 0.5, 0.75)
            val base = "${file.nameWithoutExtension}_${System.currentTimeMillis()}"
            fractions.mapIndexedNotNull { index, fraction ->
                val timeUs = (durationMs * 1000 * fraction).toLong()
                val bitmap = try {
                    retriever.getFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                } catch (e: Exception) {
                    null
                } ?: return@mapIndexedNotNull null
                val outFile = File(thumbDir, "${base}_$index.jpg")
                try {
                    FileOutputStream(outFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    if (!bitmap.isRecycled) bitmap.recycle()
                    outFile.absolutePath
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    /** Single default cover (middle frame). Null when extraction fails. */
    suspend fun extractDefaultThumbnail(videoPath: String): String? {
        val frames = extractFrames(videoPath)
        // Middle frame is the default cover; the other two stay on disk as options.
        return frames.getOrNull(1) ?: frames.firstOrNull()
    }
}
