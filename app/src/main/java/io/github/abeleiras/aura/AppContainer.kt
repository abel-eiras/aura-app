package io.github.abeleiras.aura

import android.content.Context
import androidx.room.Room
import io.github.abeleiras.aura.data.RecordingRepository
import io.github.abeleiras.aura.data.db.AuraDatabase
import io.github.abeleiras.aura.data.prefs.AppSettings
import io.github.abeleiras.aura.recording.PlaybackController
import io.github.abeleiras.aura.recording.RecordingStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/** Hand-wired singletons (ADR-0008). Reached from anywhere via [AuraApplication.container]. */
class AppContainer(private val context: Context) {
    val appContext: Context get() = context.applicationContext

    /** Outlives any Activity or Service; used for work that must finish even if they die (FR-001-07). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings = AppSettings(context)
    val recordingStateHolder = RecordingStateHolder()
    val playback = PlaybackController()

    private val database by lazy {
        Room.databaseBuilder(context, AuraDatabase::class.java, "aura.db").build()
    }

    val recordings by lazy {
        RecordingRepository(File(context.filesDir, "recordings"), database.recordingDao())
    }
}

val Context.container: AppContainer
    get() = (applicationContext as AuraApplication).container
