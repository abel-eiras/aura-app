package io.github.abeleiras.aura.domain.transcript

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Mirrors `specs/003-procesado-ia/contracts/transcripcion.schema.json` (FR-003-02). */
@Serializable
data class Transcript(
    val metadata: TranscriptMetadata,
    val segments: List<Segment>,
    @SerialName("full_text") val fullText: String,
) {
    /** False for silent/empty audio: no note is drafted (FR-003-09). */
    val hasContent: Boolean get() = fullText.isNotBlank()

    fun toJson(): String = JSON.encodeToString(serializer(), this)

    companion object {
        // Unknown keys (e.g. aura-transcribe's per-word timestamps) are ignored on read.
        private val JSON = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            encodeDefaults = false
        }

        fun fromJson(text: String): Transcript = JSON.decodeFromString(serializer(), text)
    }
}

@Serializable
data class TranscriptMetadata(
    @SerialName("source_file") val sourceFile: String,
    @SerialName("processed_at") val processedAt: String,
    @SerialName("duration_seconds") val durationSeconds: Double,
    @SerialName("language_detected") val languageDetected: String? = null,
    @SerialName("language_confidence") val languageConfidence: Double? = null,
    @SerialName("language_used") val languageUsed: String,
    @SerialName("num_speakers_detected") val numSpeakersDetected: Int,
    val alignment: Boolean? = null,
    @SerialName("pipeline_version") val pipelineVersion: String,
    @SerialName("processing_seconds") val processingSeconds: Double? = null,
    @SerialName("whisper_model") val whisperModel: String? = null,
    @SerialName("diarization_model") val diarizationModel: String? = null,
    val provider: String? = null,
    @SerialName("transcription_model") val transcriptionModel: String? = null,
    @SerialName("speaker_names") val speakerNames: Map<String, String>? = null,
)

@Serializable
data class Segment(
    val id: Int,
    val speaker: String,
    val start: Double,
    val end: Double,
    val text: String,
)
