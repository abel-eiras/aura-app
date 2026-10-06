package io.github.abeleiras.aura.ui.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.abeleiras.aura.AppContainer
import io.github.abeleiras.aura.data.Recording
import io.github.abeleiras.aura.recording.PlaybackState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordingsViewModel(private val container: AppContainer) : ViewModel() {
    /** Null until the first database read, so the screen doesn't flash "empty". */
    val recordings: StateFlow<List<Recording>?> = container.recordings.observeRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val playback: StateFlow<PlaybackState> = container.playback.state

    fun togglePlayback(recording: Recording) = container.playback.toggle(recording.file)

    fun seekTo(positionMillis: Int) = container.playback.seekTo(positionMillis)

    fun delete(recording: Recording) {
        if (container.playback.state.value.fileName == recording.fileName) container.playback.stop()
        viewModelScope.launch { container.recordings.delete(recording.fileName) }
    }

    override fun onCleared() {
        container.playback.stop()
    }
}
