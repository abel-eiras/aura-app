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

    private companion object {
        const val KEY_QUALITY = "audio_quality"
        const val KEY_REQUESTED_AUDIO = "requested_record_audio"
        const val KEY_BATTERY_PROMPT = "battery_prompt_shown"
    }
}
