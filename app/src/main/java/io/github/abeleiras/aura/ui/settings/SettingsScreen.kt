package io.github.abeleiras.aura.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.data.update.UpdateController
import io.github.abeleiras.aura.ui.update.UpdateCard
import kotlinx.coroutines.launch
import io.github.abeleiras.aura.data.RecordingRepository
import io.github.abeleiras.aura.data.ai.ProviderController
import io.github.abeleiras.aura.domain.ai.ProcessingMode
import androidx.compose.material3.OutlinedButton
import io.github.abeleiras.aura.data.export.ExportRepository
import io.github.abeleiras.aura.data.prefs.AppSettings
import io.github.abeleiras.aura.domain.recording.AudioQuality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    updates: UpdateController,
    exports: ExportRepository,
    recordings: RecordingRepository,
    versionName: String,
    provider: ProviderController,
    onProcessingClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    var quality by remember { mutableStateOf(settings.audioQuality) }
    var autoCheck by remember { mutableStateOf(settings.autoUpdateCheck) }
    var preReleases by remember { mutableStateOf(settings.includePreReleases) }
    val providerState by provider.state.collectAsStateWithLifecycle()
    val updateState by updates.state.collectAsStateWithLifecycle()
    val exportState by exports.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var folderToConfirm by remember { mutableStateOf<Uri?>(null) }
    var existingCount by remember { mutableIntStateOf(0) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            scope.launch {
                val count = recordings.count()
                if (count > 0) {
                    // Scenario 5 of HU-004-5: ask whether the older recordings should go too.
                    existingCount = count
                    folderToConfirm = uri
                } else {
                    exports.setFolder(uri, includeExisting = true)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(stringResource(R.string.settings_processing_section), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    when (providerState.mode) {
                        ProcessingMode.GEMINI -> R.string.settings_processing_gemini
                        ProcessingMode.OPENROUTER -> R.string.settings_processing_openrouter
                        ProcessingMode.NONE -> R.string.settings_processing_none
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedButton(onClick = onProcessingClick, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.settings_processing_configure))
            }
            Text(
                stringResource(R.string.settings_audio_quality_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
            Column(modifier = Modifier.selectableGroup().padding(top = 8.dp)) {
                AudioQuality.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = quality == option,
                                onClick = {
                                    quality = option
                                    settings.audioQuality = option
                                },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = quality == option, onClick = null)
                        Text(
                            text = stringResource(
                                when (option) {
                                    AudioQuality.NORMAL -> R.string.settings_audio_quality_normal
                                    AudioQuality.HIGH -> R.string.settings_audio_quality_high
                                },
                            ),
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.settings_export_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = stringResource(R.string.settings_export_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = when {
                    !exportState.folderConfigured -> stringResource(R.string.settings_export_folder_none)
                    else -> stringResource(
                        R.string.settings_export_folder_current,
                        exportState.folderName ?: stringResource(R.string.settings_export_folder_unnamed),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (exportState.permissionLost) {
                Text(
                    text = stringResource(R.string.settings_export_permission_lost),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (exportState.running) {
                Text(stringResource(R.string.settings_export_running), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
            Row(modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = { folderPicker.launch(null) }) {
                    Text(stringResource(if (exportState.folderConfigured) R.string.settings_export_change else R.string.settings_export_choose))
                }
                if (exportState.folderConfigured) {
                    TextButton(onClick = { scope.launch { exports.clearFolder() } }) {
                        Text(stringResource(R.string.settings_export_stop))
                    }
                }
            }
            if (exportState.folderConfigured && !exportState.permissionLost) {
                Row {
                    TextButton(onClick = { scope.launch { exports.exportPending() } }) { Text(stringResource(R.string.settings_export_retry)) }
                    TextButton(onClick = { scope.launch { exports.exportAllNow() } }) { Text(stringResource(R.string.settings_export_all)) }
                }
            }
            Text(
                text = stringResource(R.string.settings_about_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            SwitchRow(R.string.settings_update_auto, autoCheck) {
                autoCheck = it
                settings.autoUpdateCheck = it
            }
            SwitchRow(R.string.settings_update_pre_releases, preReleases) {
                preReleases = it
                settings.includePreReleases = it
            }
            TextButton(onClick = updates::checkNow, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.settings_update_check_now))
            }
            UpdateCard(
                state = updateState,
                onUpdate = updates::startUpdate,
                onDismiss = null,
                showQuietStates = true,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    folderToConfirm?.let { uri ->
        ExistingRecordingsDialog(
            count = existingCount,
            onExportAll = {
                folderToConfirm = null
                scope.launch { exports.setFolder(uri, includeExisting = true) }
            },
            onOnlyNew = {
                folderToConfirm = null
                scope.launch { exports.setFolder(uri, includeExisting = false) }
            },
            onDismiss = { folderToConfirm = null },
        )
    }
}

@Composable
private fun SwitchRow(label: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(label), modifier = Modifier.weight(1f).padding(end = 16.dp), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ExistingRecordingsDialog(count: Int, onExportAll: () -> Unit, onOnlyNew: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_existing_title)) },
        text = { Text(stringResource(R.string.export_existing_body, count)) },
        confirmButton = { TextButton(onClick = onExportAll) { Text(stringResource(R.string.export_existing_yes)) } },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.recordings_cancel)) }
                TextButton(onClick = onOnlyNew) { Text(stringResource(R.string.export_existing_no)) }
            }
        },
    )
}
