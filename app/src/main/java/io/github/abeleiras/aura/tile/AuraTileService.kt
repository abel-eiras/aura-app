package io.github.abeleiras.aura.tile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.container
import io.github.abeleiras.aura.domain.recording.RecordingStatus
import io.github.abeleiras.aura.recording.RecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Quick Settings tile: tap to start or stop, shows idle / recording / paused (FR-001-03). */
class AuraTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collectJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        collectJob = scope.launch {
            container.recordingStateHolder.status.collect(::render)
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        collectJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onClick() {
        super.onClick()
        val status = container.recordingStateHolder.status.value
        try {
            if (status is RecordingStatus.Idle) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.tile_permission_missing, Toast.LENGTH_LONG).show()
                    return
                }
                RecordingService.start(applicationContext)
            } else {
                RecordingService.stop(applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not toggle recording from the tile", e)
            Toast.makeText(this, R.string.tile_action_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun render(status: RecordingStatus) {
        val tile = qsTile ?: return
        val (state, label, subtitle) = when (status) {
            RecordingStatus.Idle -> Triple(Tile.STATE_INACTIVE, R.string.tile_label_idle, R.string.tile_subtitle_idle)
            is RecordingStatus.Recording -> Triple(Tile.STATE_ACTIVE, R.string.tile_label_recording, R.string.tile_subtitle_recording)
            is RecordingStatus.Paused -> Triple(Tile.STATE_ACTIVE, R.string.tile_label_paused, R.string.tile_subtitle_paused)
        }
        tile.state = state
        tile.label = getString(label)
        tile.subtitle = getString(subtitle)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_aura_mono)
        tile.updateTile()
    }

    private companion object {
        const val TAG = "AuraTileService"
    }
}
