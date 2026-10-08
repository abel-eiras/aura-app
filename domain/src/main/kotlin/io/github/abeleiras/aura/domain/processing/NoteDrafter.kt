package io.github.abeleiras.aura.domain.processing

import io.github.abeleiras.aura.domain.notes.NoteClassifier
import io.github.abeleiras.aura.domain.notes.NoteType
import io.github.abeleiras.aura.domain.transcript.Transcript
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    /**
     * Full step 2. Never called for an empty transcript (FR-003-09). With the usual types it costs ONE request
     * (classify and draft together) instead of two: free provider tiers allow very few requests per day.
     * Types that name their own model need the separate calls, since the model depends on the chosen type.
     */
    suspend fun run(transcript: Transcript): DraftedNote {
        require(transcript.hasContent) { "Empty transcript has no note" }
        if (types.none { it.model != null }) return runCombined(transcript)
        val resolution = classify(transcript)
        return DraftedNote(resolution.type, draft(transcript, resolution.type), resolution.fellBack)
    }

    private suspend fun runCombined(transcript: Transcript): DraftedNote {
        val answer = provider.complete(
            system = combinedSystemPrompt(types),
            user = NoteClassifier.userPrompt(transcript.metadata.languageUsed, transcript.fullText),
            model = defaultModel,
            temperature = 0.3,
        )
        val parsed = parseCombined(answer)
        val resolution = NoteClassifier.resolve(parsed.first, types)
        return DraftedNote(resolution.type, parsed.second, resolution.fellBack)
    }

    private fun parseCombined(answer: String): Pair<String, String> {
        val text = answer.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = try {
            Json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            throw ProviderException(ErrorReason.INVALID_RESPONSE, transient = true, message = "Answer is not the expected JSON", cause = e)
        }
        val type = obj["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val note = obj["note"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (note.isEmpty()) throw ProviderException(ErrorReason.INVALID_RESPONSE, transient = true, message = "Empty note")
        return type to note
    }

    companion object {
        /** One prompt that does both jobs: the model sees each type's criterion, picks one, then follows that type's instructions. */
        fun combinedSystemPrompt(types: List<NoteType>): String {
            val criteria = types.joinToString("\n") { "- ${it.id}: ${it.criterion}." }
            val instructions = types.joinToString("\n\n") { "### Instrucciones del tipo ${it.id}\n${it.draftingPrompt}" }
            return "Conviertes la transcripción de una grabación de voz en una nota, en dos pasos.\n\n" +
                "Paso 1: elige UNO de estos tipos EXACTOS usando solo su criterio:\n$criteria\n\n" +
                "Paso 2: redacta la nota siguiendo EXACTAMENTE las instrucciones del tipo elegido (y solo las de ese tipo):\n\n" +
                "$instructions\n\n" +
                "Responde ÚNICAMENTE con un objeto JSON válido, sin texto alrededor y sin bloques de código, con esta forma: " +
                "{\"type\": \"<identificador del tipo>\", \"note\": \"<la nota en Markdown, con saltos de línea escapados>\"}."
        }
    }
}
