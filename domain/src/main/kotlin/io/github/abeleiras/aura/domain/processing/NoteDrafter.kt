package io.github.abeleiras.aura.domain.processing

import io.github.abeleiras.aura.domain.notes.NoteClassifier
import io.github.abeleiras.aura.domain.notes.NoteType
import io.github.abeleiras.aura.domain.transcript.Transcript

data class DraftedNote(val type: NoteType, val body: String, val classifiedByFallback: Boolean)

/** Step 2: classify the transcript, then draft the note with that type's prompt (FR-003-05). */
class NoteDrafter(
    private val provider: AiProvider,
    private val types: List<NoteType>,
    private val defaultModel: String?,
) {
    init {
        require(types.isNotEmpty()) { "At least one note type is required" }
    }

    /** Classification only, so "Change type" can skip it (HU-003-4). */
    suspend fun classify(transcript: Transcript): NoteClassifier.Resolution {
        val answer = provider.complete(
            system = NoteClassifier.systemPrompt(types),
            user = NoteClassifier.userPrompt(transcript.metadata.languageUsed, transcript.fullText),
            model = defaultModel,
            temperature = 0.0,
        )
        return NoteClassifier.resolve(answer, types)
    }

    suspend fun draft(transcript: Transcript, type: NoteType): String {
        val body = provider.complete(
            system = type.draftingPrompt,
            user = transcript.fullText,
            model = type.model ?: defaultModel,
            temperature = 0.3,
        ).trim()
        if (body.isEmpty()) throw ProviderException(ErrorReason.INVALID_RESPONSE, transient = true, "Empty note")
        return body
    }

    /** Full step 2. Never called for an empty transcript (FR-003-09). */
    suspend fun run(transcript: Transcript): DraftedNote {
        require(transcript.hasContent) { "Empty transcript has no note" }
        val resolution = classify(transcript)
        return DraftedNote(resolution.type, draft(transcript, resolution.type), resolution.fellBack)
    }
}
