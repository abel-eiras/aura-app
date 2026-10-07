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
import io.github.abeleiras.aura.domain.ai.ProviderReadiness
import io.github.abeleiras.aura.domain.export.ExportStatus
import io.github.abeleiras.aura.domain.processing.ErrorReason
import io.github.abeleiras.aura.domain.processing.JobRecord
import io.github.abeleiras.aura.domain.processing.JobStatus
import io.github.abeleiras.aura.domain.recording.RecordingPolicy
import io.github.abeleiras.aura.recording.PlaybackState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(viewModel: RecordingsViewModel, onOpenNote: (String) -> Unit, onSetupProcessing: () -> Unit, onBackClick: () -> Unit) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val readiness by viewModel.readiness.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<RecordingItem?>(null) }
    var detailsFor by remember { mutableStateOf<RecordingItem?>(null) }

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
                readiness = readiness,
                onProcessAll = viewModel::processAllPending,
                onSetupProcessing = onSetupProcessing,
                onProcess = viewModel::process,
                onOpenNote = onOpenNote,
                onShowDetails = { detailsFor = it },
                onDelete = { pendingDelete = it },
                modifier = Modifier.padding(padding),
            )
        }
    }

    detailsFor?.let { item -> ErrorDetailsDialog(item, onDismiss = { detailsFor = null }) }

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
    readiness: ProviderReadiness,
    onProcessAll: () -> Unit,
    onSetupProcessing: () -> Unit,
    onProcess: (Recording) -> Unit,
    onOpenNote: (String) -> Unit,
    onShowDetails: (RecordingItem) -> Unit,
    onDelete: (RecordingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val totalBytes = items.sumOf { it.recording.sizeBytes }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            ProcessingBanner(
                readiness = readiness,
                pending = items.count { it.job == null || it.job.status == JobStatus.ERROR },
                onProcessAll = onProcessAll,
                onSetup = onSetupProcessing,
            )
        }
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
                onProcess = { if (item.canProcess) onProcess(item.recording) else onSetupProcessing() },
                onOpenNote = { onOpenNote(item.recording.fileName) },
                onShowDetails = { onShowDetails(item) },
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
    onProcess: () -> Unit,
    onOpenNote: () -> Unit,
    onShowDetails: () -> Unit,
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
                processingLabel(item.job)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.job?.status == JobStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            val status = item.job?.status
            if (status == JobStatus.ERROR || item.job?.error != null) TextButton(onClick = onShowDetails) { Text(stringResource(R.string.processing_details)) }
            when {
                status == JobStatus.READY -> TextButton(onClick = onOpenNote) { Text(stringResource(R.string.processing_action_open)) }
                status == null || status == JobStatus.ERROR ->
                    TextButton(onClick = onProcess) {
                        Text(stringResource(if (status == JobStatus.ERROR) R.string.processing_action_retry else R.string.processing_action_process))
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

/** Says where processing stands, and what to do about it, above the list (nothing silent). */
@Composable
private fun ProcessingBanner(readiness: ProviderReadiness, pending: Int, onProcessAll: () -> Unit, onSetup: () -> Unit) {
    val text = when (readiness) {
        ProviderReadiness.NO_PROVIDER -> R.string.banner_no_provider
        ProviderReadiness.NO_KEY -> R.string.banner_no_key
        ProviderReadiness.KEY_INVALID -> R.string.banner_key_invalid
        ProviderReadiness.NEEDS_PRIVACY -> R.string.banner_needs_privacy
        ProviderReadiness.READY -> if (pending > 0) R.string.banner_ready_pending else R.string.banner_ready
    }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                if (readiness == ProviderReadiness.READY && pending > 0) stringResource(text, pending) else stringResource(text),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (readiness != ProviderReadiness.READY) {
                TextButton(onClick = onSetup) { Text(stringResource(R.string.banner_setup)) }
            } else if (pending > 0) {
                TextButton(onClick = onProcessAll) { Text(stringResource(R.string.banner_process_all)) }
            }
        }
    }
}

@Composable
internal fun processingLabel(job: JobRecord?): String? {
    job ?: return null
    return when (job.status) {
        JobStatus.QUEUED -> stringResource(
            when {
                job.attempts == 0 -> R.string.processing_queued
                job.error == ErrorReason.PROVIDER_UNAVAILABLE || job.error == ErrorReason.RATE_LIMITED -> R.string.processing_retrying_busy
                else -> R.string.processing_retrying
            },
        )
        JobStatus.WAITING_NETWORK -> stringResource(if (job.attempts > 0) R.string.processing_retrying_network else R.string.processing_waiting_network)
        JobStatus.TRANSCRIBING -> stringResource(R.string.processing_transcribing)
        JobStatus.DRAFTING -> stringResource(R.string.processing_drafting)
        JobStatus.READY -> stringResource(R.string.processing_ready)
        JobStatus.NO_CONTENT -> stringResource(R.string.processing_no_content)
        JobStatus.ERROR -> stringResource(
            when (job.error) {
                ErrorReason.NETWORK -> R.string.processing_error_network
                ErrorReason.RATE_LIMITED -> R.string.processing_error_rate_limited
                ErrorReason.PROVIDER_UNAVAILABLE -> R.string.processing_error_unavailable
                ErrorReason.INVALID_CREDENTIAL -> R.string.processing_error_credential
                ErrorReason.QUOTA_EXHAUSTED -> R.string.processing_error_quota
                ErrorReason.INSUFFICIENT_FUNDS -> R.string.processing_error_funds
                ErrorReason.AUDIO_TOO_LONG -> R.string.processing_error_too_long
                ErrorReason.INVALID_RESPONSE -> R.string.processing_error_invalid_response
                ErrorReason.MODEL_UNAVAILABLE -> R.string.processing_error_model
                ErrorReason.UNKNOWN, null -> R.string.processing_error_unknown
            },
        )
    }
}

@Composable
private fun exportLabel(status: ExportStatus): String? = when (status) {
    ExportStatus.NONE -> null
    ExportStatus.PENDING -> stringResource(R.string.recordings_export_pending)
    ExportStatus.ERROR -> stringResource(R.string.recordings_export_failed)
    ExportStatus.EXPORTED -> stringResource(R.string.recordings_export_exported)
}

/** The technical detail of a failed processing, copyable, so a report can say exactly what happened. */
@Composable
private fun ErrorDetailsDialog(item: RecordingItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val job = item.job
    val text = buildString {
        appendLine("file: ${item.recording.fileName}")
        appendLine("size: ${item.recording.sizeBytes} bytes, duration: ${item.recording.durationMillis / 1000} s")
        appendLine("status: ${job?.status}")
        appendLine("reason: ${job?.error}")
        appendLine("attempts: ${job?.attempts}")
        appendLine("at: ${job?.updatedAt}")
        append("detail: ${job?.message ?: "-"}")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.processing_details)) },
        text = { androidx.compose.foundation.text.selection.SelectionContainer { Text(text, style = MaterialTheme.typography.bodySmall) } },
        confirmButton = {
            TextButton(onClick = {
                context.getSystemService(android.content.ClipboardManager::class.java)
                    ?.setPrimaryClip(android.content.ClipData.newPlainText("Aura", text))
            }) { Text(stringResource(R.string.processing_details_copy)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.crash_close)) } },
    )
}
