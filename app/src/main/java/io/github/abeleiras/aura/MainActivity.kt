package io.github.abeleiras.aura

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.abeleiras.aura.ui.main.MainScreen
import io.github.abeleiras.aura.ui.main.MainViewModel
import io.github.abeleiras.aura.ui.permissions.BatteryOptimizationDialog
import io.github.abeleiras.aura.ui.permissions.PermissionDeniedDialog
import io.github.abeleiras.aura.ui.permissions.PermissionRationaleDialog
import io.github.abeleiras.aura.ui.recordings.RecordingsScreen
import io.github.abeleiras.aura.ui.recordings.RecordingsViewModel
import io.github.abeleiras.aura.ui.settings.SettingsScreen
import io.github.abeleiras.aura.ui.theme.AuraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TEMPORARY, to prove the CI smoke test can fail. Reverted in the next commit.
        if (packageName.isNotEmpty()) throw IllegalStateException("smoke test proof: this build is meant to crash")
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        setContent {
            AuraTheme {
                AuraApp(versionName)
            }
        }
    }
}

private object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val RECORDINGS = "recordings"
}

private enum class Prompt { None, Rationale, PermanentlyDenied, Battery }

@Composable
private fun AuraApp(versionName: String) {
    val context = LocalContext.current
    val container = context.container
    val navController = rememberNavController()
    var prompt by remember { mutableStateOf(Prompt.None) }

    val mainViewModel: MainViewModel = viewModel(factory = viewModelFactory { initializer { MainViewModel(container) } })
    val recordingsViewModel: RecordingsViewModel = viewModel(factory = viewModelFactory { initializer { RecordingsViewModel(container) } })

    fun startRecording() {
        mainViewModel.toggleRecording()
        if (!container.settings.batteryPromptShown && !context.isIgnoringBatteryOptimizations()) {
            container.settings.batteryPromptShown = true
            prompt = Prompt.Battery
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        container.settings.hasRequestedRecordAudioBefore = true
        when {
            results[Manifest.permission.RECORD_AUDIO] == true -> startRecording() // the user tapped "record"
            context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == false ->
                prompt = Prompt.PermanentlyDenied
        }
    }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    // Permissions are asked when first needed: the first tap on "record" (FR-001-10).
    val onToggleRecording: () -> Unit = {
        val needsPermission = !container.recordingStateHolder.status.value.isActive &&
            !context.hasPermission(Manifest.permission.RECORD_AUDIO)
        when {
            !needsPermission && container.recordingStateHolder.status.value.isActive -> mainViewModel.toggleRecording()
            !needsPermission -> startRecording()
            container.settings.hasRequestedRecordAudioBefore &&
                context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == false ->
                prompt = Prompt.PermanentlyDenied
            else -> prompt = Prompt.Rationale
        }
    }

    when (prompt) {
        Prompt.None -> Unit
        Prompt.Rationale -> PermissionRationaleDialog(
            onContinue = {
                prompt = Prompt.None
                permissionLauncher.launch(requestedPermissions())
            },
            onDismiss = { prompt = Prompt.None },
        )
        Prompt.PermanentlyDenied -> PermissionDeniedDialog(
            onOpenSettings = {
                prompt = Prompt.None
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}")),
                )
            },
            onDismiss = { prompt = Prompt.None },
        )
        Prompt.Battery -> BatteryOptimizationDialog(
            onContinue = {
                prompt = Prompt.None
                batteryLauncher.launch(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:${context.packageName}")),
                )
            },
            onDismiss = { prompt = Prompt.None },
        )
    }

    LaunchedEffect(Unit) { container.updates.checkOnAppOpen() }
    // Back from the "install unknown apps" settings page: carry on with the update.
    LifecycleResumeEffect(Unit) {
        container.updates.onResumed()
        // Back in the foreground: retry exports that were waiting for the folder (spec 004, scenario 4).
        container.appScope.launch { container.exports.exportPending() }
        onPauseOrDispose { }
    }

    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                viewModel = mainViewModel,
                updates = container.updates,
                onToggleRecording = onToggleRecording,
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onRecordingsClick = { navController.navigate(Routes.RECORDINGS) },
            )
        }
        composable(Routes.RECORDINGS) {
            RecordingsScreen(viewModel = recordingsViewModel, onBackClick = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(settings = container.settings, updates = container.updates, exports = container.exports, recordings = container.recordings, versionName = versionName, onBackClick = { navController.popBackStack() })
        }
    }
}

/** Microphone (required), notifications (so the recording indicator shows) and phone state (optional, for call auto-pause). */
private fun requestedPermissions(): Array<String> = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    add(Manifest.permission.READ_PHONE_STATE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.isIgnoringBatteryOptimizations(): Boolean =
    getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) ?: true

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
