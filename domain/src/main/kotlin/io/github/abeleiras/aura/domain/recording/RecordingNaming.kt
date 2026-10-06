package io.github.abeleiras.aura.domain.recording

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** `aura_AAAAMMDD_HHMMSS.ogg` in local time, the pattern aura-transcribe reads dates from (FR-001-08). */
object RecordingNaming {
    const val EXTENSION = "ogg"
    private const val PREFIX = "aura_"
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val fileNamePattern = Regex("^aura_(\\d{8}_\\d{6})\\.$EXTENSION$")

    fun fileName(startedAtMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        PREFIX + stamp.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(startedAtMillis), zone)) + ".$EXTENSION"

    /** Start time encoded in [fileName], or null if it isn't one of ours. */
    fun startedAtMillis(fileName: String, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val m = fileNamePattern.matchEntire(fileName) ?: return null
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
