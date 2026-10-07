package io.github.abeleiras.aura.data.update

import android.content.Context
import android.os.StatFs
import android.util.Log
import io.github.abeleiras.aura.data.prefs.AppSettings
import io.github.abeleiras.aura.domain.update.ApkDownloader
import io.github.abeleiras.aura.domain.update.AvailableUpdate
import io.github.abeleiras.aura.domain.update.ReleasesClient
import io.github.abeleiras.aura.domain.update.UpdateChecker
import io.github.abeleiras.aura.domain.update.UpdateException
import io.github.abeleiras.aura.domain.update.UpdateFailure
import io.github.abeleiras.aura.domain.update.UpdatePolicy
import io.github.abeleiras.aura.domain.update.Version
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val update: AvailableUpdate) : UpdateUiState
    data class Downloading(val update: AvailableUpdate, val bytesRead: Long, val totalBytes: Long) : UpdateUiState

    /** Waiting for the user to let Aura install apps (asked once, with an explanation). */
    data class NeedsInstallPermission(val update: AvailableUpdate) : UpdateUiState
    data class Installing(val update: AvailableUpdate) : UpdateUiState

    /** A recording is in progress; installing would close the app (FR-006-08). */
    data class BlockedByRecording(val update: AvailableUpdate) : UpdateUiState
    data class Failed(val failure: UpdateFailure, val update: AvailableUpdate? = null) : UpdateUiState
}

/**
 * Check, download, verify, install (spec 006). Runs in the app scope so a download
 * survives leaving the screen. Never downloads without an explicit tap (no background
 * downloads on mobile data, spec 006 edge cases).
 */
class UpdateController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settings: AppSettings,
    private val releases: ReleasesClient,
    private val downloader: ApkDownloader,
    private val isRecording: () -> Boolean,
) {
    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var job: Job? = null
    private var verifiedApk: File? = null
    private val updatesDir get() = File(context.filesDir, "updates")

    val installedVersion: Version
        get() = Version.parseOrNull(context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty())
            ?: Version(0, 0, 0)

    /** Called when the app opens: a silent check if one is due (FR-006-04). */
    fun checkOnAppOpen() {
        updatesDir.deleteRecursively() // leftovers from an earlier update; the next one downloads again
        if (UpdatePolicy.shouldCheck(System.currentTimeMillis(), settings.lastUpdateCheckMillis, settings.autoUpdateCheck)) {
            check(silent = true)
        }
    }

    /** "Check for updates" in Settings: always asks, and shows the outcome. */
    fun checkNow() = check(silent = false)

    private fun check(silent: Boolean) {
        if (job?.isActive == true) return
        job = scope.launch {
            if (!silent) _state.value = UpdateUiState.Checking
            try {
                val found = UpdateChecker.findUpdate(releases.releases(), installedVersion, settings.includePreReleases)
                settings.lastUpdateCheckMillis = System.currentTimeMillis()
                // A silent check doesn't nag about a version the user already dismissed.
                val offered = found?.takeUnless { silent && it.version.toString() == settings.dismissedUpdateVersion }
                _state.value = offered?.let { UpdateUiState.Available(it) } ?: if (silent) UpdateUiState.Idle else UpdateUiState.UpToDate
            } catch (e: UpdateException) {
                // A silent check that fails says nothing (HU-006-2); a manual one explains.
                if (!silent) _state.value = UpdateUiState.Failed(e.failure)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Anything unexpected must not leave a manual check stuck on "Checking" (FR-007-06).
                Log.w("UpdateController", "Update check failed", e)
                if (!silent) _state.value = UpdateUiState.Failed(UpdateFailure.BAD_RESPONSE)
            }
        }
    }

    fun startUpdate() {
        val update = when (val s = _state.value) {
            is UpdateUiState.Available -> s.update
            is UpdateUiState.Failed -> s.update
            is UpdateUiState.BlockedByRecording -> s.update
            else -> null
        } ?: return
        if (job?.isActive == true) return
        if (!UpdatePolicy.canInstallNow(recordingActive = isRecording())) {
            _state.value = UpdateUiState.BlockedByRecording(update)
            return
        }
        job = scope.launch {
            try {
                updatesDir.mkdirs()
                if (StatFs(updatesDir.path).availableBytes < update.apkSize * 2) throw UpdateException(UpdateFailure.NO_SPACE)
                _state.value = UpdateUiState.Downloading(update, 0, update.apkSize)
                val apk = downloader.download(update, File(updatesDir, "aura-${update.version}.apk")) { read, total ->
                    _state.value = UpdateUiState.Downloading(update, read, total)
                }
                if (!ApkSignatureVerifier.matchesInstalled(context, apk)) {
                    apk.delete()
                    throw UpdateException(UpdateFailure.SIGNATURE_MISMATCH)
                }
                verifiedApk = apk
                proceedToInstall(update)
            } catch (e: UpdateException) {
                _state.value = UpdateUiState.Failed(e.failure, update)
            }
        }
    }

    /** Called when the app returns to the foreground, e.g. after the install-permission settings page. */
    fun onResumed() {
        val s = _state.value
        if (s is UpdateUiState.NeedsInstallPermission && ApkInstaller.canInstall(context)) proceedToInstall(s.update)
    }

    fun onInstallFailed() {
        val update = (_state.value as? UpdateUiState.Installing)?.update
        _state.value = UpdateUiState.Failed(UpdateFailure.INSTALL_FAILED, update)
    }

    fun dismissAvailable() {
        (_state.value as? UpdateUiState.Available)?.let { settings.dismissedUpdateVersion = it.update.version.toString() }
        _state.value = UpdateUiState.Idle
    }

    private fun proceedToInstall(update: AvailableUpdate) {
        val apk = verifiedApk ?: return
        if (!ApkInstaller.canInstall(context)) {
            _state.value = UpdateUiState.NeedsInstallPermission(update)
            return
        }
        _state.value = UpdateUiState.Installing(update)
        try {
            ApkInstaller.install(context, apk)
        } catch (e: Exception) {
            _state.value = UpdateUiState.Failed(UpdateFailure.INSTALL_FAILED, update)
        }
    }
}
