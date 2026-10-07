package io.github.abeleiras.aura

import android.content.Context
import android.util.Log
import androidx.room.Room
import io.github.abeleiras.aura.data.RecordingRepository
import io.github.abeleiras.aura.data.db.AuraDatabase
import io.github.abeleiras.aura.data.export.ExportRepository
import io.github.abeleiras.aura.data.update.UpdateController
import io.github.abeleiras.aura.data.prefs.AppSettings
import io.github.abeleiras.aura.recording.PlaybackController
import io.github.abeleiras.aura.domain.recording.RecordingStatus
import io.github.abeleiras.aura.domain.update.ApkDownloader
import io.github.abeleiras.aura.domain.update.ReleasesClient
import io.github.abeleiras.aura.recording.RecordingStateHolder
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/** Hand-wired singletons (ADR-0008). Reached from anywhere via [AuraApplication.container]. */
class AppContainer(private val context: Context) {
    val appContext: Context get() = context.applicationContext

    /** Outlives any Activity or Service; used for work that must finish even if they die (FR-001-07). */
    val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            // A failing background task is logged, never fatal (FR-007-05).
            CoroutineExceptionHandler { _, error -> Log.e("AuraApp", "Background task failed", error) },
    )

    val settings = AppSettings(context)
    val recordingStateHolder = RecordingStateHolder()
    val playback = PlaybackController()

    private val database by lazy {
        Room.databaseBuilder(context, AuraDatabase::class.java, "aura.db").build()
    }

    val exports by lazy { ExportRepository(appContext, settings, recordings) }

    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val updates by lazy {
        val releases = ReleasesClient(http)
        UpdateController(
            context = appContext,
            scope = appScope,
            settings = settings,
            releases = releases,
            downloader = ApkDownloader(http, releases),
            isRecording = { recordingStateHolder.status.value !is RecordingStatus.Idle },
        )
    }

    val recordings by lazy {
        RecordingRepository(File(context.filesDir, "recordings"), database.recordingDao())
    }
}

val Context.container: AppContainer
    get() = (applicationContext as AuraApplication).container
