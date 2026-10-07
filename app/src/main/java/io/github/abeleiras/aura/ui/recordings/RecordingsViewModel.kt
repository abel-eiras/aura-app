package io.github.abeleiras.aura.ui.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.abeleiras.aura.AppContainer
import io.github.abeleiras.aura.data.Recording
import io.github.abeleiras.aura.data.processing.ProcessingRepository
import io.github.abeleiras.aura.domain.processing.JobRecord
import io.github.abeleiras.aura.domain.export.ExportStatus
import io.github.abeleiras.aura.recording.PlaybackState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecordingItem(
    val recording: Recording,
    val exportStatus: ExportStatus,
    val job: JobRecord?,
    /** A provider is ready, so "Process" makes sense (FR-003-13). */
    val canProcess: Boolean,
)

class RecordingsViewModel(private val container: AppContainer) : ViewModel() {
    /** Null until the first database read, so the screen doesn't flash "empty". */
    val items: StateFlow<List<RecordingItem>?> =
        combine(
            container.recordings.observeRecordings(),
            container.exports.state,
            container.processing.jobs,
            container.provider.state,
        ) { recordings, export, jobs, provider ->
            recordings.map {
                RecordingItem(it, export.statusOf(it), jobs[ProcessingRepository.idOf(it.fileName)], provider.canSend)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val playback: StateFlow<PlaybackState> = container.playback.state

    fun togglePlayback(recording: Recording) = container.playback.toggle(recording.file)

    fun seekTo(positionMillis: Int) = container.playback.seekTo(positionMillis)

    /** Deleting never touches the exported copy: it belongs to the user (FR-004-10). */
    fun delete(recording: Recording) {
        if (container.playback.state.value.fileName == recording.fileName) container.playback.stop()
        container.exports.forget(recording.fileName)
        container.processing.forget(recording.fileName)
        viewModelScope.launch { container.recordings.delete(recording.fileName) }
    }

    fun process(recording: Recording) = container.processing.enqueue(recording.fileName)

    /** Runs in the app scope so leaving the screen doesn't cancel a copy in progress. */
    fun retryExport() {
        container.appScope.launch { container.exports.exportPending() }
    }

    override fun onCleared() {
        container.playback.stop()
    }
}
