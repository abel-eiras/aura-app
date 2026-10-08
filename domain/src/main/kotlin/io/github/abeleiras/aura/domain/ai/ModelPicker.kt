package io.github.abeleiras.aura.domain.ai

/** Chooses a model from what the user's key can really use, instead of trusting a name that may be retired. */
object ModelPicker {
    private val flash = Regex("^gemini-(\\d+)(?:\\.(\\d+))?-flash$")
    private val lite = Regex("^gemini-(\\d+)(?:\\.(\\d+))?-flash-lite$")
    private const val ALIAS = "gemini-flash-latest"
    private const val LITE_ALIAS = "gemini-flash-lite-latest"

    /**
     * What the app uses by default. On the free tier the newest Flash had 20 requests a day while the Flash-Lite
     * models had 500 (seen in a real key's rate-limit page), so Lite comes first; the user can pick a bigger model in
     * Advanced settings if its quality matters more than its quota.
     */
    fun bestForFreeTier(models: List<GeminiModel>): String? {
        val ids = models.map { it.id }
        if (LITE_ALIAS in ids) return LITE_ALIAS
        return highest(ids, lite) ?: bestFlash(models)
    }

    private fun highest(ids: List<String>, pattern: Regex): String? =
        ids.mapNotNull { id ->
            pattern.matchEntire(id)?.let { m -> id to (m.groupValues[1].toInt() * 1000 + (m.groupValues[2].toIntOrNull() ?: 0)) }
        }.maxByOrNull { it.second }?.first

    /** The `-latest` alias if the key has it, else the highest stable `gemini-X.Y-flash`, else null. */
    fun bestFlash(models: List<GeminiModel>): String? {
        val ids = models.map { it.id }
        if (ALIAS in ids) return ALIAS
        return highest(ids, flash)
    }

    /** Short list shown in Advanced settings: Flash models, newest first. */
    fun flashModels(models: List<GeminiModel>): List<String> =
        models.map { it.id }.filter { "flash" in it && !it.contains("image") && !it.contains("tts") && !it.contains("live") }.sortedDescending()
}
