package io.github.abeleiras.aura.domain.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExportTest {
    private fun status(
        folder: Boolean = true,
        name: String = "aura_1.ogg",
        startedAt: Long = 1_000,
        from: Long = 0,
        exported: Set<String> = emptySet(),
        failed: Set<String> = emptySet(),
    ) = ExportPolicy.status(folder, name, startedAt, from, exported, failed)

    @Test
    fun `FR-004-07 without a folder nothing is pending`() {
        assertEquals(ExportStatus.NONE, status(folder = false))
    }

    @Test
    fun `FR-004-07 with a folder, a new recording is pending`() {
        assertEquals(ExportStatus.PENDING, status())
    }

    @Test
    fun `FR-004-09 a failed attempt shows as error, and exported wins over everything`() {
        assertEquals(ExportStatus.ERROR, status(failed = setOf("aura_1.ogg")))
        assertEquals(ExportStatus.EXPORTED, status(exported = setOf("aura_1.ogg"), failed = setOf("aura_1.ogg")))
    }

    @Test
    fun `HU-004-5 choosing only new recordings leaves older ones alone`() {
        assertEquals(ExportStatus.NONE, status(startedAt = 999, from = 1_000))
        assertEquals(ExportStatus.PENDING, status(startedAt = 1_000, from = 1_000))
        assertEquals(ExportStatus.PENDING, status(startedAt = 1, from = 0))
    }

    @Test
    fun `an exported recording stays exported even if the folder is later removed`() {
        assertEquals(ExportStatus.EXPORTED, status(folder = false, exported = setOf("aura_1.ogg")))
    }

    @Test
    fun `spec 004 a foreign file is never overwritten`() {
        assertEquals("aura_1.ogg", ExportNaming.uniqueName("aura_1.ogg", emptySet()))
        assertEquals("aura_1_1.ogg", ExportNaming.uniqueName("aura_1.ogg", setOf("aura_1.ogg")))
        assertEquals("aura_1_3.ogg", ExportNaming.uniqueName("aura_1.ogg", setOf("aura_1.ogg", "aura_1_1.ogg", "aura_1_2.ogg")))
        assertEquals("notes_1", ExportNaming.uniqueName("notes", setOf("notes")))
    }

    @Test
    fun `FR-004-09 in-progress copies are recognisable`() {
        assertTrue(ExportNaming.isPartial("aura_1.ogg.part"))
        assertFalse(ExportNaming.isPartial("aura_1.ogg"))
    }
}
