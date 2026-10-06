package io.github.abeleiras.aura.domain.notes

/** Step 2a of processing: choose a note type (FR-003-05). Mirrors aura-transcribe's clasificar.py. */
object NoteClassifier {

    /** System prompt: the model sees only each type's [NoteType.criterion]. */
    fun systemPrompt(types: List<NoteType>): String {
        val ids = types.joinToString(", ") { it.id }
        val criteria = types.joinToString("\n") { "- ${it.id}: ${it.criterion}." }
        return "Clasificas transcripciones de notas de voz en uno de estos tipos EXACTOS:\n\n" +
            "$criteria\n\n" +
            "Responde ÚNICAMENTE con uno de estos identificadores, en minúsculas y sin nada más: $ids."
    }

    fun userPrompt(language: String, transcriptText: String): String =
        "Idioma detectado: $language\n\n$transcriptText"

    /**
     * Maps the model's answer to a type. An unknown answer falls back to the
     * type flagged as default, or the first one (spec 005, edge cases).
     */
    fun resolve(answer: String, types: List<NoteType>): Resolution {
        require(types.isNotEmpty()) { "At least one note type is required" }
        val cleaned = answer.trim().trim('`', '"', '\'', '.').lowercase()
        types.firstOrNull { it.id == cleaned }?.let { return Resolution(it, fellBack = false) }
        val fallback = types.firstOrNull { it.isDefault } ?: types.first()
        return Resolution(fallback, fellBack = true)
    }

    data class Resolution(val type: NoteType, val fellBack: Boolean)
}
