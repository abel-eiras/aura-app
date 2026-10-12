package io.github.abeleiras.aura.domain.recording

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * `aura_AAAAMMDD_HHMMSS.<ext>` in local time, the pattern aura-transcribe reads dates from (FR-001-08).
 * Recordings made by Aura are `.ogg`; imported audio (spec 009) keeps its own supported extension.
 */
object RecordingNaming {
    const val EXTENSION = "ogg"
    private const val PREFIX = "aura_"
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val fileNamePattern = Regex("^aura_(\\d{8}_\\d{6})\\.([A-Za-z0-9]+)$")

    fun fileName(startedAtMillis: Long, zone: ZoneId = ZoneId.systemDefault(), extension: String = EXTENSION): String =
        PREFIX + stamp.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(startedAtMillis), zone)) + ".$extension"

    /**
     * A name for an imported file that is not in [taken]: the start time is moved on a second at a time until
     * it is free, so two audios with the same date never overwrite each other (spec 009, edge cases).
     * Returns the name and the start time it ended up with.
     */
    fun uniqueFileName(startedAtMillis: Long, extension: String, taken: Set<String>, zone: ZoneId = ZoneId.systemDefault()): Pair<String, Long> {
        var at = startedAtMillis
        while (true) {
            val name = fileName(at, zone, extension)
            if (name !in taken) return name to at
            at += 1_000
        }
    }

    /** True for names Aura creates for recordings: the pattern with a supported audio extension. */
    fun isRecordingFile(fileName: String): Boolean {
        val m = fileNamePattern.matchEntire(fileName) ?: return false
        return AudioFormats.isSupported(m.groupValues[2])
    }

    /** Start time encoded in [fileName], or null if it isn't one of ours. */
    fun startedAtMillis(fileName: String, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val m = fileNamePattern.matchEntire(fileName) ?: return null
        if (!AudioFormats.isSupported(m.groupValues[2])) return null
        return try {
            LocalDateTime.parse(m.groupValues[1], stamp).atZone(zone).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /** `AAAA-MM-DD` date part used in exported note names (spec 004). */
    fun date(startedAtMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(startedAtMillis).atZone(zone).toLocalDate().toString()
}
