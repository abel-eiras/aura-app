package io.github.abeleiras.aura.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import io.github.abeleiras.aura.domain.recording.AudioFormats
import io.github.abeleiras.aura.domain.recording.AudioQuality
import io.github.abeleiras.aura.domain.recording.RecordingNaming
import io.github.abeleiras.aura.domain.recording.RecordingPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** What happened to the files of one import (spec 009, FR-009-08). */
data class ImportSummary(
    val imported: Int = 0,
    val unsupported: Int = 0,
    val unreadable: Int = 0,
    val noSpace: Int = 0,
    val failed: Int = 0,
) {
    val total: Int get() = imported + unsupported + unreadable + noSpace + failed

    operator fun plus(other: ImportSummary) = ImportSummary(
        imported + other.imported, unsupported + other.unsupported, unreadable + other.unreadable,
        noSpace + other.noSpace, failed + other.failed,
    )
}

/**
 * Brings audio recorded elsewhere into the app so it is processed exactly like a recording made here (spec 009).
 * The original is only ever read; the app works on a copy that appears in the list only once it is complete and
 * readable as audio (FR-009-04, FR-009-05, Constitution VI).
 */
class AudioImporter(
    private val context: Context,
    private val recordings: RecordingRepository,
    /** Called for each imported file after it is registered: automatic processing and export (FR-009-06). */
    private val onImported: (fileName: String) -> Unit,
) {
    suspend fun importAll(uris: List<Uri>): ImportSummary = withContext(Dispatchers.IO) {
        deleteStaleParts()
        uris.fold(ImportSummary()) { total, uri -> total + importOne(uri) }
    }

    private suspend fun importOne(uri: Uri): ImportSummary {
        val displayName = queryString(uri, OpenableColumns.DISPLAY_NAME)
        val extension = AudioFormats.extensionFor(displayName, context.contentResolver.getType(uri))
            ?: return ImportSummary(unsupported = 1)
        val size = queryLong(uri, OpenableColumns.SIZE)
        val dir = recordings.directory()
        if (size != null && RecordingPolicy.isStorageLow(dir.usableSpace - size)) return ImportSummary(noSpace = 1)

        val startedAt = queryLong(uri, DocumentsContract.Document.COLUMN_LAST_MODIFIED)?.takeIf { it > 0 } ?: System.currentTimeMillis()
        val taken = dir.list()?.toSet().orEmpty()
        val (finalName, recordedAt) = RecordingNaming.uniqueFileName(startedAt, extension, taken)
        val part = File(dir, "$finalName.part")
        val target = File(dir, finalName)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                part.outputStream().use { output -> input.copyTo(output) }
            } ?: return ImportSummary(failed = 1)
            val durationMillis = recordings.durationMillisOf(part)
            if (durationMillis <= 0L) {
                part.delete()
                return ImportSummary(unreadable = 1)
            }
            if (!part.renameTo(target)) {
                part.delete()
                return ImportSummary(failed = 1)
            }
            recordings.register(target, recordedAt, durationMillis, AudioQuality.DEFAULT)
            onImported(finalName)
            return ImportSummary(imported = 1)
        } catch (e: IOException) {
            Log.w(TAG, "Could not import audio", e)
            part.delete()
            return ImportSummary(failed = 1)
        } catch (e: SecurityException) {
            Log.w(TAG, "No access to the chosen audio", e)
            part.delete()
            return ImportSummary(failed = 1)
        }
    }

    /** A copy left half-written by a process that died; never listed, and removed after an hour. */
    private fun deleteStaleParts() {
        val limit = System.currentTimeMillis() - 60 * 60 * 1000
        recordings.directory().listFiles { f -> f.name.endsWith(".part") && f.lastModified() < limit }?.forEach { it.delete() }
    }

    private fun queryString(uri: Uri, column: String): String? = try {
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }

    private fun queryLong(uri: Uri, column: String): Long? = try {
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
    } catch (_: Exception) {
        null // not every provider offers every column
    }

    private companion object {
        const val TAG = "AudioImporter"
    }
}
