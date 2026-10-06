package io.github.abeleiras.aura.domain.notes

import io.github.abeleiras.aura.domain.transcript.Speakers
import io.github.abeleiras.aura.domain.transcript.Transcript
import java.util.Locale

/**
 * Renders the exported Markdown note and decides where exported files go.
 * Format: `specs/004-notas-y-exportacion/contracts/nota-frontmatter.md`, which
 * matches aura-transcribe's notas.py (FR-004-08).
 */
object NoteRenderer {

    /** Where each exported artifact goes, relative to the export folder (FR-004-08). */
    data class ExportPaths(val audio: String, val transcript: String, val note: String)

    fun exportPaths(audioFileName: String, type: NoteType, date: String): ExportPaths {
        val stem = audioFileName.substringBeforeLast('.')
        return ExportPaths(
            audio = audioFileName,
            transcript = "transcription/$stem.json",
            note = "notas/${type.folder}/$date-$stem.md",
        )
    }

    fun render(
        type: NoteType,
        date: String,
        transcript: Transcript,
        body: String,
        generatedBy: String? = null,
        genericSpeaker: String = "Hablante",
    ): String {
        val meta = transcript.metadata
        val stem = meta.sourceFile.substringBeforeLast('.')
        val minutes = String.format(Locale.ROOT, "%.1f", meta.durationSeconds / 60)
        val front = buildList {
            add("---")
            add("tipo: ${type.id}")
            add("fecha: $date")
            add("audio_origen: ${quote(meta.sourceFile)}")
            add("json_origen: ${quote("$stem.json")}")
            add("idioma: ${meta.languageUsed}")
            add("hablantes: ${meta.numSpeakersDetected}")
            add("duracion_min: $minutes")
            add("tags: [aura, ${type.tag}]")
            if (!meta.speakerNames.isNullOrEmpty()) {
                val names = Speakers.displayNames(transcript, genericSpeaker).joinToString(", ") { quote(it) }
                add("hablantes_nombres: [$names]")
            }
            if (generatedBy != null) add("generado_por: ${quote(generatedBy)}")
            add("---")
            add("")
        }.joinToString("\n")
        val namedBody = Speakers.applyNames(transcript, body.trim(), genericSpeaker)
        return "$front\n# ${type.label} — $date\n\n$namedBody\n"
    }

    private fun quote(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
