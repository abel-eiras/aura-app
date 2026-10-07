package io.github.abeleiras.aura.domain.processing

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class JobStatus { QUEUED, WAITING_NETWORK, TRANSCRIBING, DRAFTING, READY, NO_CONTENT, ERROR }

enum class ProcessingStep { TRANSCRIBE, DRAFT }

/** Why a step failed; the UI maps each reason to an actionable message (FR-003-07, spec 003 HU-003-3). */
enum class ErrorReason {
    NETWORK,
    RATE_LIMITED,
    PROVIDER_UNAVAILABLE,
    INVALID_CREDENTIAL,
    QUOTA_EXHAUSTED,
    INSUFFICIENT_FUNDS,
    AUDIO_TOO_LONG,
    INVALID_RESPONSE,
    UNKNOWN,
}

/** Thrown by providers; [transient] errors are retried automatically. */
class ProviderException(
    val reason: ErrorReason,
    val transient: Boolean,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message ?: reason.name, cause)

sealed interface FailureDecision {
    data class Retry(val after: Duration) : FailureDecision
    data class GiveUp(val reason: ErrorReason) : FailureDecision
}

/** Retry and resume rules for the two-step pipeline (FR-003-01, FR-003-07, FR-003-08). */
object ProcessingPolicy {
    const val MAX_ATTEMPTS = 5
    const val MAX_INVALID_RESPONSE_ATTEMPTS = 2
    const val MAX_AUDIO_SECONDS = 3 * 60 * 60

    private val BASE_DELAY = 15.seconds
    private val MAX_DELAY = 15.minutes

    /** Exponential backoff: 15 s, 30 s, 1 min, 2 min, ... capped at 15 min. [attempt] is 1-based. */
    fun backoff(attempt: Int): Duration {
        require(attempt >= 1)
        val factor = 1L shl (attempt - 1).coerceAtMost(20)
        return minOf(BASE_DELAY * factor.toDouble(), MAX_DELAY)
    }

    /** [attempts] = failed attempts so far on the current step, including this one. */
    fun onFailure(error: ProviderException, attempts: Int): FailureDecision = when {
        !error.transient -> FailureDecision.GiveUp(error.reason)
        // Spec 003, edge case: an answer in the wrong format is retried once, not five times.
        error.reason == ErrorReason.INVALID_RESPONSE && attempts >= MAX_INVALID_RESPONSE_ATTEMPTS -> FailureDecision.GiveUp(error.reason)
        attempts >= MAX_ATTEMPTS -> FailureDecision.GiveUp(error.reason)
        else -> FailureDecision.Retry(backoff(attempts))
    }

    /** First step without a persisted result, or null when there's nothing left to do (FR-003-01). */
    fun nextStep(hasTranscript: Boolean, hasNote: Boolean, noContent: Boolean = false): ProcessingStep? = when {
        noContent -> null
        !hasTranscript -> ProcessingStep.TRANSCRIBE
        !hasNote -> ProcessingStep.DRAFT
        else -> null
    }

    fun exceedsMaxDuration(durationSeconds: Double): Boolean = durationSeconds > MAX_AUDIO_SECONDS
}
