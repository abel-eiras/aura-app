package io.github.abeleiras.aura.domain.recording

/** Thresholds from spec 001. */
object RecordingPolicy {
    /** Recordings shorter than this are discarded as accidental taps (FR-001-11). */
    const val MIN_DURATION_MILLIS = 1_000L

    /** Below this much free space an active recording is stopped and kept (FR-001-12). */
    const val LOW_STORAGE_BYTES = 100L * 1024 * 1024

    /** Total size of recordings above which the list suggests exporting and deleting (spec 001, open question). */
    const val LARGE_ARCHIVE_BYTES = 2L * 1024 * 1024 * 1024

    fun isTooShort(durationMillis: Long): Boolean = durationMillis < MIN_DURATION_MILLIS

    fun isStorageLow(freeBytes: Long): Boolean = freeBytes < LOW_STORAGE_BYTES

    fun isArchiveLarge(totalBytes: Long): Boolean = totalBytes > LARGE_ARCHIVE_BYTES

    /** `m:ss`, or `h:mm:ss` from one hour up. */
    fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
