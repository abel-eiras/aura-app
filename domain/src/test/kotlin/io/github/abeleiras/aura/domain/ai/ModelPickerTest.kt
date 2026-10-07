package io.github.abeleiras.aura.domain.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModelPickerTest {
    private fun models(vararg ids: String) = ids.map { GeminiModel(it, it) }

    // FR-002-07: no hard-wired model name
    @Test
    fun `prefers the latest alias when the key has it`() {
        assertEquals("gemini-flash-latest", ModelPicker.bestFlash(models("gemini-2.5-flash", "gemini-flash-latest")))
    }

    @Test
    fun `otherwise takes the highest stable flash version`() {
        val picked = ModelPicker.bestFlash(models("gemini-2.5-flash", "gemini-3.8-flash", "gemini-3.8-flash-lite", "gemini-3-pro", "gemini-3.10-flash-preview"))
        assertEquals("gemini-3.8-flash", picked)
    }

    @Test
    fun `no flash model means no automatic choice`() {
        assertNull(ModelPicker.bestFlash(models("gemini-3-pro", "gemma-3")))
    }

    @Test
    fun `flash list hides image, tts and live variants`() {
        assertEquals(listOf("gemini-3.8-flash", "gemini-2.5-flash"), ModelPicker.flashModels(models("gemini-2.5-flash", "gemini-3.8-flash", "gemini-3.8-flash-image", "gemini-live-flash", "gemini-3-pro")))
    }
}
