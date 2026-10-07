package io.github.abeleiras.aura.data.prefs

import android.content.Context
import androidx.core.content.edit
import io.github.abeleiras.aura.domain.ai.CredentialState
import io.github.abeleiras.aura.domain.ai.ProcessingMode
import io.github.abeleiras.aura.domain.ai.ProviderDefaults
import io.github.abeleiras.aura.domain.recording.AudioQuality

/** Non-sensitive preferences. Credentials live in [io.github.abeleiras.aura.data.ai.CredentialStore] (FR-002-05). */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var audioQuality: AudioQuality
        get() = AudioQuality.fromNameOrDefault(prefs.getString(KEY_QUALITY, null))
        set(value) = prefs.edit { putString(KEY_QUALITY, value.name) }

    var hasRequestedRecordAudioBefore: Boolean
        get() = prefs.getBoolean(KEY_REQUESTED_AUDIO, false)
        set(value) = prefs.edit { putBoolean(KEY_REQUESTED_AUDIO, value) }

    /** The battery-optimization explanation is shown once (spec 001, edge cases). */
    var batteryPromptShown: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_PROMPT, false)
        set(value) = prefs.edit { putBoolean(KEY_BATTERY_PROMPT, value) }

    /** Daily silent check against GitHub Releases; the only request Aura makes on its own (FR-006-04). */
    var autoUpdateCheck: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_UPDATE, value) }

    var includePreReleases: Boolean
        get() = prefs.getBoolean(KEY_PRE_RELEASES, false)
        set(value) = prefs.edit { putBoolean(KEY_PRE_RELEASES, value) }

    var lastUpdateCheckMillis: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_UPDATE_CHECK, value) }

    /** Version the user dismissed the banner for; shown again only for a newer one. */
    var dismissedUpdateVersion: String?
        get() = prefs.getString(KEY_DISMISSED_UPDATE, null)
        set(value) = prefs.edit { putString(KEY_DISMISSED_UPDATE, value) }

    /** `content://` tree URI of the export folder chosen with the system picker (spec 004), or null. */
    var exportFolderUri: String?
        get() = prefs.getString(KEY_EXPORT_FOLDER, null)
        set(value) = prefs.edit { if (value == null) remove(KEY_EXPORT_FOLDER) else putString(KEY_EXPORT_FOLDER, value) }

    /** Recordings that started before this are not exported (0 = export everything). */
    var exportFromMillis: Long
        get() = prefs.getLong(KEY_EXPORT_FROM, 0L)
        set(value) = prefs.edit { putLong(KEY_EXPORT_FROM, value) }

    /** File names already copied to the export folder. Deliberately not in Room: see specs/004 plan. */
    val exportedFiles: Set<String>
        get() = prefs.getStringSet(KEY_EXPORTED, null)?.toSet() ?: emptySet()

    /** Exported transcripts and notes: relative path -> hash of the content last written there. */
    val exportedDerived: Map<String, String>
        get() = (prefs.getStringSet(KEY_EXPORTED_DERIVED, null) ?: emptySet())
            .mapNotNull { entry -> entry.substringBefore('\t', "").takeIf { it.isNotEmpty() }?.let { it to entry.substringAfter('\t') } }
            .toMap()

    @Synchronized
    fun setExportedDerived(path: String, hash: String) {
        val others = (prefs.getStringSet(KEY_EXPORTED_DERIVED, null) ?: emptySet()).filterNot { it.startsWith("$path\t") }
        prefs.edit { putStringSet(KEY_EXPORTED_DERIVED, (others + "$path\t$hash").toSet()) }
    }

    @Synchronized
    fun addExported(fileName: String) = prefs.edit { putStringSet(KEY_EXPORTED, exportedFiles + fileName) }

    @Synchronized
    fun removeExported(fileName: String) = prefs.edit { putStringSet(KEY_EXPORTED, exportedFiles - fileName) }

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) }

    var processingMode: ProcessingMode
        get() = ProcessingMode.fromNameOrDefault(prefs.getString(KEY_MODE, null))
        set(value) = prefs.edit { putString(KEY_MODE, value.name) }

    var credentialState: CredentialState
        get() = CredentialState.fromNameOrDefault(prefs.getString(KEY_CREDENTIAL_STATE, null))
        set(value) = prefs.edit { putString(KEY_CREDENTIAL_STATE, value.name) }

    var credentialCheckedAtMillis: Long
        get() = prefs.getLong(KEY_CREDENTIAL_CHECKED, 0L)
        set(value) = prefs.edit { putLong(KEY_CREDENTIAL_CHECKED, value) }

    /** FR-002-08: the user accepted what Gemini receives. Without it nothing is sent. */
    var privacyAcceptedGemini: Boolean
        get() = prefs.getBoolean(KEY_PRIVACY_GEMINI, false)
        set(value) = prefs.edit { putBoolean(KEY_PRIVACY_GEMINI, value) }

    /** BCP-47 tag chosen by the user; null = follow the system language (FR-002-06). */
    var primaryLanguage: String?
        get() = prefs.getString(KEY_LANG_PRIMARY, null)
        set(value) = prefs.edit { if (value == null) remove(KEY_LANG_PRIMARY) else putString(KEY_LANG_PRIMARY, value) }

    var secondaryLanguage: String?
        get() = prefs.getString(KEY_LANG_SECONDARY, null)
        set(value) = prefs.edit { if (value == null) remove(KEY_LANG_SECONDARY) else putString(KEY_LANG_SECONDARY, value) }

    /** FR-003-12: process each recording when it is saved, if a provider is ready. */
    var autoProcess: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PROCESS, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_PROCESS, value) }

    /** FR-003-10: notification when a note is ready and the app is in the background. */
    var notifyWhenReady: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_READY, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFY_READY, value) }

    var transcribeModel: String
        get() = prefs.getString(KEY_MODEL_TRANSCRIBE, null) ?: ProviderDefaults.GEMINI_TRANSCRIBE_MODEL
        set(value) = prefs.edit { putString(KEY_MODEL_TRANSCRIBE, value) }

    var draftModel: String
        get() = prefs.getString(KEY_MODEL_DRAFT, null) ?: ProviderDefaults.GEMINI_DRAFT_MODEL
        set(value) = prefs.edit { putString(KEY_MODEL_DRAFT, value) }

    var processingReminderDismissed: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_DISMISSED, false)
        set(value) = prefs.edit { putBoolean(KEY_REMINDER_DISMISSED, value) }

    private companion object {
        const val KEY_REMINDER_DISMISSED = "processing_reminder_dismissed"
        const val KEY_AUTO_PROCESS = "auto_process"
        const val KEY_NOTIFY_READY = "notify_when_ready"
        const val KEY_MODEL_TRANSCRIBE = "model_transcribe"
        const val KEY_MODEL_DRAFT = "model_draft"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_MODE = "processing_mode"
        const val KEY_CREDENTIAL_STATE = "credential_state"
        const val KEY_CREDENTIAL_CHECKED = "credential_checked_at"
        const val KEY_PRIVACY_GEMINI = "privacy_accepted_gemini"
        const val KEY_LANG_PRIMARY = "language_primary"
        const val KEY_LANG_SECONDARY = "language_secondary"
        const val KEY_EXPORT_FOLDER = "export_folder_uri"
        const val KEY_EXPORT_FROM = "export_from_millis"
        const val KEY_EXPORTED = "exported_files"
        const val KEY_EXPORTED_DERIVED = "exported_derived"

        const val KEY_AUTO_UPDATE = "auto_update_check"
        const val KEY_PRE_RELEASES = "include_pre_releases"
        const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        const val KEY_DISMISSED_UPDATE = "dismissed_update_version"

        const val KEY_QUALITY = "audio_quality"
        const val KEY_REQUESTED_AUDIO = "requested_record_audio"
        const val KEY_BATTERY_PROMPT = "battery_prompt_shown"
    }
}
