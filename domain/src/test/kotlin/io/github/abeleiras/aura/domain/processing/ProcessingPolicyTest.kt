package io.github.abeleiras.aura.domain.processing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ProcessingPolicyTest {

    @Test
    fun `FR-003-07 backoff doubles from 15s and caps at 15 minutes`() {
        assertEquals(listOf(15.seconds, 30.seconds, 60.seconds, 120.seconds, 240.seconds), (1..5).map { ProcessingPolicy.backoff(it) })
        assertEquals(15.minutes, ProcessingPolicy.backoff(30))
    }

    @Test
    fun `FR-003-07 transient errors retry up to 5 attempts, permanent ones stop at once`() {
        val net = ProviderException(ErrorReason.NETWORK, transient = true)
        assertEquals(FailureDecision.Retry(15.seconds), ProcessingPolicy.onFailure(net, attempts = 1))
        assertEquals(FailureDecision.GiveUp(ErrorReason.NETWORK), ProcessingPolicy.onFailure(net, attempts = 5))
        val key = ProviderException(ErrorReason.INVALID_CREDENTIAL, transient = false)
        assertEquals(FailureDecision.GiveUp(ErrorReason.INVALID_CREDENTIAL), ProcessingPolicy.onFailure(key, attempts = 1))
    }

    @Test
    fun `FR-003-01 resume starts at the first step without a persisted result`() {
        assertEquals(ProcessingStep.TRANSCRIBE, ProcessingPolicy.nextStep(hasTranscript = false, hasNote = false))
        assertEquals(ProcessingStep.DRAFT, ProcessingPolicy.nextStep(hasTranscript = true, hasNote = false))
        assertNull(ProcessingPolicy.nextStep(hasTranscript = true, hasNote = true))
        assertNull(ProcessingPolicy.nextStep(hasTranscript = true, hasNote = false, noContent = true))
    }

    @Test
    fun `FR-003-08 audio over three hours is rejected before upload`() {
        assertTrue(ProcessingPolicy.exceedsMaxDuration(3 * 3600 + 1.0))
        assertTrue(!ProcessingPolicy.exceedsMaxDuration(3 * 3600.0))
    }
}
