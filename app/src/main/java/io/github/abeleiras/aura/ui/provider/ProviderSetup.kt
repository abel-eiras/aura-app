package io.github.abeleiras.aura.ui.provider

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.data.ai.ProviderController
import io.github.abeleiras.aura.data.ai.ProviderState
import io.github.abeleiras.aura.domain.ai.CredentialCheck
import io.github.abeleiras.aura.domain.ai.CredentialState
import io.github.abeleiras.aura.domain.ai.ProcessingMode
import io.github.abeleiras.aura.domain.ai.ProviderDefaults

/**
 * Mode choice plus the step for the chosen provider (HU-002-1/2/4). Used both in the first-run wizard and in
 * Settings → Processing, so there is a single implementation of the setup.
 */
@Composable
fun ProviderSetup(controller: ProviderController, modifier: Modifier = Modifier) {
    val state by controller.state.collectAsStateWithLifecycle()
    var confirmDisconnect by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.provider_mode_question), style = MaterialTheme.typography.titleMedium)
        Column(modifier = Modifier.selectableGroup()) {
            ModeRow(ProcessingMode.GEMINI, state.mode, R.string.provider_mode_gemini, R.string.provider_mode_gemini_desc, true, controller)
            ModeRow(ProcessingMode.OPENROUTER, state.mode, R.string.provider_mode_openrouter, R.string.provider_mode_openrouter_desc, false, controller)
            ModeRow(ProcessingMode.NONE, state.mode, R.string.provider_mode_none, R.string.provider_mode_none_desc, true, controller)
        }
        if (state.mode == ProcessingMode.GEMINI) {
            GeminiStep(controller, state)
            if (state.hasKey) {
                OutlinedButton(onClick = { confirmDisconnect = true }) { Text(stringResource(R.string.provider_disconnect)) }
            }
        }
    }

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text(stringResource(R.string.provider_disconnect_title)) },
            text = { Text(stringResource(R.string.provider_disconnect_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDisconnect = false
                    controller.disconnect()
                }) { Text(stringResource(R.string.provider_disconnect)) }
            },
            dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text(stringResource(R.string.provider_cancel)) } },
        )
    }
}

@Composable
private fun ModeRow(
    mode: ProcessingMode,
    selected: ProcessingMode,
    title: Int,
    description: Int,
    enabled: Boolean,
    controller: ProviderController,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected == mode, enabled = enabled, onClick = { controller.selectMode(mode) }, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected == mode, onClick = null, enabled = enabled)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.7f else 0.4f),
            )
        }
    }
}

@Composable
private fun GeminiStep(controller: ProviderController, state: ProviderState) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var clipboardKey by remember { mutableStateOf<String?>(null) }

    // The user comes back from the browser having copied the key: offer to paste it (FR-002-03).
    LifecycleResumeEffect(Unit) {
        clipboardKey = controller.clipboardKeyOrNull(context.readClipboardText())
        onPauseOrDispose { }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("1. " + stringResource(R.string.gemini_step_1))
        Text("2. " + stringResource(R.string.gemini_step_2))
        Text("3. " + stringResource(R.string.gemini_step_3))
        Button(onClick = { context.openUrl(ProviderDefaults.GEMINI_KEYS_URL) }) { Text(stringResource(R.string.gemini_open_studio)) }

        if (state.hasKey) {
            Text(stringResource(R.string.gemini_key_saved, state.keyHint.orEmpty()), style = MaterialTheme.typography.bodyMedium)
        }
        clipboardKey?.takeIf { it != input }?.let { found ->
            OutlinedButton(onClick = {
                input = found
                clipboardKey = null
            }) { Text(stringResource(R.string.gemini_paste_detected)) }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(stringResource(R.string.gemini_key_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = input.isNotBlank() && !state.checking,
                onClick = {
                    if (controller.saveAndCheck(input)) input = ""
                },
            ) { Text(stringResource(if (state.checking) R.string.gemini_checking else R.string.gemini_check)) }
            if (state.hasKey && input.isBlank()) {
                OutlinedButton(enabled = !state.checking, onClick = controller::check) { Text(stringResource(R.string.gemini_check_again)) }
            }
        }
        if (state.saveFailed) Text(stringResource(R.string.gemini_save_failed), color = MaterialTheme.colorScheme.error)
        CredentialStatus(state)
        if (state.hasKey && state.credential == CredentialState.VALID) PrivacyNotice(controller, state)
    }
}

@Composable
private fun CredentialStatus(state: ProviderState) {
    val last = state.lastCheck
    val message = when (last) {
        is CredentialCheck.Valid -> stringResource(R.string.gemini_result_valid)
        CredentialCheck.InvalidKey -> stringResource(R.string.gemini_result_invalid)
        CredentialCheck.RegionNotSupported -> stringResource(R.string.gemini_result_region)
        CredentialCheck.RateLimited -> stringResource(R.string.gemini_result_rate_limited)
        CredentialCheck.Network -> stringResource(R.string.gemini_result_network)
        is CredentialCheck.Unavailable -> stringResource(R.string.gemini_result_unavailable, last.httpCode)
        null -> when {
            !state.hasKey -> null
            state.credential == CredentialState.VALID ->
                stringResource(R.string.gemini_status_valid, DateUtils.getRelativeTimeSpanString(state.checkedAtMillis).toString())
            state.credential == CredentialState.INVALID -> stringResource(R.string.gemini_status_invalid)
            else -> stringResource(R.string.gemini_status_unchecked)
        }
    }
    val isError = last != null && last !is CredentialCheck.Valid
    if (message != null) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

/** FR-002-08: explicit, provider-specific consent before anything is sent. */
@Composable
private fun PrivacyNotice(controller: ProviderController, state: ProviderState) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.privacy_gemini_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.privacy_gemini_body), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { context.openUrl(ProviderDefaults.GEMINI_TERMS_URL) }) { Text(stringResource(R.string.privacy_policy_link)) }
            Row(
                modifier = Modifier.fillMaxWidth().selectable(
                    selected = state.privacyAccepted,
                    onClick = { if (!state.privacyAccepted) controller.acceptPrivacy() },
                    role = Role.Checkbox,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = state.privacyAccepted, onCheckedChange = null)
                Text(stringResource(R.string.privacy_accept), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

private fun Context.openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun Context.readClipboardText(): String? = try {
    getSystemService(ClipboardManager::class.java)?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
} catch (_: SecurityException) {
    null
}
