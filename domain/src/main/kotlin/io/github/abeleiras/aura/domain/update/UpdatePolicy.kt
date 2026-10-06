package io.github.abeleiras.aura.domain.update

import kotlin.time.Duration.Companion.hours

/** When to check and when it is safe to install (FR-006-04, FR-006-08). */
object UpdatePolicy {
    private val CHECK_INTERVAL_MILLIS = 24.hours.inWholeMilliseconds

    /** At most once every 24 h on app open, unless the user asks ([force]) or turned it off. */
    fun shouldCheck(nowMillis: Long, lastCheckMillis: Long, autoCheckEnabled: Boolean, force: Boolean = false): Boolean =
        force || (autoCheckEnabled && (lastCheckMillis <= 0L || nowMillis - lastCheckMillis >= CHECK_INTERVAL_MILLIS))

    /** Installing closes the app, so it waits for a recording (or processing) to finish. */
    fun canInstallNow(recordingActive: Boolean, processingActive: Boolean = false): Boolean =
        !recordingActive && !processingActive
}
