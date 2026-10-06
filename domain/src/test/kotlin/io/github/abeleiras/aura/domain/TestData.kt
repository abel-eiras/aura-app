package io.github.abeleiras.aura.domain

import io.github.abeleiras.aura.domain.transcript.Segment
import io.github.abeleiras.aura.domain.transcript.Speakers
import io.github.abeleiras.aura.domain.transcript.Transcript
import io.github.abeleiras.aura.domain.transcript.TranscriptMetadata

fun transcriptOf(vararg lines: Pair<String, String>, duration: Double = 150.0, source: String = "aura_20261006_101500.ogg"): Transcript {
    val segments = lines.mapIndexed { i, (speaker, text) -> Segment(i, speaker, i * 5.0, i * 5.0 + 4.0, text) }
    return Transcript(
        metadata = TranscriptMetadata(
            sourceFile = source,
            processedAt = "2026-10-06T10:20:00Z",
            durationSeconds = duration,
            languageUsed = "es",
            numSpeakersDetected = segments.map { it.speaker }.distinct().size,
            pipelineVersion = "aura-app test",
            provider = "gemini",
        ),
        segments = segments,
        fullText = Speakers.buildFullText(segments),
    )
}
