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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.abeleiras.aura.ui.main.MainScreen
import io.github.abeleiras.aura.ui.main.MainViewModel
import io.github.abeleiras.aura.ui.notes.NoteScreen
import io.github.abeleiras.aura.ui.notes.NoteViewModel
import io.github.abeleiras.aura.ui.onboarding.OnboardingScreen
import io.github.abeleiras.aura.ui.permissions.BatteryOptimizationDialog
import io.github.abeleiras.aura.ui.permissions.PermissionDeniedDialog
import io.github.abeleiras.aura.ui.permissions.PermissionRationaleDialog
import io.github.abeleiras.aura.ui.provider.ProviderScreen
import io.github.abeleiras.aura.ui.recordings.RecordingsScreen
import io.github.abeleiras.aura.ui.recordings.RecordingsViewModel
import io.github.abeleiras.aura.ui.settings.SettingsScreen
import io.github.abeleiras.aura.ui.theme.AuraTheme

class MainActivity : ComponentActivity() {
    /** File name of the recording whose note a notification asked to open; consumed by the UI once. */
    private val openNote = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openNote.value = intent?.getStringExtra(EXTRA_OPEN_NOTE)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        setContent {
            AuraTheme {
                AuraApp(versionName, openNote.value, onOpenNoteHandled = { openNote.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_NOTE)?.let { openNote.value = it }
    }

    override fun onStart() {
        super.onStart()
        container.processing.uiVisible = true
    }

    override fun onStop() {
        container.processing.uiVisible = false
        super.onStop()
    }

    companion object {
        const val EXTRA_OPEN_NOTE = "open_note"
    }
}

private object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val PROVIDER = "provider"
    const val NOTE = "note/{file}"
    fun note(file: String) = "note/${Uri.encode(file)}"
    const val SETTINGS = "settings"
    const val RECORDINGS = "recordings"
}

private enum class Prompt { None, Rationale, PermanentlyDenied, Battery }

@Composable
private fun AuraApp(versionName: String, openNote: String?, onOpenNoteHandled: () -> Unit) {
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
    // A "note ready" notification was tapped (HU-003-1, scenario 4).
    LaunchedEffect(openNote) {
        if (openNote != null) {
            navController.navigate(Routes.note(openNote))
            onOpenNoteHandled()
        }
    }
    // Back from the "install unknown apps" settings page: carry on with the update.
    LifecycleResumeEffect(Unit) {
        container.updates.onResumed()
        // Back in the foreground: retry exports that were waiting for the folder (spec 004, scenario 4).
        container.appScope.launch { container.exports.exportPending() }
        onPauseOrDispose { }
    }

    NavHost(
        navController = navController,
        startDestination = if (container.settings.onboardingDone) Routes.MAIN else Routes.ONBOARDING,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                settings = container.settings,
                provider = container.provider,
                onFinished = {
                    navController.navigate(Routes.MAIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                },
            )
        }
        composable(Routes.PROVIDER) {
            ProviderScreen(controller = container.provider, onBackClick = { navController.popBackStack() })
        }
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
            RecordingsScreen(
                viewModel = recordingsViewModel,
                onOpenNote = { navController.navigate(Routes.note(it)) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Routes.NOTE, arguments = listOf(navArgument("file") { type = NavType.StringType })) { entry ->
            val file = entry.arguments?.getString("file").orEmpty()
            val noteViewModel: NoteViewModel = viewModel(
                key = file,
                factory = viewModelFactory { initializer { NoteViewModel(container, file) } },
            )
            NoteScreen(viewModel = noteViewModel, onBackClick = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(settings = container.settings, updates = container.updates, exports = container.exports, recordings = container.recordings, versionName = versionName, provider = container.provider, onProcessingClick = { navController.navigate(Routes.PROVIDER) }, onBackClick = { navController.popBackStack() })
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
