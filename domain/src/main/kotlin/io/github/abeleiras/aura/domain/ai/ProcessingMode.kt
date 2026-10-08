package io.github.abeleiras.aura.domain.ai

/** How the user wants recordings processed (FR-002-02). */
enum class ProcessingMode {
    NONE,
    GEMINI,
    OPENROUTER,
    ;

    companion object {
        fun fromNameOrDefault(name: String?): ProcessingMode = entries.firstOrNull { it.name == name } ?: NONE
    }
}

/** State of the stored credential as last learned (FR-002-09). */
enum class CredentialState {
    UNCHECKED,
    VALID,
    INVALID,
    ;

    companion object {
        fun fromNameOrDefault(name: String?): CredentialState = entries.firstOrNull { it.name == name } ?: UNCHECKED
    }
}

/**
 * Default models, the single place they are defined (FR-002-07). Model names change quickly, so these are only
 * the starting point: see [ModelPicker].
 */
object ProviderDefaults {
    // An alias that Google keeps pointing at the current Flash model: a fixed name was retired within weeks
    // (found with 0.3.0-beta.3). After a successful key check the app replaces it with a model the key really has.
    const val GEMINI_TRANSCRIBE_MODEL = "gemini-flash-lite-latest"
    const val GEMINI_DRAFT_MODEL = "gemini-flash-lite-latest"
    const val GEMINI_KEYS_URL = "https://aistudio.google.com/apikey"
    const val GEMINI_TERMS_URL = "https://ai.google.dev/gemini-api/terms"
}

/** FR-003-13: nothing leaves the phone unless the mode is set, a credential exists and the notice was accepted. */
fun canSendToProvider(mode: ProcessingMode, hasCredential: Boolean, privacyAccepted: Boolean): Boolean =
    mode != ProcessingMode.NONE && hasCredential && privacyAccepted

/** Why recordings are or are not being processed; the UI says it out loud instead of staying silent. */
enum class ProviderReadiness { NO_PROVIDER, NO_KEY, KEY_INVALID, NEEDS_PRIVACY, READY }

fun providerReadiness(mode: ProcessingMode, hasCredential: Boolean, credential: CredentialState, privacyAccepted: Boolean): ProviderReadiness = when {
    mode == ProcessingMode.NONE -> ProviderReadiness.NO_PROVIDER
    !hasCredential -> ProviderReadiness.NO_KEY
    credential == CredentialState.INVALID -> ProviderReadiness.KEY_INVALID
    !privacyAccepted -> ProviderReadiness.NEEDS_PRIVACY
    else -> ProviderReadiness.READY
}
