package io.github.abeleiras.aura.domain.crash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrashReportFormatTest {
    private fun build(trace: String) = CrashReportFormat.build(
        timestamp = "2026-10-07 10:15:00 +0200",
        appVersion = "0.2.0",
        device = "Google Pixel 10 Pro",
        androidVersion = "16 (API 36)",
        thread = "main",
        stackTrace = trace,
    )

    @Test
    fun `FR-007-03 the report carries app, device, Android, thread and the full trace`() {
        val report = build("java.lang.IllegalStateException: boom\n\tat a.b.C.d(C.kt:1)\nCaused by: java.io.IOException: x")
        listOf(
            "Time: 2026-10-07 10:15:00 +0200", "Aura version: 0.2.0", "Device: Google Pixel 10 Pro",
            "Android: 16 (API 36)", "Thread: main", "IllegalStateException: boom", "Caused by: java.io.IOException: x",
        ).forEach { assertTrue(it in report, it) }
    }

    @Test
    fun `FR-007-07 a very long trace is cut to the limit and says so`() {
        val report = build("x".repeat(100_000))
        assertEquals(CrashReportFormat.MAX_CHARS, report.length)
        assertTrue(report.endsWith("(truncated)"))
    }

    @Test
    fun `a short trace is left alone`() {
        assertFalse(build("short").contains("truncated"))
    }
}
