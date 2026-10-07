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

    @Synchronized
    fun addExported(fileName: String) = prefs.edit { putStringSet(KEY_EXPORTED, exportedFiles + fileName) }

    @Synchronized
    fun removeExported(fileName: String) = prefs.edit { putStringSet(KEY_EXPORTED, exportedFiles - fileName) }

    private companion object {
        const val KEY_EXPORT_FOLDER = "export_folder_uri"
        const val KEY_EXPORT_FROM = "export_from_millis"
        const val KEY_EXPORTED = "exported_files"

        const val KEY_AUTO_UPDATE = "auto_update_check"
        const val KEY_PRE_RELEASES = "include_pre_releases"
        const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        const val KEY_DISMISSED_UPDATE = "dismissed_update_version"

        const val KEY_QUALITY = "audio_quality"
        const val KEY_REQUESTED_AUDIO = "requested_record_audio"
        const val KEY_BATTERY_PROMPT = "battery_prompt_shown"
    }
}
