package io.github.abeleiras.aura.domain.transcript

import io.github.abeleiras.aura.domain.transcriptOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpeakersTest {

    @Test
    fun `FR-003-03 normalize relabels by first appearance and rebuilds full_text`() {
        val raw = transcriptOf("B" to " Hola ", "A" to "Buenas", "B" to "Qué tal", "" to "   ")
        val n = Speakers.normalize(raw)
        assertEquals(listOf("SPEAKER_00", "SPEAKER_01", "SPEAKER_00"), n.segments.map { it.speaker })
        assertEquals(listOf(0, 1, 2), n.segments.map { it.id })
        assertEquals(2, n.metadata.numSpeakersDetected)
        assertEquals("SPEAKER_00: Hola\nSPEAKER_01: Buenas\nSPEAKER_00: Qué tal", n.fullText)
    }

    @Test
    fun `FR-003-09 a transcript with only blank segments has no content`() {
        assertFalse(Speakers.normalize(transcriptOf("A" to " ", "B" to "")).hasContent)
        assertTrue(Speakers.normalize(transcriptOf("A" to "hola")).hasContent)
    }

    @Test
    fun `FR-004-02 rename sets and clears names without touching segments`() {
        val t = transcriptOf("SPEAKER_00" to "Hola", "SPEAKER_01" to "Buenas")
        val named = Speakers.rename(t, "SPEAKER_00", "  Ana ")
        assertEquals("Ana", Speakers.displayName(named, "SPEAKER_00"))
        assertEquals("Hablante 2", Speakers.displayName(named, "SPEAKER_01"))
        assertEquals(t.segments, named.segments)
        assertNull(Speakers.rename(named, "SPEAKER_00", " ").metadata.speakerNames)
    }

    @Test
    fun `FR-004-02 merge makes two speakers one and keeps the target name`() {
        val t = Speakers.rename(
            Speakers.rename(transcriptOf("SPEAKER_00" to "a", "SPEAKER_01" to "b", "SPEAKER_02" to "c"), "SPEAKER_01", "Luis"),
            "SPEAKER_02", "Ana",
        )
        val merged = Speakers.merge(t, from = "SPEAKER_02", into = "SPEAKER_01")
        assertEquals(listOf("SPEAKER_00", "SPEAKER_01", "SPEAKER_01"), merged.segments.map { it.speaker })
        assertEquals(2, merged.metadata.numSpeakersDetected)
        assertEquals(mapOf("SPEAKER_01" to "Luis"), merged.metadata.speakerNames)
        assertEquals("SPEAKER_00: a\nSPEAKER_01: b\nSPEAKER_01: c", merged.fullText)
        assertEquals(listOf("Hablante 1", "Luis"), Speakers.displayNames(merged))
    }

    @Test
    fun `FR-004-03 applyNames only replaces well-formed labels`() {
        val t = Speakers.rename(transcriptOf("SPEAKER_00" to "x", "SPEAKER_01" to "y"), "SPEAKER_00", "Ana")
        assertEquals("Ana habló con Hablante 2; SPEAKER_7 queda igual", Speakers.applyNames(t, "SPEAKER_00 habló con SPEAKER_01; SPEAKER_7 queda igual"))
    }

    @Test
    fun `FR-003-02 transcript json round-trips and tolerates aura-transcribe extras`() {
        val t = transcriptOf("SPEAKER_00" to "Hola")
        assertEquals(t, Transcript.fromJson(t.toJson()))
        val withWords = t.toJson().replace("\"text\": \"Hola\"", "\"text\": \"Hola\", \"words\": [{\"word\": \"Hola\"}]")
        assertEquals(t, Transcript.fromJson(withWords))
    }
}
