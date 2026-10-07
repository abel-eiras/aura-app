package io.github.abeleiras.aura.domain.crash

/**
 * Text of the local error report (spec 007). It only ever holds technical facts about the
 * app and the phone model, never recordings, notes, credentials or user identifiers (FR-007-03).
 */
object CrashReportFormat {
    const val MAX_CHARS = 20_000
    private const val TRUNCATED = "\n… (truncated)"

    fun build(
        timestamp: String,
        appVersion: String,
        device: String,
        androidVersion: String,
        thread: String,
        stackTrace: String,
    ): String {
        val report = buildString {
            appendLine("Aura error report")
            appendLine("Time: $timestamp")
            appendLine("Aura version: $appVersion")
            appendLine("Device: $device")
            appendLine("Android: $androidVersion")
            appendLine("Thread: $thread")
            appendLine()
            append(stackTrace.trim())
        }
        return if (report.length <= MAX_CHARS) report else report.take(MAX_CHARS - TRUNCATED.length) + TRUNCATED
    }
}
