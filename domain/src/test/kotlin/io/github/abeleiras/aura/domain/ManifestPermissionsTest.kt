package io.github.abeleiras.aura.domain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The app's manifest can't be run in JVM tests, but it can be read. 0.1.0 shipped without INTERNET
 * (the code makes network calls from the first launch) and crashed on open; nothing in CI noticed.
 */
class ManifestPermissionsTest {
    private val manifest = File("../app/src/main/AndroidManifest.xml").readText()
    private val declared = Regex("<uses-permission[^>]*android:name=\"([^\"]+)\"").findAll(manifest).map { it.groupValues[1] }.toSet()

    private fun requires(permission: String, why: String) =
        assertTrue("android.permission.$permission" in declared, "AndroidManifest.xml must declare $permission: $why")

    @Test
    fun `the manifest declares the permissions the code relies on`() {
        requires("INTERNET", "the update check runs at startup (spec 006)")
        requires("RECORD_AUDIO", "recording (spec 001)")
        requires("FOREGROUND_SERVICE", "the recording service (spec 001)")
        requires("FOREGROUND_SERVICE_MICROPHONE", "the recording service uses the microphone type (spec 001)")
        requires("POST_NOTIFICATIONS", "the recording indicator (spec 001)")
        requires("REQUEST_INSTALL_PACKAGES", "installing updates (spec 006)")
    }

    @Test
    fun `no permission outside the project's policy sneaks in`() {
        val allowed = setOf(
            "INTERNET", "RECORD_AUDIO", "FOREGROUND_SERVICE", "FOREGROUND_SERVICE_MICROPHONE", "POST_NOTIFICATIONS",
            "READ_PHONE_STATE", "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", "REQUEST_INSTALL_PACKAGES",
        ).map { "android.permission.$it" }.toSet()
        val unexpected = declared - allowed
        assertTrue(unexpected.isEmpty(), "Unreviewed permissions (update this list and the privacy notes together): $unexpected")
    }
}
