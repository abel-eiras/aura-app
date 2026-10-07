package io.github.abeleiras.aura.recording

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val fileName: String? = null,
    val isPlaying: Boolean = false,
    val positionMillis: Int = 0,
    val durationMillis: Int = 0,
)

/** Plays one recording at a time with pause and seek (HU-001-4). Main-thread only. */
class PlaybackController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var ticker: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /** Starts [file], resumes it if paused, or pauses it if playing. */
    fun toggle(file: File) {
        val current = _state.value
        val mediaPlayer = player
        if (current.fileName == file.name && mediaPlayer != null) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                _state.value = current.copy(isPlaying = false, positionMillis = mediaPlayer.currentPosition)
            } else {
                mediaPlayer.start()
                _state.value = current.copy(isPlaying = true)
                startTicker()
            }
            return
        }
        play(file)
    }

    fun seekTo(positionMillis: Int) {
        player?.seekTo(positionMillis)
        _state.value = _state.value.copy(positionMillis = positionMillis)
    }

    fun stop() {
        ticker?.cancel()
        ticker = null
        player?.let { runCatching { it.release() } }
        player = null
        _state.value = PlaybackState()
    }

    private fun play(file: File) {
        stop()
        runCatching {
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
            player = mediaPlayer
            _state.value = PlaybackState(file.name, isPlaying = true, positionMillis = 0, durationMillis = mediaPlayer.duration)
            startTicker()
        }.onFailure { stop() }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                player?.let { _state.value = _state.value.copy(positionMillis = it.currentPosition) }
                delay(300)
            }
        }
    }
}
