package io.github.abeleiras.aura.domain.processing

import io.github.abeleiras.aura.domain.transcript.Transcript
import java.io.File

/** Languages the user speaks; passed to the provider as a hint, never as a hard rule (FR-003-04). */
data class LanguageHints(val primary: String, val secondary: String? = null)

/**
 * What the rest of the app needs from an AI provider (ADR-0002). Implementations
 * throw [ProviderException]. Adding a provider means adding one implementation.
 */
interface AiProvider {
    /** Step 1: audio to a transcript with speakers. */
    suspend fun transcribe(audio: File, hints: LanguageHints): Transcript

    /** Step 2: plain chat completion used for classification and drafting. */
    suspend fun complete(system: String, user: String, model: String?, temperature: Double): String
}
