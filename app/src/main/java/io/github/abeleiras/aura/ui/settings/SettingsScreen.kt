package io.github.abeleiras.aura.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import io.github.abeleiras.aura.data.prefs.AppSettings
import io.github.abeleiras.aura.domain.recording.AudioQuality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    updates: UpdateController,
    versionName: String,
    onBackClick: () -> Unit,
) {
    var quality by remember { mutableStateOf(settings.audioQuality) }
    var autoCheck by remember { mutableStateOf(settings.autoUpdateCheck) }
    var preReleases by remember { mutableStateOf(settings.includePreReleases) }
    val updateState by updates.state.collectAsStateWithLifecycle()

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
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(stringResource(R.string.settings_audio_quality_section), style = MaterialTheme.typography.titleMedium)
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
