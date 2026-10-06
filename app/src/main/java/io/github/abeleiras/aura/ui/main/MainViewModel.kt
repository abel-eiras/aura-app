package io.github.abeleiras.aura.ui.main

import androidx.lifecycle.ViewModel
import io.github.abeleiras.aura.AppContainer
import io.github.abeleiras.aura.domain.recording.RecordingStatus
import io.github.abeleiras.aura.recording.RecordingService
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(private val container: AppContainer) : ViewModel() {
    val status: StateFlow<RecordingStatus> = container.recordingStateHolder.status

    /** Start when idle, stop otherwise. Permissions are checked by the caller. */
    fun toggleRecording() {
        if (status.value is RecordingStatus.Idle) {
            RecordingService.start(container.appContext)
        } else {
            RecordingService.stop(container.appContext)
        }
    }

    fun togglePause() {
        when (status.value) {
            is RecordingStatus.Recording -> RecordingService.pause(container.appContext)
            is RecordingStatus.Paused -> RecordingService.resume(container.appContext)
            RecordingStatus.Idle -> Unit
        }
    }
}
