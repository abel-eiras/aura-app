package io.github.abeleiras.aura.domain.recording

/** What the recorder is doing now. Shared by service, tile, widget and UI so they never disagree. */
sealed interface RecordingStatus {
    data object Idle : RecordingStatus

    /** [accumulatedPausedMillis] is the time spent in earlier pause/resume cycles. */
    data class Recording(val startedAtMillis: Long, val accumulatedPausedMillis: Long = 0L) : RecordingStatus

    data class Paused(
        val startedAtMillis: Long,
        val pausedAtMillis: Long,
        val accumulatedPausedMillis: Long,
    ) : RecordingStatus

    val isActive: Boolean get() = this !is Idle

    /** Recorded time (pauses excluded) as of [nowMillis]; zero while idle. */
    fun elapsedMillis(nowMillis: Long): Long = when (this) {
        Idle -> 0L
        is Recording -> (nowMillis - startedAtMillis - accumulatedPausedMillis).coerceAtLeast(0L)
        is Paused -> (pausedAtMillis - startedAtMillis - accumulatedPausedMillis).coerceAtLeast(0L)
    }
}

/** Valid transitions (FR-001-05); an invalid one returns the status unchanged. */
object RecordingTransitions {
    fun start(status: RecordingStatus, nowMillis: Long): RecordingStatus =
        if (status is RecordingStatus.Idle) RecordingStatus.Recording(nowMillis) else status

    fun pause(status: RecordingStatus, nowMillis: Long): RecordingStatus =
        if (status is RecordingStatus.Recording) {
            RecordingStatus.Paused(status.startedAtMillis, nowMillis, status.accumulatedPausedMillis)
        } else {
            status
        }

    fun resume(status: RecordingStatus, nowMillis: Long): RecordingStatus =
        if (status is RecordingStatus.Paused) {
            RecordingStatus.Recording(
                status.startedAtMillis,
                status.accumulatedPausedMillis + (nowMillis - status.pausedAtMillis),
            )
        } else {
            status
        }

    fun stop(@Suppress("UNUSED_PARAMETER") status: RecordingStatus): RecordingStatus = RecordingStatus.Idle
}
