package io.github.abeleiras.aura.domain.processing

import io.github.abeleiras.aura.domain.notes.NoteType
import io.github.abeleiras.aura.domain.transcript.Transcript
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import kotlin.time.Duration

/** The drafted note as persisted (step 2 output), independent of the exported Markdown (spec 004). */
@Serializable
data class StoredNote(
    val typeId: String,
    val body: String,
    val draftedAt: String,
    val model: String? = null,
    val classifiedByFallback: Boolean = false,
)

/** Progress of one recording's processing, persisted after every change so it survives the process dying. */
@Serializable
data class JobRecord(
    val status: JobStatus,
    val attempts: Int = 0,
    val error: ErrorReason? = null,
    val message: String? = null,
    val updatedAt: String,
    /** Set when the provider said how long to wait (e.g. the free quota resets in 5 h). */
    val retryAfterSeconds: Long? = null,
) {
    fun toJson(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        fun fromJson(text: String): JobRecord = JSON.decodeFromString(serializer(), text)
    }
}

private val NOTE_JSON = Json { encodeDefaults = true; prettyPrint = true; ignoreUnknownKeys = true }

fun StoredNote.toJson(): String = NOTE_JSON.encodeToString(StoredNote.serializer(), this)
fun storedNoteFromJson(text: String): StoredNote = NOTE_JSON.decodeFromString(StoredNote.serializer(), text)

/** Where each step's result lives. Implemented on files in the app and in memory in tests (FR-003-01). */
interface ProcessingStore {
    fun loadTranscript(id: String): Transcript?
    fun saveTranscript(id: String, transcript: Transcript)
    fun loadNote(id: String): StoredNote?
    fun saveNote(id: String, note: StoredNote)
    fun loadJob(id: String): JobRecord?
    fun saveJob(id: String, job: JobRecord)
}

data class ProcessingInput(val id: String, val audio: File, val durationSeconds: Double)

sealed interface ProcessingOutcome {
    /** Reached a final status: READY, NO_CONTENT or ERROR. */
    data class Finished(val status: JobStatus, val error: ErrorReason? = null) : ProcessingOutcome

    /** A transient failure; run again after [after] (state is persisted, finished steps are not repeated). */
    data class RetryLater(val after: Duration) : ProcessingOutcome
}

/**
 * The two-step pipeline of spec 003: transcribe, then draft. Each result is persisted before the next step
 * starts, so a failure in step 2 never repeats step 1 (FR-003-01), and nothing here touches the audio
 * (Constitution VI). Whoever calls [run] decides when: a background worker with backoff (FR-003-06).
 */
class ProcessingPipeline(
    private val provider: AiProvider,
    private val types: List<NoteType>,
    private val store: ProcessingStore,
    private val hints: LanguageHints,
    private val draftModel: String?,
    private val now: () -> Instant = { Instant.now() },
) {
    /**
     * @param forcedType draft with this type and skip classification ("Change type", HU-003-4); null classifies as usual.
     */
    suspend fun run(input: ProcessingInput, forcedType: NoteType? = null): ProcessingOutcome {
        var job = store.loadJob(input.id) ?: JobRecord(JobStatus.QUEUED, updatedAt = now().toString())
        fun update(status: JobStatus, attempts: Int = job.attempts, error: ErrorReason? = null, message: String? = null, retryAfterSeconds: Long? = null) {
            job = JobRecord(status, attempts, error, message, now().toString(), retryAfterSeconds)
            store.saveJob(input.id, job)
        }
        suspend fun steps(): ProcessingOutcome {
            while (true) {
                val step = ProcessingPolicy.nextStep(
                    hasTranscript = store.loadTranscript(input.id) != null,
                    hasNote = store.loadNote(input.id) != null,
                    noContent = job.status == JobStatus.NO_CONTENT,
                ) ?: return finish(job, ::update)
                when (step) {
                    ProcessingStep.TRANSCRIBE -> {
                        update(JobStatus.TRANSCRIBING)
                        if (ProcessingPolicy.exceedsMaxDuration(input.durationSeconds)) {
                            throw ProviderException(ErrorReason.AUDIO_TOO_LONG, transient = false)
                        }
                        val raw = provider.transcribe(input.audio, hints)
                        val transcript = raw.copy(metadata = raw.metadata.copy(durationSeconds = input.durationSeconds, sourceFile = input.audio.name))
                        store.saveTranscript(input.id, transcript)
                        if (!transcript.hasContent) { // FR-003-09: no second call for silence
                            update(JobStatus.NO_CONTENT, attempts = 0)
                        } else {
                            update(JobStatus.DRAFTING, attempts = 0)
                        }
                    }
                    ProcessingStep.DRAFT -> {
                        update(JobStatus.DRAFTING)
                        val transcript = checkNotNull(store.loadTranscript(input.id))
                        val drafter = NoteDrafter(provider, types, draftModel)
                        val drafted = if (forcedType != null) {
                            DraftedNote(forcedType, drafter.draft(transcript, forcedType), classifiedByFallback = false)
                        } else {
                            drafter.run(transcript)
                        }
                        store.saveNote(
                            input.id,
                            StoredNote(drafted.type.id, drafted.body, now().toString(), draftModel, drafted.classifiedByFallback),
                        )
                        update(JobStatus.READY, attempts = 0)
                    }
                }
            }
        }
        return try {
            steps()
        } catch (e: CancellationException) {
            throw e // the process is being stopped; the persisted state resumes later
        } catch (e: ProviderException) {
            failed(e, job, ::update)
        }
    }

    private fun finish(job: JobRecord, update: (JobStatus, Int, ErrorReason?, String?, Long?) -> Unit): ProcessingOutcome {
        if (job.status != JobStatus.READY && job.status != JobStatus.NO_CONTENT) update(JobStatus.READY, 0, null, null, null)
        return ProcessingOutcome.Finished(if (job.status == JobStatus.NO_CONTENT) JobStatus.NO_CONTENT else JobStatus.READY)
    }

    private fun failed(
        error: ProviderException,
        job: JobRecord,
        update: (JobStatus, Int, ErrorReason?, String?, Long?) -> Unit,
    ): ProcessingOutcome {
        val attempts = job.attempts + 1
        return when (val decision = ProcessingPolicy.onFailure(error, attempts)) {
            is FailureDecision.Retry -> {
                val status = if (error.reason == ErrorReason.NETWORK) JobStatus.WAITING_NETWORK else JobStatus.QUEUED
                update(status, attempts, error.reason, error.message, error.retryAfter?.inWholeSeconds)
                ProcessingOutcome.RetryLater(maxOf(decision.after, error.retryAfter ?: decision.after))
            }
            is FailureDecision.GiveUp -> {
                update(JobStatus.ERROR, attempts, decision.reason, error.message, error.retryAfter?.inWholeSeconds)
                ProcessingOutcome.Finished(JobStatus.ERROR, decision.reason)
            }
        }
    }
}
