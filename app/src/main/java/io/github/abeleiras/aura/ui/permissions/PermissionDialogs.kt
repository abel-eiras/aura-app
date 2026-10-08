package io.github.abeleiras.aura.ui.permissions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.ui.components.AuraTextButton

/** Explains, in plain words, why each permission is needed, before the system dialog (FR-001-10). */
@Composable
fun PermissionRationaleDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    ExplanationDialog(
        title = R.string.permission_rationale_title,
        body = R.string.permission_rationale_body,
        confirm = R.string.permission_rationale_continue,
        onConfirm = onContinue,
        onDismiss = onDismiss,
    )
}

/** Shown when the microphone permission was denied for good and only system settings can grant it. */
@Composable
fun PermissionDeniedDialog(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    ExplanationDialog(
        title = R.string.permission_denied_title,
        body = R.string.permission_denied_body,
        confirm = R.string.permission_denied_open_settings,
        onConfirm = onOpenSettings,
        onDismiss = onDismiss,
    )
}

/** One-time explanation about vendor battery optimization killing long recordings. */
@Composable
fun BatteryOptimizationDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    ExplanationDialog(
        title = R.string.battery_optimization_title,
        body = R.string.battery_optimization_body,
        confirm = R.string.battery_optimization_continue,
        onConfirm = onContinue,
        onDismiss = onDismiss,
    )
}

@Composable
private fun ExplanationDialog(
    title: Int,
    body: Int,
    confirm: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(body)) },
        confirmButton = { AuraTextButton(onClick = onConfirm) { Text(stringResource(confirm)) } },
        dismissButton = { AuraTextButton(onClick = onDismiss) { Text(stringResource(R.string.permission_not_now)) } },
    )
}
