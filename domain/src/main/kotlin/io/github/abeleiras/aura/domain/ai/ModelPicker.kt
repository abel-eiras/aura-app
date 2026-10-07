package io.github.abeleiras.aura.domain.ai

/** Chooses a model from what the user's key can really use, instead of trusting a name that may be retired. */
object ModelPicker {
    private val flash = Regex("^gemini-(\\d+)(?:\\.(\\d+))?-flash$")
    private const val ALIAS = "gemini-flash-latest"

    /** The `-latest` alias if the key has it, else the highest stable `gemini-X.Y-flash`, else null. */
    fun bestFlash(models: List<GeminiModel>): String? {
        val ids = models.map { it.id }
        if (ALIAS in ids) return ALIAS
        return ids.mapNotNull { id ->
            flash.matchEntire(id)?.let { m -> id to (m.groupValues[1].toInt() * 1000 + (m.groupValues[2].toIntOrNull() ?: 0)) }
        }.maxByOrNull { it.second }?.first
    }

    /** Short list shown in Advanced settings: Flash models, newest first. */
    fun flashModels(models: List<GeminiModel>): List<String> =
        models.map { it.id }.filter { "flash" in it && !it.contains("image") && !it.contains("tts") && !it.contains("live") }.sortedDescending()
}
