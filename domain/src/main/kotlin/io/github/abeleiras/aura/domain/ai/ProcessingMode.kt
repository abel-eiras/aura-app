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
 * Default models, the single place they are defined (FR-002-07). Not verified against the live API yet:
 * `scripts/probe-gemini.sh list-models` shows what a key can actually use.
 */
object ProviderDefaults {
    const val GEMINI_TRANSCRIBE_MODEL = "gemini-2.5-flash"
    const val GEMINI_DRAFT_MODEL = "gemini-2.5-flash"
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
