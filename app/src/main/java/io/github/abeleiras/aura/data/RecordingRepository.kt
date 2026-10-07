package io.github.abeleiras.aura.data

import android.media.MediaMetadataRetriever
import io.github.abeleiras.aura.data.db.RecordingDao
import io.github.abeleiras.aura.data.db.RecordingEntity
import io.github.abeleiras.aura.domain.recording.AudioQuality
import io.github.abeleiras.aura.domain.recording.RecordingNaming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/** A recording as the UI sees it. */
data class Recording(
    val fileName: String,
    val file: File,
    val startedAtMillis: Long,
    val durationMillis: Long,
    val sizeBytes: Long,
)

/** Owns the recordings directory and its index in Room (FR-001-07, FR-001-09). */
class RecordingRepository(
    private val recordingsDir: File,
    private val dao: RecordingDao,
) {
    fun directory(): File = recordingsDir.also { it.mkdirs() }

    fun observeRecordings(): Flow<List<Recording>> = dao.observeAll().map { rows ->
        rows.map { Recording(it.fileName, File(directory(), it.fileName), it.startedAtMillis, it.durationMillis, it.sizeBytes) }
    }

    /** Registers a finished file. Called before anything else reacts to the new recording. */
    suspend fun register(file: File, startedAtMillis: Long, durationMillis: Long, quality: AudioQuality) {
        dao.insert(RecordingEntity(file.name, startedAtMillis, durationMillis, file.length(), quality.name))
    }

    /** Deletes the audio and its row; only ever called for an explicit user action. */
    suspend fun delete(fileName: String) = withContext(Dispatchers.IO) {
        File(directory(), fileName).delete()
        dao.delete(fileName)
    }

    /**
     * Audio files with no row (the app died mid-recording, or before registering) are
     * added to the list instead of being lost (Constitution VI, spec 001 edge cases).
     * Files shorter than the minimum are not deleted either: the user decides.
     */
    suspend fun recoverOrphans(): Int = withContext(Dispatchers.IO) {
        val known = dao.fileNames().toSet()
        val orphans = directory().listFiles { f -> f.isFile && f.name.endsWith(".${RecordingNaming.EXTENSION}") }
            .orEmpty()
            .filter { it.name !in known && it.length() > 0 }
        orphans.forEach { file ->
            val startedAt = RecordingNaming.startedAtMillis(file.name) ?: file.lastModified()
            dao.insert(
                RecordingEntity(file.name, startedAt, readDurationMillis(file), file.length(), AudioQuality.DEFAULT.name),
            )
        }
        orphans.size
    }

    private fun readDurationMillis(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: RuntimeException) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }
}
