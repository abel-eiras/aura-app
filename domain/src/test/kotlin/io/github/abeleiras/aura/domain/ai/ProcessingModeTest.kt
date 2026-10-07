package io.github.abeleiras.aura.domain.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProcessingModeTest {
    // FR-003-13
    @Test
    fun `nothing is sent without mode, credential and accepted notice`() {
        assertTrue(canSendToProvider(ProcessingMode.GEMINI, hasCredential = true, privacyAccepted = true))
        assertFalse(canSendToProvider(ProcessingMode.NONE, true, true))
        assertFalse(canSendToProvider(ProcessingMode.GEMINI, false, true))
        assertFalse(canSendToProvider(ProcessingMode.GEMINI, true, false))
    }

    @Test
    fun `unknown stored names fall back safely`() {
        assertEquals(ProcessingMode.NONE, ProcessingMode.fromNameOrDefault("bogus"))
        assertEquals(ProcessingMode.NONE, ProcessingMode.fromNameOrDefault(null))
        assertEquals(CredentialState.UNCHECKED, CredentialState.fromNameOrDefault("x"))
    }

    @Test
    fun `readiness names the first thing that is missing`() {
        fun r(mode: ProcessingMode, key: Boolean, c: CredentialState, p: Boolean) = providerReadiness(mode, key, c, p)
        assertEquals(ProviderReadiness.NO_PROVIDER, r(ProcessingMode.NONE, true, CredentialState.VALID, true))
        assertEquals(ProviderReadiness.NO_KEY, r(ProcessingMode.GEMINI, false, CredentialState.UNCHECKED, false))
        assertEquals(ProviderReadiness.KEY_INVALID, r(ProcessingMode.GEMINI, true, CredentialState.INVALID, true))
        assertEquals(ProviderReadiness.NEEDS_PRIVACY, r(ProcessingMode.GEMINI, true, CredentialState.VALID, false))
        assertEquals(ProviderReadiness.NEEDS_PRIVACY, r(ProcessingMode.GEMINI, true, CredentialState.UNCHECKED, false))
        assertEquals(ProviderReadiness.READY, r(ProcessingMode.GEMINI, true, CredentialState.VALID, true))
    }
}
