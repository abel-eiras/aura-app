package io.github.abeleiras.aura.domain.recording

/**
 * Opus-in-OGG bitrates (FR-001-04). Speech at 32 kbps is about 14 MB per hour, which
 * keeps uploads to AI providers small (CE-001-3).
 */
enum class AudioQuality(val bitRate: Int) {
    NORMAL(32_000),
    HIGH(64_000);

    companion object {
        val DEFAULT = NORMAL
        const val SAMPLE_RATE = 48_000
        const val CHANNELS = 1

        fun fromNameOrDefault(name: String?): AudioQuality = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
