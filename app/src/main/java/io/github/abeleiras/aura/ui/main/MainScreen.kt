package io.github.abeleiras.aura.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.domain.recording.RecordingPolicy
import io.github.abeleiras.aura.domain.recording.RecordingStatus
import io.github.abeleiras.aura.ui.components.AuraOrb
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onToggleRecording: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecordingsClick: () -> Unit,
) {
    val status by viewModel.status.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                IconButton(onClick = onRecordingsClick) {
                    Icon(Icons.Outlined.List, contentDescription = stringResource(R.string.content_description_recordings))
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.content_description_settings))
                }
            }
            MainContent(
                status = status,
                onToggleRecording = onToggleRecording,
                onTogglePause = viewModel::togglePause,
            )
        }
    }
}

/** Plain content: takes immutable state and callbacks only, so it can be previewed and tested. */
@Composable
private fun MainContent(
    status: RecordingStatus,
    onToggleRecording: () -> Unit,
    onTogglePause: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuraOrb(
            isRecording = status is RecordingStatus.Recording,
            modifier = Modifier.clickable(onClick = onToggleRecording),
        )
        Text(
            text = statusText(status),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 24.dp, start = 32.dp, end = 32.dp),
        )
        if (status.isActive) {
            val paused = status is RecordingStatus.Paused
            IconButton(onClick = onTogglePause, modifier = Modifier.padding(top = 16.dp)) {
                Icon(
                    imageVector = if (paused) Icons.Outlined.PlayCircle else Icons.Outlined.PauseCircle,
                    contentDescription = stringResource(
                        if (paused) R.string.content_description_resume else R.string.content_description_pause,
                    ),
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}

@Composable
private fun statusText(status: RecordingStatus): String {
    // Ticks once a second while recording; paused and idle states are static.
    var now by remember(status) { mutableLongStateOf(System.currentTimeMillis()) }
    if (status is RecordingStatus.Recording) {
        LaunchedEffect(status) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1000)
            }
        }
    }
    val elapsed = RecordingPolicy.formatDuration(status.elapsedMillis(now))
    return when (status) {
        RecordingStatus.Idle -> stringResource(R.string.status_idle_ready)
        is RecordingStatus.Recording -> stringResource(R.string.status_recording, elapsed)
        is RecordingStatus.Paused -> stringResource(R.string.status_paused, elapsed)
    }
}
