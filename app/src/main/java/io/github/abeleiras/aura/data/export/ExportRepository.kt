package io.github.abeleiras.aura.data.export

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import io.github.abeleiras.aura.data.Recording
import io.github.abeleiras.aura.data.RecordingRepository
import io.github.abeleiras.aura.data.prefs.AppSettings
import io.github.abeleiras.aura.domain.export.ExportNaming
import io.github.abeleiras.aura.domain.export.ExportPolicy
import io.github.abeleiras.aura.domain.export.ExportStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

data class ExportState(
    val folderUri: Uri? = null,
    val folderName: String? = null,
    /** The system no longer lets us write to the chosen folder (e.g. after restoring on another phone). */
    val permissionLost: Boolean = false,
    val exportFromMillis: Long = 0L,
    val exported: Set<String> = emptySet(),
    /** Files whose last attempt in this session failed. */
    val failed: Set<String> = emptySet(),
    val running: Boolean = false,
) {
    val folderConfigured: Boolean get() = folderUri != null

    fun statusOf(recording: Recording): ExportStatus =
        ExportPolicy.status(folderConfigured, recording.fileName, recording.startedAtMillis, exportFromMillis, exported, failed)
}

/**
 * Copies recordings to a folder the user picked with the system picker (Storage Access
 * Framework), so Syncthing, Obsidian or aura-transcribe can take them from there
 * (spec 004, FR-004-07/09). Audio only for now.
 */
class ExportRepository(
    private val context: Context,
    private val settings: AppSettings,
    private val recordings: RecordingRepository,
) {
    private val resolver: ContentResolver get() = context.contentResolver
    private val mutex = Mutex()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<ExportState> = _state.asStateFlow()

    /** Chooses [uri] as the export folder and keeps the permission across reboots (HU-004-5, scenario 1). */
    suspend fun setFolder(uri: Uri, includeExisting: Boolean) {
        withContext(Dispatchers.IO) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            resolver.takePersistableUriPermission(uri, flags)
            releaseCurrent(except = uri)
            settings.exportFolderUri = uri.toString()
            settings.exportFromMillis = if (includeExisting) 0L else System.currentTimeMillis()
        }
        reload()
        exportPending()
    }

    /** Stops exporting. What was already copied stays where it is. */
    suspend fun clearFolder() {
        withContext(Dispatchers.IO) {
            releaseCurrent(except = null)
            settings.exportFolderUri = null
        }
        reload()
    }

    /** "Export all recordings": also the ones from before the folder was chosen. */
    suspend fun exportAllNow() {
        settings.exportFromMillis = 0L
        reload()
        exportPending()
    }

    /** Called when a recording is registered, when the app opens or resumes, and on "Retry". */
    suspend fun exportPending() {
        mutex.withLock {
            reload()
            val tree = _state.value.folderUri ?: return
            val hasPermission = withContext(Dispatchers.IO) { hasPermission(tree) }
            if (!hasPermission) {
                _state.value = _state.value.copy(permissionLost = true)
                return
            }
            _state.value = _state.value.copy(permissionLost = false, failed = emptySet(), running = true)
            try {
                val start = _state.value
                val pending = recordings.all().filter {
                    ExportPolicy.status(true, it.fileName, it.startedAtMillis, start.exportFromMillis, start.exported, emptySet()) ==
                        ExportStatus.PENDING
                }
                for (recording in pending) {
                    try {
                        copy(tree, recording.file)
                        settings.addExported(recording.fileName)
                        _state.value = _state.value.copy(exported = settings.exportedFiles)
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Lost access to the export folder", e)
                        _state.value = _state.value.copy(permissionLost = true)
                        return
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not export ${recording.fileName}", e)
                        _state.value = _state.value.copy(failed = _state.value.failed + recording.fileName)
                    }
                }
            } finally {
                // NonCancellable: `running` must go back to false even if the caller was cancelled.
                withContext(NonCancellable) {
                    _state.value = _state.value.copy(running = false, folderName = withContext(Dispatchers.IO) { displayName(tree) })
                }
            }
        }
    }

    /** A deleted recording leaves the ledger; the exported copy is the user's and stays (FR-004-10). */
    fun forget(fileName: String) {
        settings.removeExported(fileName)
        _state.value = _state.value.copy(exported = settings.exportedFiles)
    }

    /** Straight from preferences and the system's permission list; never reads [_state]. */
    private fun load(): ExportState {
        val uri = settings.exportFolderUri?.let(Uri::parse)
        return ExportState(
            folderUri = uri,
            permissionLost = uri != null && !hasPermission(uri),
            exportFromMillis = settings.exportFromMillis,
            exported = settings.exportedFiles,
        )
    }

    /** Refreshes from preferences, keeping what only lives in memory (failures, progress, folder name). */
    private suspend fun reload() {
        val fresh = withContext(Dispatchers.IO) { load() }
        val current = _state.value
        val name = when {
            fresh.folderUri == null -> null
            fresh.folderUri == current.folderUri && current.folderName != null -> current.folderName
            else -> withContext(Dispatchers.IO) { displayName(fresh.folderUri) }
        }
        _state.value = fresh.copy(folderName = name, failed = current.failed, running = current.running)
    }

    private fun hasPermission(tree: Uri): Boolean =
        resolver.persistedUriPermissions.any { it.uri == tree && it.isReadPermission && it.isWritePermission }

    private fun releaseCurrent(except: Uri?) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val current = settings.exportFolderUri?.let(Uri::parse) ?: return
        if (current != except) runCatching { resolver.releasePersistableUriPermission(current, flags) }
    }

    private fun displayName(tree: Uri): String? = try {
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        resolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }

    private class Child(val documentId: String, val size: Long)

    private fun listChildren(tree: Uri, treeDocumentId: String): Map<String, Child> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeDocumentId)
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        val result = linkedMapOf<String, Child>()
        resolver.query(children, columns, null, null, null)?.use { c ->
            while (c.moveToNext()) result[c.getString(1)] = Child(c.getString(0), if (c.isNull(2)) -1L else c.getLong(2))
        } ?: throw IOException("The folder can't be listed")
        return result
    }

    private suspend fun copy(tree: Uri, file: File) = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext // deleted in the meantime: nothing to export
        val treeId = DocumentsContract.getTreeDocumentId(tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, treeId)
        val existing = listChildren(tree, treeId)

        // Leftover of an interrupted copy: remove it and start over.
        existing[file.name + ExportNaming.PARTIAL_SUFFIX]?.let { stale ->
            runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, stale.documentId)) }
        }
        // Same name and size: this recording is already there (e.g. the ledger was lost).
        if (existing[file.name]?.size == file.length()) return@withContext

        val finalName = ExportNaming.uniqueName(file.name, existing.keys.filterNot(ExportNaming::isPartial).toSet())
        writeAtomically(parent, finalName, file)
    }

    /**
     * Writes `<name>.part`, then renames it, so the final name only ever points at complete
     * content (FR-004-09). Providers that can't rename get a direct write: aura-transcribe
     * already waits for a file to stop changing before it reads it.
     */
    private fun writeAtomically(parent: Uri, finalName: String, file: File) {
        val part = DocumentsContract.createDocument(resolver, parent, "application/octet-stream", finalName + ExportNaming.PARTIAL_SUFFIX)
            ?: throw IOException("The folder refused a new file")
        var renamed = false
        try {
            writeTo(part, file)
            renamed = DocumentsContract.renameDocument(resolver, part, finalName) != null
        } finally {
            if (!renamed) runCatching { DocumentsContract.deleteDocument(resolver, part) }
        }
        if (renamed) return

        val direct = DocumentsContract.createDocument(resolver, parent, "audio/ogg", finalName)
            ?: throw IOException("The folder refused a new file")
        try {
            writeTo(direct, file)
        } catch (e: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, direct) }
            throw e
        }
    }

    private fun writeTo(target: Uri, file: File) {
        val output = resolver.openOutputStream(target, "wt") ?: throw IOException("No output stream")
        output.use { out -> file.inputStream().use { it.copyTo(out) } }
    }

    private companion object {
        const val TAG = "ExportRepository"
    }
}
