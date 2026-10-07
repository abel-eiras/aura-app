package io.github.abeleiras.aura.domain.processing

import io.github.abeleiras.aura.domain.notes.NoteTypeCatalogs
import io.github.abeleiras.aura.domain.transcript.Segment
import io.github.abeleiras.aura.domain.transcript.Transcript
import io.github.abeleiras.aura.domain.transcriptOf
import kotlinx.coroutines.test.runTest
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ProcessingPipelineTest {
    private class MemoryStore : ProcessingStore {
        val transcripts = mutableMapOf<String, Transcript>()
        val notes = mutableMapOf<String, StoredNote>()
        val jobs = mutableMapOf<String, JobRecord>()
        val statuses = mutableListOf<JobStatus>()
        override fun loadTranscript(id: String) = transcripts[id]
        override fun saveTranscript(id: String, transcript: Transcript) { transcripts[id] = transcript }
        override fun loadNote(id: String) = notes[id]
        override fun saveNote(id: String, note: StoredNote) { notes[id] = note }
        override fun loadJob(id: String) = jobs[id]
        override fun saveJob(id: String, job: JobRecord) { jobs[id] = job; statuses += job.status }
    }

    private class ScriptedProvider(
        var transcribe: () -> Transcript,
        var complete: (String) -> String,
    ) : AiProvider {
        var transcribeCalls = 0
        var completeCalls = 0
        override suspend fun transcribe(audio: File, hints: LanguageHints): Transcript { transcribeCalls++; return transcribe() }
        override suspend fun complete(system: String, user: String, model: String?, temperature: Double): String {
            completeCalls++
            return complete(user)
        }
    }

    private val audio = File("aura_20261006_101500.ogg")
    private val input = ProcessingInput("aura_20261006_101500", audio, 150.0)
    private val types = NoteTypeCatalogs.defaults().types
    private val talk = transcriptOf("SPEAKER_00" to "Hola a todos", "SPEAKER_01" to "Buenas")
    private val clock = { Instant.parse("2026-10-06T10:30:00Z") }

    private fun pipeline(provider: AiProvider, store: ProcessingStore) =
        ProcessingPipeline(provider, types, store, LanguageHints("es"), null, clock)

    // FR-003-01, FR-003-02
    @Test
    fun `happy path persists transcript then note and ends READY`() = runTest {
        val store = MemoryStore()
        var calls = 0
        val provider = ScriptedProvider({ talk }, { if (++calls == 1) "reunion" else "## Asistentes\nAna" })
        val outcome = pipeline(provider, store).run(input)

        assertEquals(ProcessingOutcome.Finished(JobStatus.READY), outcome)
        assertNotNull(store.transcripts[input.id])
        assertEquals(150.0, store.transcripts[input.id]!!.metadata.durationSeconds, "real duration replaces the provider's guess")
        assertNotNull(store.notes[input.id])
        assertEquals(listOf(JobStatus.TRANSCRIBING, JobStatus.DRAFTING, JobStatus.DRAFTING, JobStatus.READY), store.statuses)
    }

    // FR-003-01: a failure in step 2 does not repeat step 1
    @Test
    fun `a failed draft keeps the transcript and the retry skips transcription`() = runTest {
        val store = MemoryStore()
        var fail = true
        val provider = ScriptedProvider({ talk }, { if (fail) throw ProviderException(ErrorReason.RATE_LIMITED, true) else "reunion" })
        val first = pipeline(provider, store).run(input)

        assertEquals(ProcessingOutcome.RetryLater(15.seconds), first)
        assertNotNull(store.transcripts[input.id])
        assertNull(store.notes[input.id])
        assertEquals(1, provider.transcribeCalls)
        assertEquals(1, store.jobs[input.id]!!.attempts)

        fail = false
        provider.complete = { "reunion\n## Asistentes" }
        val second = pipeline(provider, store).run(input)
        assertEquals(ProcessingOutcome.Finished(JobStatus.READY), second)
        assertEquals(1, provider.transcribeCalls, "transcription is not repeated")
    }

    // FR-003-09
    @Test
    fun `silence ends NO_CONTENT without calling step 2`() = runTest {
        val store = MemoryStore()
        val silent = talk.copy(segments = emptyList<Segment>(), fullText = "")
        val provider = ScriptedProvider({ silent }, { error("must not be called") })
        val outcome = pipeline(provider, store).run(input)
        assertEquals(ProcessingOutcome.Finished(JobStatus.NO_CONTENT), outcome)
        assertEquals(0, provider.completeCalls)
        // running again does nothing more
        assertEquals(ProcessingOutcome.Finished(JobStatus.NO_CONTENT), pipeline(provider, store).run(input))
        assertEquals(1, provider.transcribeCalls)
    }

    // FR-003-07
    @Test
    fun `network failure waits for connection and gives up after five attempts`() = runTest {
        val store = MemoryStore()
        val provider = ScriptedProvider({ throw ProviderException(ErrorReason.NETWORK, true) }, { "" })
        repeat(4) { assertIs<ProcessingOutcome.RetryLater>(pipeline(provider, store).run(input)) }
        assertEquals(JobStatus.WAITING_NETWORK, store.jobs[input.id]!!.status)
        val last = pipeline(provider, store).run(input)
        assertEquals(ProcessingOutcome.Finished(JobStatus.ERROR, ErrorReason.NETWORK), last)
    }

    @Test
    fun `permanent failures stop at once with the reason`() = runTest {
        val store = MemoryStore()
        val provider = ScriptedProvider({ throw ProviderException(ErrorReason.INVALID_CREDENTIAL, false) }, { "" })
        val outcome = pipeline(provider, store).run(input)
        assertEquals(ProcessingOutcome.Finished(JobStatus.ERROR, ErrorReason.INVALID_CREDENTIAL), outcome)
        assertEquals(ErrorReason.INVALID_CREDENTIAL, store.jobs[input.id]!!.error)
    }

    // spec 003 edge case: wrong format is retried once
    @Test
    fun `an invalid answer is retried once and then reported`() = runTest {
        val store = MemoryStore()
        val provider = ScriptedProvider({ throw ProviderException(ErrorReason.INVALID_RESPONSE, true) }, { "" })
        assertIs<ProcessingOutcome.RetryLater>(pipeline(provider, store).run(input))
        assertEquals(
            ProcessingOutcome.Finished(JobStatus.ERROR, ErrorReason.INVALID_RESPONSE),
            pipeline(provider, store).run(input),
        )
    }

    // FR-003-08
    @Test
    fun `audio over the maximum is rejected before any upload`() = runTest {
        val store = MemoryStore()
        val provider = ScriptedProvider({ error("must not upload") }, { "" })
        val outcome = pipeline(provider, store).run(input.copy(durationSeconds = 3 * 3600 + 5.0))
        assertEquals(ProcessingOutcome.Finished(JobStatus.ERROR, ErrorReason.AUDIO_TOO_LONG), outcome)
        assertEquals(0, provider.transcribeCalls)
    }

    @Test
    fun `job records round trip`() {
        val job = JobRecord(JobStatus.ERROR, 3, ErrorReason.QUOTA_EXHAUSTED, "daily quota", "2026-10-06T10:30:00Z")
        assertEquals(job, JobRecord.fromJson(job.toJson()))
        val note = StoredNote("reunion", "## A", "2026-10-06T10:30:00Z", "m", true)
        assertEquals(note, storedNoteFromJson(note.toJson()))
        assertTrue(note.toJson().contains("reunion"))
    }

    // FR-003-11 / HU-003-4: "Change type" redrafts with that type, without classifying and without transcribing again
    @Test
    fun `a forced type skips classification and reuses the transcript`() = runTest {
        val store = MemoryStore()
        store.saveTranscript(input.id, talk)
        val prompts = mutableListOf<String>()
        val provider = ScriptedProvider({ error("must not transcribe") }, { prompts += it; "## Contenido\nTexto" })
        val personal = types.first { it.id == "nota_personal" }

        val outcome = pipeline(provider, store).run(input, forcedType = personal)

        assertEquals(ProcessingOutcome.Finished(JobStatus.READY), outcome)
        assertEquals(1, provider.completeCalls, "only the drafting call")
        assertEquals("nota_personal", store.notes[input.id]!!.typeId)
        assertEquals(0, provider.transcribeCalls)
    }
}
