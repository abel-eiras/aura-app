package io.github.abeleiras.aura.domain.recording

/**
 * Audio formats Aura keeps and sends to the provider: what Gemini's Files API accepts (spec 009, FR-009-02).
 * `m4a` is mapped to `audio/aac`, which is not yet verified against the real API.
 */
object AudioFormats {
    private val mimeByExtension = linkedMapOf(
        "ogg" to "audio/ogg",
        "oga" to "audio/ogg",
        "opus" to "audio/ogg",
        "mp3" to "audio/mp3",
        "wav" to "audio/wav",
        "flac" to "audio/flac",
        "aac" to "audio/aac",
        "m4a" to "audio/aac",
        "aiff" to "audio/aiff",
    )

    /** Android reports different types for the same format depending on the app that shares it. */
    private val extensionByMime = mapOf(
        "audio/ogg" to "ogg",
        "application/ogg" to "ogg",
        "audio/opus" to "opus",
        "audio/mpeg" to "mp3",
        "audio/mp3" to "mp3",
        "audio/wav" to "wav",
        "audio/x-wav" to "wav",
        "audio/wave" to "wav",
        "audio/flac" to "flac",
        "audio/x-flac" to "flac",
        "audio/aac" to "aac",
        "audio/mp4" to "m4a",
        "audio/x-m4a" to "m4a",
        "audio/m4a" to "m4a",
        "audio/aiff" to "aiff",
        "audio/x-aiff" to "aiff",
    )

    val supportedExtensions: List<String> = mimeByExtension.keys.toList()

    fun isSupported(extension: String): Boolean = extension.lowercase() in mimeByExtension

    /** Type to send to the provider; unknown extensions are treated as Ogg, what Aura records. */
    fun mimeType(extension: String): String = mimeByExtension[extension.lowercase()] ?: "audio/ogg"

    /**
     * The extension to keep a file under: the one in [displayName] if supported, else the one that matches
     * [mimeType]. Null means the format is not supported.
     */
    fun extensionFor(displayName: String?, mimeType: String?): String? {
        val fromName = displayName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        if (fromName in mimeByExtension) return fromName
        val baseMime = mimeType?.substringBefore(';')?.trim()?.lowercase()
        return baseMime?.let { extensionByMime[it] }
    }
}
