package io.github.abeleiras.aura.data.prefs

import android.content.Context
import androidx.core.content.edit
import io.github.abeleiras.aura.domain.recording.AudioQuality

/** Non-sensitive preferences. Credentials will live elsewhere (spec 002, FR-002-05). */
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

    private companion object {
        const val KEY_AUTO_UPDATE = "auto_update_check"
        const val KEY_PRE_RELEASES = "include_pre_releases"
        const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        const val KEY_DISMISSED_UPDATE = "dismissed_update_version"

        const val KEY_QUALITY = "audio_quality"
        const val KEY_REQUESTED_AUDIO = "requested_record_audio"
        const val KEY_BATTERY_PROMPT = "battery_prompt_shown"
    }
}
