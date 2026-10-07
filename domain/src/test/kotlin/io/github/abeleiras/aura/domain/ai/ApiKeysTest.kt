package io.github.abeleiras.aura.domain.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApiKeysTest {
    private val key = "AIza" + "a".repeat(35)

    // FR-002-03
    @Test
    fun `accepts a well formed key, trimming whitespace`() {
        assertEquals(key, ApiKeys.geminiKeyOrNull("  $key\n"))
    }

    @Test
    fun `rejects other text`() {
        assertNull(ApiKeys.geminiKeyOrNull("hello"))
        assertNull(ApiKeys.geminiKeyOrNull("AIza" + "a".repeat(10)))
        assertNull(ApiKeys.geminiKeyOrNull("$key extra"))
        assertNull(ApiKeys.geminiKeyOrNull(""))
    }
}
