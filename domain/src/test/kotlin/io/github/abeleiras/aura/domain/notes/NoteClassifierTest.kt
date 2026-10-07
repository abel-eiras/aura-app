package io.github.abeleiras.aura.domain.notes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoteClassifierTest {
    private val types = NoteTypeCatalogs.defaults().types

    @Test
    fun `FR-003-05 classifier prompt shows only ids and criteria, never drafting prompts`() {
        val prompt = NoteClassifier.systemPrompt(types)
        types.forEach {
            assertTrue("- ${it.id}: ${it.criterion}." in prompt)
            assertFalse(it.draftingPrompt.take(40) in prompt)
        }
        assertTrue(prompt.endsWith("reunion, visita_cliente, nota_personal."))
    }

    @Test
    fun `FR-003-05 answer is matched ignoring case, whitespace and quoting`() {
        assertEquals("reunion", NoteClassifier.resolve("  Reunion\n", types).type.id)
        assertEquals("visita_cliente", NoteClassifier.resolve("`visita_cliente`", types).type.id)
        assertFalse(NoteClassifier.resolve("reunion", types).fellBack)
    }

    @Test
    fun `spec 005 unknown answer falls back to the default type, or the first`() {
        val r = NoteClassifier.resolve("podcast", types)
        assertTrue(r.fellBack)
        assertEquals("nota_personal", r.type.id)
        val noDefault = types.map { it.copy(isDefault = false) }
        assertEquals("reunion", NoteClassifier.resolve("???", noDefault).type.id)
    }
}
