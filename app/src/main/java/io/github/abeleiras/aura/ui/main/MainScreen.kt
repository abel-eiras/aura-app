package io.github.abeleiras.aura.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.data.update.UpdateController
import io.github.abeleiras.aura.domain.recording.RecordingPolicy
import io.github.abeleiras.aura.domain.recording.RecordingStatus
import io.github.abeleiras.aura.ui.components.AuraAttentionCard
import io.github.abeleiras.aura.ui.components.AuraButton
import io.github.abeleiras.aura.ui.components.AuraLogo
import io.github.abeleiras.aura.ui.components.AuraOutlinedButton
import io.github.abeleiras.aura.ui.components.AuraTag
import io.github.abeleiras.aura.ui.components.AuraTextButton
import io.github.abeleiras.aura.ui.components.hardShadow
import io.github.abeleiras.aura.ui.theme.BorderWidth
import io.github.abeleiras.aura.ui.theme.aura
import io.github.abeleiras.aura.ui.update.UpdateCard
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    updates: UpdateController,
    onToggleRecording: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecordingsClick: () -> Unit,
    showProcessingReminder: Boolean,
    reminderIncomplete: Boolean,
    onSetupProcessing: () -> Unit,
    onDismissReminder: () -> Unit,
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val updateState by updates.state.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AuraLogo(size = 40.dp)
                Column {
                    Text(stringResource(R.string.app_name).uppercase(), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.brand_by), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SquareIconButton(Icons.Outlined.List, stringResource(R.string.content_description_recordings), onRecordingsClick)
                SquareIconButton(Icons.Outlined.Settings, stringResource(R.string.content_description_settings), onSettingsClick)
            }
            UpdateCard(
                state = updateState,
                onUpdate = updates::startUpdate,
                onDismiss = updates::dismissAvailable,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 84.dp, start = 16.dp, end = 16.dp),
            )
            MainContent(
                status = status,
                onToggleRecording = onToggleRecording,
                onTogglePause = viewModel::togglePause,
                onStop = viewModel::stopRecording,
            )
            // A discreet offer to finish the setup that was skipped (HU-002-1, scenario 4).
            if (showProcessingReminder && status is RecordingStatus.Idle) {
                AuraAttentionCard(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)) {
                        Text(
                            text = stringResource(if (reminderIncomplete) R.string.reminder_processing_incomplete else R.string.reminder_processing),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        AuraTextButton(onClick = onSetupProcessing) { Text(stringResource(R.string.reminder_processing_action)) }
                        if (!reminderIncomplete) AuraTextButton(onClick = onDismissReminder) { Text(stringResource(R.string.reminder_processing_dismiss)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SquareIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(BorderWidth, MaterialTheme.colorScheme.outline))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

/** Plain content: takes immutable state and callbacks only, so it can be previewed and tested. */
@Composable
private fun MainContent(
    status: RecordingStatus,
    onToggleRecording: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
) {
    val elapsed = elapsedText(status)
    val recording = status is RecordingStatus.Recording
    val paused = status is RecordingStatus.Paused
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The state is always written out, not only coloured (Constitution V).
        AuraTag(
            text = stringResource(
                when {
                    recording -> R.string.main_tag_recording
                    paused -> R.string.main_tag_paused
                    else -> R.string.main_tag_idle
                },
            ),
            container = when {
                recording -> MaterialTheme.aura.recording
                paused -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.aura.attention
            },
            content = when {
                recording -> MaterialTheme.aura.onRecording
                paused -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.aura.onAttention
            },
        )
        Text(
            text = elapsed,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(Modifier.height(28.dp))
        val buttonColor = if (status.isActive) MaterialTheme.aura.recording else MaterialTheme.aura.attention
        val iconColor = if (status.isActive) MaterialTheme.aura.onRecording else MaterialTheme.aura.onAttention
        val description = stringResource(if (status.isActive) R.string.content_description_stop else R.string.content_description_record)
        Box(
            modifier = Modifier
                .size(200.dp)
                .hardShadow(8.dp)
                .background(buttonColor)
                .border(BorderStroke(3.dp, MaterialTheme.colorScheme.outline))
                .clickable(role = Role.Button, onClick = onToggleRecording)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(72.dp)) {
                if (status.isActive) {
                    drawRect(iconColor, style = Fill)
                } else {
                    drawCircle(iconColor, style = Fill)
                }
            }
        }
        Text(
            text = stringResource(if (status.isActive) R.string.main_hint_active else R.string.status_idle_ready),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 36.dp, start = 32.dp, end = 32.dp),
        )
        if (status.isActive) {
            Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AuraOutlinedButton(onClick = onTogglePause) {
                    Text(stringResource(if (paused) R.string.main_resume else R.string.main_pause))
                }
                AuraButton(onClick = onStop) { Text(stringResource(R.string.main_stop)) }
            }
        }
    }
}

@Composable
private fun elapsedText(status: RecordingStatus): String {
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
    return RecordingPolicy.formatDuration(status.elapsedMillis(now))
}
