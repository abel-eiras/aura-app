package io.github.abeleiras.aura.recording

import io.github.abeleiras.aura.domain.recording.RecordingStatus
import io.github.abeleiras.aura.domain.recording.RecordingTransitions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide source of truth for the recording state, shared by the service, the tile,
 * the widget and the UI so they never disagree about whether recording is active.
 */
class RecordingStateHolder {
    private val _status = MutableStateFlow<RecordingStatus>(RecordingStatus.Idle)
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()

    fun start(nowMillis: Long = System.currentTimeMillis()) = _status.update { RecordingTransitions.start(it, nowMillis) }
    fun pause(nowMillis: Long = System.currentTimeMillis()) = _status.update { RecordingTransitions.pause(it, nowMillis) }
    fun resume(nowMillis: Long = System.currentTimeMillis()) = _status.update { RecordingTransitions.resume(it, nowMillis) }
    fun stop() = _status.update { RecordingTransitions.stop(it) }

    private inline fun MutableStateFlow<RecordingStatus>.update(transform: (RecordingStatus) -> RecordingStatus) {
        value = transform(value)
    }
}
