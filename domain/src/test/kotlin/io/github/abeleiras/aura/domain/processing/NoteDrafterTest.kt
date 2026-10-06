package io.github.abeleiras.aura.domain.processing

import io.github.abeleiras.aura.domain.notes.NoteTypeCatalogs
import io.github.abeleiras.aura.domain.transcript.Transcript
import io.github.abeleiras.aura.domain.transcriptOf
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NoteDrafterTest {
    private class FakeProvider(private val answers: ArrayDeque<String>) : AiProvider {
        data class Call(val system: String, val user: String, val model: String?, val temperature: Double)
        val calls = mutableListOf<Call>()
        override suspend fun transcribe(audio: File, hints: LanguageHints): Transcript = error("not used")
        override suspend fun complete(system: String, user: String, model: String?, temperature: Double): String {
            calls += Call(system, user, model, temperature)
            return answers.removeFirst()
        }
    }

    private val types = NoteTypeCatalogs.defaults().types
    private val transcript = transcriptOf("SPEAKER_00" to "Hola", "SPEAKER_01" to "Buenas")

    @Test
    fun `FR-003-05 classifies then drafts with the chosen type's prompt and model override`() = runTest {
        val withModel = types.map { if (it.id == "reunion") it.copy(model = "mejor-modelo") else it }
        val provider = FakeProvider(ArrayDeque(listOf("reunion", "  ## Asistentes\nAna  ")))
        val note = NoteDrafter(provider, withModel, defaultModel = "barato").run(transcript)

        assertEquals("reunion", note.type.id)
        assertEquals("## Asistentes\nAna", note.body)
        val (classify, draft) = provider.calls
        assertEquals(0.0, classify.temperature)
        assertEquals("barato", classify.model)
        assertTrue("Idioma detectado: es" in classify.user && transcript.fullText in classify.user)
        assertEquals(withModel.first { it.id == "reunion" }.draftingPrompt, draft.system)
        assertEquals("mejor-modelo", draft.model)
        assertEquals(transcript.fullText, draft.user)
    }

    @Test
    fun `spec 003 unknown classification falls back to the default type and is flagged`() = runTest {
        val note = NoteDrafter(FakeProvider(ArrayDeque(listOf("???", "texto"))), types, null).run(transcript)
        assertEquals("nota_personal", note.type.id)
        assertTrue(note.classifiedByFallback)
    }

    @Test
    fun `an empty draft is a retryable invalid response`() = runTest {
        val e = assertFailsWith<ProviderException> {
            NoteDrafter(FakeProvider(ArrayDeque(listOf("reunion", "   "))), types, null).run(transcript)
        }
        assertEquals(ErrorReason.INVALID_RESPONSE, e.reason)
        assertTrue(e.transient)
    }

    @Test
    fun `FR-003-09 never drafts an empty transcript`() = runTest {
        val empty = transcript.copy(fullText = "")
        assertFailsWith<IllegalArgumentException> { NoteDrafter(FakeProvider(ArrayDeque()), types, null).run(empty) }
    }
}
