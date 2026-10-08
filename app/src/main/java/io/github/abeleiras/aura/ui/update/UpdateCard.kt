package io.github.abeleiras.aura.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.ui.components.AuraTextButton
import io.github.abeleiras.aura.ui.components.AuraAttentionCard
import io.github.abeleiras.aura.data.update.ApkInstaller
import io.github.abeleiras.aura.data.update.UpdateUiState
import io.github.abeleiras.aura.domain.update.AvailableUpdate
import io.github.abeleiras.aura.domain.update.UpdateFailure

/**
 * Shows where an update is: offered, downloading, waiting for the install permission, installing,
 * blocked by a recording, or failed. Idle, checking and up-to-date only show on the Settings
 * page ([showQuietStates]) so the main screen stays empty when there is nothing to say.
 */
@Composable
fun UpdateCard(
    state: UpdateUiState,
    onUpdate: () -> Unit,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showQuietStates: Boolean = false,
) {
    val context = LocalContext.current
    var showNotes by remember { mutableStateOf<AvailableUpdate?>(null) }

    val quiet = state is UpdateUiState.Idle || state is UpdateUiState.Checking || state is UpdateUiState.UpToDate
    if (!quiet || (showQuietStates && state !is UpdateUiState.Idle)) {
        AuraAttentionCard(modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                when (state) {
                    UpdateUiState.Idle -> Unit
                    UpdateUiState.Checking -> Message(R.string.update_checking)
                    UpdateUiState.UpToDate -> Message(R.string.update_up_to_date)
                    is UpdateUiState.Available -> {
                        Message(R.string.update_available, state.update.version.toString())
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            if (onDismiss != null) AuraTextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
                            AuraTextButton(onClick = { showNotes = state.update }) { Text(stringResource(R.string.update_whats_new)) }
                            AuraTextButton(onClick = onUpdate) { Text(stringResource(R.string.update_install)) }
                        }
                    }
                    is UpdateUiState.Downloading -> {
                        Message(R.string.update_downloading, state.update.version.toString())
                        if (state.totalBytes > 0) {
                            LinearProgressIndicator(
                                progress = { (state.bytesRead.toFloat() / state.totalBytes).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        }
                    }
                    is UpdateUiState.NeedsInstallPermission -> {
                        Message(R.string.update_needs_permission)
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            AuraTextButton(onClick = { context.startActivity(ApkInstaller.permissionSettingsIntent(context)) }) {
                                Text(stringResource(R.string.update_open_settings))
                            }
                        }
                    }
                    is UpdateUiState.Installing -> Message(R.string.update_installing)
                    is UpdateUiState.BlockedByRecording -> {
                        Message(R.string.update_blocked_by_recording)
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            AuraTextButton(onClick = onUpdate) { Text(stringResource(R.string.update_try_again)) }
                        }
                    }
                    is UpdateUiState.Failed -> {
                        Message(failureText(state.failure))
                        if (state.update != null) {
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                AuraTextButton(onClick = onUpdate) { Text(stringResource(R.string.update_try_again)) }
                            }
                        }
                    }
                }
            }
        }
    }

    showNotes?.let { update ->
        AlertDialog(
            onDismissRequest = { showNotes = null },
            title = { Text(stringResource(R.string.update_notes_title, update.version.toString())) },
            text = {
                Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text(update.notes.ifBlank { stringResource(R.string.update_notes_empty) }, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                AuraTextButton(onClick = {
                    showNotes = null
                    onUpdate()
                }) { Text(stringResource(R.string.update_install)) }
            },
            dismissButton = { AuraTextButton(onClick = { showNotes = null }) { Text(stringResource(R.string.update_close)) } },
        )
    }
}

@Composable
private fun Message(resId: Int, vararg args: Any) {
    Text(stringResource(resId, *args), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Message(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun failureText(failure: UpdateFailure): String = stringResource(
    when (failure) {
        UpdateFailure.NETWORK -> R.string.update_error_network
        UpdateFailure.RATE_LIMITED -> R.string.update_error_rate_limited
        UpdateFailure.BAD_RESPONSE -> R.string.update_error_bad_response
        UpdateFailure.HASH_MISMATCH -> R.string.update_error_invalid_download
        UpdateFailure.SIGNATURE_MISMATCH -> R.string.update_error_signature
        UpdateFailure.NO_SPACE -> R.string.update_error_no_space
        UpdateFailure.INSTALL_FAILED -> R.string.update_error_install
    },
)
