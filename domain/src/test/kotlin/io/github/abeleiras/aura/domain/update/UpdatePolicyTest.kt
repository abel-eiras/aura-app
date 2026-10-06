package io.github.abeleiras.aura.domain.update

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdatePolicyTest {
    private val hour = 3_600_000L
    private val now = 100 * 24 * hour

    @Test
    fun `FR-006-04 checks at most once every 24 hours`() {
        assertTrue(UpdatePolicy.shouldCheck(now, lastCheckMillis = 0, autoCheckEnabled = true))
        assertFalse(UpdatePolicy.shouldCheck(now, lastCheckMillis = now - 23 * hour, autoCheckEnabled = true))
        assertTrue(UpdatePolicy.shouldCheck(now, lastCheckMillis = now - 24 * hour, autoCheckEnabled = true))
    }

    @Test
    fun `FR-006-04 no request at all when automatic checks are off, unless the user asks`() {
        assertFalse(UpdatePolicy.shouldCheck(now, 0, autoCheckEnabled = false))
        assertTrue(UpdatePolicy.shouldCheck(now, now, autoCheckEnabled = false, force = true))
    }

    @Test
    fun `FR-006-08 never installs during a recording or processing`() {
        assertTrue(UpdatePolicy.canInstallNow(recordingActive = false))
        assertFalse(UpdatePolicy.canInstallNow(recordingActive = true))
        assertFalse(UpdatePolicy.canInstallNow(recordingActive = false, processingActive = true))
    }
}
