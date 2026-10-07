package io.github.abeleiras.aura.ui.recordings

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.data.Recording
import io.github.abeleiras.aura.domain.export.ExportStatus
import io.github.abeleiras.aura.domain.recording.RecordingPolicy
import io.github.abeleiras.aura.recording.PlaybackState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(viewModel: RecordingsViewModel, onBackClick: () -> Unit) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<RecordingItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recordings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                },
            )
        },
    ) { padding ->
        val list = items
        when {
            list == null -> Box(Modifier.fillMaxSize().padding(padding))
            list.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.recordings_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            else -> RecordingsList(
                items = list,
                playback = playback,
                onPlayToggle = viewModel::togglePlayback,
                onSeek = viewModel::seekTo,
                onRetryExport = viewModel::retryExport,
                onDelete = { pendingDelete = it },
                modifier = Modifier.padding(padding),
            )
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.recordings_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        if (item.exportStatus == ExportStatus.EXPORTED) {
                            R.string.recordings_delete_confirm_body_exported
                        } else {
                            R.string.recordings_delete_confirm_body
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(item.recording)
                    pendingDelete = null
                }) { Text(stringResource(R.string.recordings_delete)) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.recordings_cancel)) } },
        )
    }
}

@Composable
private fun RecordingsList(
    items: List<RecordingItem>,
    playback: PlaybackState,
    onPlayToggle: (Recording) -> Unit,
    onSeek: (Int) -> Unit,
    onRetryExport: () -> Unit,
    onDelete: (RecordingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val totalBytes = items.sumOf { it.recording.sizeBytes }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.recordings_total, items.size, Formatter.formatShortFileSize(context, totalBytes)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                if (RecordingPolicy.isArchiveLarge(totalBytes)) {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(
                            text = stringResource(R.string.recordings_large_archive_warning, Formatter.formatShortFileSize(context, totalBytes)),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
        items(items, key = { it.recording.fileName }) { item ->
            RecordingRow(
                item = item,
                playback = playback.takeIf { it.fileName == item.recording.fileName },
                onPlayToggle = { onPlayToggle(item.recording) },
                onSeek = onSeek,
                onRetryExport = onRetryExport,
                onDelete = { onDelete(item) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun RecordingRow(
    item: RecordingItem,
    playback: PlaybackState?,
    onPlayToggle: () -> Unit,
    onSeek: (Int) -> Unit,
    onRetryExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val recording = item.recording
    val isPlaying = playback?.isPlaying == true
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onPlayToggle) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.content_description_pause_playback else R.string.content_description_play_recording,
                    ),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(recording.startedAtMillis)),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "${RecordingPolicy.formatDuration(recording.durationMillis)} · ${Formatter.formatShortFileSize(context, recording.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                exportLabel(item.exportStatus)?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            if (item.exportStatus == ExportStatus.ERROR) {
                TextButton(onClick = onRetryExport) { Text(stringResource(R.string.recordings_export_retry)) }
            }
            IconButton(onClick = { shareRecording(context, recording) }) {
                Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.content_description_share_recording))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.content_description_delete_recording))
            }
        }
        if (playback != null && playback.durationMillis > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(RecordingPolicy.formatDuration(playback.positionMillis.toLong()), style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = playback.positionMillis.toFloat(),
                    onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..playback.durationMillis.toFloat(),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text(RecordingPolicy.formatDuration(playback.durationMillis.toLong()), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun exportLabel(status: ExportStatus): String? = when (status) {
    ExportStatus.NONE -> null
    ExportStatus.PENDING -> stringResource(R.string.recordings_export_pending)
    ExportStatus.ERROR -> stringResource(R.string.recordings_export_failed)
    ExportStatus.EXPORTED -> stringResource(R.string.recordings_export_exported)
}
