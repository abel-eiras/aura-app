package io.github.abeleiras.aura.domain.update

enum class UpdateFailure {
    /** No connection, timeout, or the server dropped it. */
    NETWORK,

    /** GitHub's anonymous rate limit (403/429): try again in the next cycle (spec 006, edge cases). */
    RATE_LIMITED,

    /** The server answered, but not with what a release looks like. */
    BAD_RESPONSE,

    /** The downloaded file doesn't match the published SHA-256 (FR-006-06). */
    HASH_MISMATCH,

    /** The APK's signing certificate isn't the installed app's (FR-006-06). */
    SIGNATURE_MISMATCH,

    /** Not enough free space to download the APK. */
    NO_SPACE,

    /** The system installer reported a failure. */
    INSTALL_FAILED,
}

class UpdateException(val failure: UpdateFailure, message: String? = null, cause: Throwable? = null) :
    Exception(message ?: failure.name, cause)
