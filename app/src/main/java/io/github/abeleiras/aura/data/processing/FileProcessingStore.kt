package io.github.abeleiras.aura.data.processing

import io.github.abeleiras.aura.domain.processing.JobRecord
import io.github.abeleiras.aura.domain.processing.ProcessingStore
import io.github.abeleiras.aura.domain.processing.StoredNote
import io.github.abeleiras.aura.domain.processing.storedNoteFromJson
import io.github.abeleiras.aura.domain.processing.toJson
import io.github.abeleiras.aura.domain.transcript.Transcript
import java.io.File

/**
 * One small JSON file per result next to the app's private data: `<id>.transcript.json`, `<id>.note.json`,
 * `<id>.job.json`, where `<id>` is the audio file name without extension. Files rather than Room so existing
 * installs need no database migration, and every write is atomic (temp file + rename): a process killed in
 * the middle never leaves half a transcript that would be mistaken for a finished step (FR-003-01).
 */
class FileProcessingStore(private val dir: File) : ProcessingStore {
    /** Called after every write or delete so observers can refresh. */
    var onChange: () -> Unit = {}

    override fun loadTranscript(id: String): Transcript? = read(id, TRANSCRIPT) { Transcript.fromJson(it) }

    override fun saveTranscript(id: String, transcript: Transcript) = write(id, TRANSCRIPT, transcript.toJson())

    override fun loadNote(id: String): StoredNote? = read(id, NOTE) { storedNoteFromJson(it) }

    override fun saveNote(id: String, note: StoredNote) = write(id, NOTE, note.toJson())

    override fun loadJob(id: String): JobRecord? = read(id, JOB) { JobRecord.fromJson(it) }

    override fun saveJob(id: String, job: JobRecord) = write(id, JOB, job.toJson())

    fun loadAllJobs(): Map<String, JobRecord> =
        dir.listFiles { f -> f.name.endsWith(".$JOB.json") }.orEmpty().mapNotNull { f ->
            val id = f.name.removeSuffix(".$JOB.json")
            loadJob(id)?.let { id to it }
        }.toMap()

    /** Everything derived from a recording; called when the user deletes it. */
    fun delete(id: String) {
        listOf(TRANSCRIPT, NOTE, JOB).forEach { File(dir, "$id.$it.json").delete() }
        onChange()
    }

    private fun <T> read(id: String, kind: String, parse: (String) -> T): T? {
        val file = File(dir, "$id.$kind.json")
        if (!file.isFile) return null
        return try {
            parse(file.readText())
        } catch (_: IllegalArgumentException) {
            null // unreadable (SerializationException is one): treated as missing, so the step is redone rather than the app failing
        }
    }

    private fun write(id: String, kind: String, text: String) {
        dir.mkdirs()
        val target = File(dir, "$id.$kind.json")
        val temp = File(dir, "$id.$kind.json.part")
        temp.writeText(text)
        if (!temp.renameTo(target)) {
            target.delete()
            check(temp.renameTo(target)) { "Could not store $kind for $id" }
        }
        onChange()
    }

    private companion object {
        const val TRANSCRIPT = "transcript"
        const val NOTE = "note"
        const val JOB = "job"
    }
}
