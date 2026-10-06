package io.github.abeleiras.aura.domain.notes

import io.github.abeleiras.aura.domain.transcriptOf
import io.github.abeleiras.aura.domain.transcript.Speakers
import kotlin.test.Test
import kotlin.test.assertEquals

class NoteRendererTest {
    private val reunion = NoteTypeCatalogs.defaults().types.first { it.id == "reunion" }
    private val transcript = transcriptOf("SPEAKER_00" to "Hola", "SPEAKER_01" to "Buenas", duration = 2550.0)

    @Test
    fun `FR-004-08 export paths follow the aura-transcribe layout`() {
        val p = NoteRenderer.exportPaths("aura_20261006_101500.ogg", reunion, "2026-10-06")
        assertEquals("aura_20261006_101500.ogg", p.audio)
        assertEquals("transcription/aura_20261006_101500.json", p.transcript)
        assertEquals("notas/Reuniones/2026-10-06-aura_20261006_101500.md", p.note)
    }

    @Test
    fun `FR-004-08 note matches the frontmatter contract`() {
        val md = NoteRenderer.render(reunion, "2026-10-06", transcript, "## Asistentes\nAna\n")
        assertEquals(
            """
            ---
            tipo: reunion
            fecha: 2026-10-06
            audio_origen: "aura_20261006_101500.ogg"
            json_origen: "aura_20261006_101500.json"
            idioma: es
            hablantes: 2
            duracion_min: 42.5
            tags: [aura, reunion]
            ---

            # Reunión — 2026-10-06

            ## Asistentes
            Ana

            """.trimIndent(),
            md,
        )
    }

    @Test
    fun `FR-004-03 speaker names replace labels in body and frontmatter`() {
        val named = Speakers.rename(Speakers.rename(transcript, "SPEAKER_00", "Ana"), "SPEAKER_01", "Luis \"el jefe\"")
        val md = NoteRenderer.render(reunion, "2026-10-06", named, "SPEAKER_00 propuso; SPEAKER_01 aceptó.", generatedBy = "aura-app 1.0.0 (gemini)")
        assertEquals(true, "Ana propuso; Luis \"el jefe\" aceptó." in md)
        assertEquals(true, "hablantes_nombres: [\"Ana\", \"Luis \\\"el jefe\\\"\"]" in md)
        assertEquals(true, "generado_por: \"aura-app 1.0.0 (gemini)\"" in md)
    }
}
