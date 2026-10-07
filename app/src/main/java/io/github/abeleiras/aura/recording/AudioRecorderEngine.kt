package io.github.abeleiras.aura.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import io.github.abeleiras.aura.domain.recording.AudioQuality
import io.github.abeleiras.aura.domain.recording.RecordingNaming
import java.io.File

/**
 * `MediaRecorder` wrapper: mono Opus in an OGG container (FR-001-04; API 29 is the minSdk).
 * Pause/resume keep writing to the same file (FR-001-05).
 */
class AudioRecorderEngine(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun start(outputDir: File, quality: AudioQuality, startedAtMillis: Long): File {
        val file = File(outputDir, RecordingNaming.fileName(startedAtMillis))
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.OGG)
                setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                setAudioChannels(AudioQuality.CHANNELS)
                setAudioSamplingRate(AudioQuality.SAMPLE_RATE)
                setAudioEncodingBitRate(quality.bitRate)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            runCatching { mediaRecorder.release() }
            file.delete()
            throw e
        }
        recorder = mediaRecorder
        currentFile = file
        return file
    }

    /** Best effort: if a device can't pause OGG/Opus, recording simply continues. */
    fun pause(): Boolean = runCatching { recorder?.pause() }
        .onFailure { Log.w(TAG, "pause() failed", it) }
        .isSuccess

    fun resume(): Boolean = runCatching { recorder?.resume() }
        .onFailure { Log.w(TAG, "resume() failed", it) }
        .isSuccess

    /** The finished file, or null if the recorder produced nothing usable (stopped almost at once). */
    fun stop(): File? {
        val file = currentFile
        val mediaRecorder = recorder
        recorder = null
        currentFile = null
        val startedStopping = SystemClock.elapsedRealtime()
        return try {
            mediaRecorder?.stop()
            Log.i(TAG, "MediaRecorder.stop() took ${SystemClock.elapsedRealtime() - startedStopping} ms")
            file
        } catch (e: RuntimeException) {
            // stop() throws when called right after start() with nothing captured.
            Log.w(TAG, "stop() failed; nothing to keep", e)
            file?.delete()
            null
        } finally {
            runCatching { mediaRecorder?.release() }
        }
    }

    private companion object {
        const val TAG = "AudioRecorderEngine"
    }
}
