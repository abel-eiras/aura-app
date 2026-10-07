package io.github.abeleiras.aura.data.ai

import io.github.abeleiras.aura.data.prefs.AppSettings
import io.github.abeleiras.aura.domain.ai.ApiKeys
import io.github.abeleiras.aura.domain.ai.CredentialCheck
import io.github.abeleiras.aura.domain.ai.CredentialState
import io.github.abeleiras.aura.domain.ai.GeminiClient
import io.github.abeleiras.aura.domain.ai.ProcessingMode
import io.github.abeleiras.aura.domain.ai.canSendToProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

data class ProviderState(
    val mode: ProcessingMode,
    /** Masked, e.g. `AIza…x9Zq`; the full key is never exposed after saving (FR-002-04 spirit). */
    val keyHint: String?,
    val credential: CredentialState,
    val checkedAtMillis: Long,
    val privacyAccepted: Boolean,
    val checking: Boolean = false,
    val lastCheck: CredentialCheck? = null,
    val saveFailed: Boolean = false,
) {
    val hasKey: Boolean get() = keyHint != null
    val canSend: Boolean get() = canSendToProvider(mode, hasKey, privacyAccepted)
}

/** Owns the provider setup (spec 002). Lives in [io.github.abeleiras.aura.AppContainer] so it survives navigation. */
class ProviderController(
    private val settings: AppSettings,
    private val credentials: CredentialStore,
    private val http: OkHttpClient,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(load())
    val state: StateFlow<ProviderState> = _state.asStateFlow()

    private fun load(): ProviderState {
        val key = credentials.get(CredentialStore.GEMINI_KEY)
        return ProviderState(
            mode = settings.processingMode,
            keyHint = key?.let(::mask),
            credential = if (key == null) CredentialState.UNCHECKED else settings.credentialState,
            checkedAtMillis = settings.credentialCheckedAtMillis,
            privacyAccepted = settings.privacyAcceptedGemini,
        )
    }

    fun selectMode(mode: ProcessingMode) {
        settings.processingMode = mode
        _state.update { it.copy(mode = mode, lastCheck = null, saveFailed = false) }
    }

    /** Stores [text] if it looks like a key and checks it right away. Returns false if it was rejected before saving. */
    fun saveAndCheck(text: String): Boolean {
        val key = text.trim()
        if (key.length < MIN_KEY_LENGTH) return false
        if (!credentials.put(CredentialStore.GEMINI_KEY, key)) {
            _state.update { it.copy(saveFailed = true) }
            return false
        }
        settings.credentialState = CredentialState.UNCHECKED
        _state.update {
            it.copy(keyHint = mask(key), credential = CredentialState.UNCHECKED, lastCheck = null, saveFailed = false)
        }
        check()
        return true
    }

    fun check() {
        val key = credentials.get(CredentialStore.GEMINI_KEY) ?: return
        if (_state.value.checking) return
        _state.update { it.copy(checking = true, lastCheck = null) }
        scope.launch {
            val result = GeminiClient(http, key).checkCredential()
            val now = System.currentTimeMillis()
            when (result) {
                is CredentialCheck.Valid -> store(CredentialState.VALID, now)
                CredentialCheck.InvalidKey -> store(CredentialState.INVALID, now)
                else -> Unit // couldn't tell: keep whatever we knew
            }
            _state.update {
                it.copy(
                    checking = false,
                    lastCheck = result,
                    credential = settings.credentialState,
                    checkedAtMillis = settings.credentialCheckedAtMillis,
                )
            }
        }
    }

    private fun store(state: CredentialState, at: Long) {
        settings.credentialState = state
        settings.credentialCheckedAtMillis = at
    }

    fun acceptPrivacy() {
        settings.privacyAcceptedGemini = true
        _state.update { it.copy(privacyAccepted = true) }
    }

    /** Deletes the credential from the device and goes back to "Record only" (HU-002-4). */
    fun disconnect() {
        credentials.remove(CredentialStore.GEMINI_KEY)
        settings.processingMode = ProcessingMode.NONE
        settings.credentialState = CredentialState.UNCHECKED
        settings.credentialCheckedAtMillis = 0L
        settings.privacyAcceptedGemini = false
        _state.value = load()
    }

    /** The key for the processing worker (spec 003); null unless FR-003-13 allows sending. */
    fun keyForSending(): String? =
        if (_state.value.canSend) credentials.get(CredentialStore.GEMINI_KEY) else null

    fun clipboardKeyOrNull(text: String?): String? = ApiKeys.geminiKeyOrNull(text)

    private fun mask(key: String) = "${key.take(4)}…${key.takeLast(4)}"

    private companion object {
        const val MIN_KEY_LENGTH = 20
    }
}
