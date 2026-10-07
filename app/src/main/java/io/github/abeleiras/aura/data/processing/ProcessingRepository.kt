package io.github.abeleiras.aura.data.processing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.abeleiras.aura.MainActivity
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.container
import io.github.abeleiras.aura.data.RecordingRepository
import io.github.abeleiras.aura.data.ai.ProviderController
import io.github.abeleiras.aura.data.prefs.AppSettings
import io.github.abeleiras.aura.domain.ai.GeminiProvider
import io.github.abeleiras.aura.domain.notes.NoteType
import io.github.abeleiras.aura.domain.notes.NoteTypeCatalogs
import io.github.abeleiras.aura.domain.processing.ErrorReason
import io.github.abeleiras.aura.domain.processing.JobRecord
import io.github.abeleiras.aura.domain.processing.JobStatus
import io.github.abeleiras.aura.domain.processing.LanguageHints
import io.github.abeleiras.aura.domain.processing.ProcessingInput
import io.github.abeleiras.aura.domain.processing.ProcessingOutcome
import io.github.abeleiras.aura.domain.processing.ProcessingPipeline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.time.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Queue and runner of the processing of recordings (spec 003). WorkManager keeps the work alive when the user
 * leaves the app and waits for a connection (FR-003-06); the pipeline persists each step, so a restarted
 * worker continues where the last one stopped (FR-003-01).
 */
class ProcessingRepository(
    private val context: Context,
    private val store: FileProcessingStore,
    private val recordings: RecordingRepository,
    private val provider: ProviderController,
    private val settings: AppSettings,
    private val http: OkHttpClient,
) {
    private val _jobs = MutableStateFlow(store.loadAllJobs())
    val jobs: StateFlow<Map<String, JobRecord>> = _jobs.asStateFlow()

    /** Set by the activity so notifications are only shown when the app is in the background (FR-003-10). */
    @Volatile
    var uiVisible: Boolean = false

    init {
        store.onChange = { _jobs.value = store.loadAllJobs() }
    }

    fun store(): FileProcessingStore = store

    /** True when the user has a provider ready and accepted its privacy notice (FR-003-13). */
    val canProcess: Boolean get() = provider.state.value.canSend

    /** Called when a recording was registered: processes it if the user wants that (FR-003-12). */
    fun onRecordingRegistered(fileName: String) {
        if (settings.autoProcess && canProcess) enqueue(fileName)
    }

    /** Queues one recording; a failed one starts over with a fresh attempt count. */
    fun enqueue(fileName: String) {
        if (!canProcess) return
        val id = idOf(fileName)
        val existing = store.loadJob(id)
        if (existing?.status == JobStatus.READY || existing?.status == JobStatus.NO_CONTENT) return
        if (existing == null || existing.status == JobStatus.ERROR) {
            store.saveJob(id, JobRecord(JobStatus.QUEUED, updatedAt = Instant.now().toString()))
        }
        val request = OneTimeWorkRequestBuilder<ProcessingWorker>()
            .setInputData(workDataOf(ProcessingWorker.KEY_FILE to fileName))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("process-$id", ExistingWorkPolicy.KEEP, request)
    }

    /** Recording deleted by the user: stop its work and forget everything derived from it. */
    fun forget(fileName: String) {
        val id = idOf(fileName)
        WorkManager.getInstance(context).cancelUniqueWork("process-$id")
        store.delete(id)
    }

    /** Runs the pipeline for one recording; returns what the worker should answer. */
    internal suspend fun process(fileName: String): ProcessingOutcome? = runLock.withLock {
        val id = idOf(fileName)
        val recording = recordings.all().firstOrNull { it.fileName == fileName } ?: return@withLock null // deleted meanwhile
        val key = provider.keyForSending() ?: return@withLock null // mode changed or consent revoked: send nothing
        val language = Locale.getDefault().language
        val pipeline = ProcessingPipeline(
            provider = GeminiProvider(
                http = http,
                apiKey = key,
                transcribeModel = settings.transcribeModel,
                draftModel = settings.draftModel,
                pipelineVersion = "aura-app ${versionName()}",
            ),
            types = NoteTypeCatalogs.defaults(language).types,
            store = store,
            hints = LanguageHints(settings.primaryLanguage ?: language, settings.secondaryLanguage),
            draftModel = settings.draftModel,
        )
        val outcome = try {
            pipeline.run(ProcessingInput(id, recording.file, recording.durationMillis / 1000.0))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A bug or a disk problem, not a provider failure: show an error instead of leaving "Transcribing…" forever.
            Log.e("Processing", "Unexpected failure for $fileName", e)
            store.saveJob(id, JobRecord(JobStatus.ERROR, error = ErrorReason.UNKNOWN, message = e.javaClass.simpleName, updatedAt = Instant.now().toString()))
            ProcessingOutcome.Finished(JobStatus.ERROR, ErrorReason.UNKNOWN)
        }
        if (outcome is ProcessingOutcome.Finished && outcome.status == JobStatus.READY) notifyReady(recording.fileName, recording.startedAtMillis, id)
        outcome
    }

    private fun notifyReady(fileName: String, startedAt: Long, id: String) {
        if (uiVisible || !settings.notifyWhenReady) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.processing_channel_name), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val note = store.loadNote(id)
        val type = note?.let { typeById(it.typeId) }
        val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(startedAt))
        val title = context.getString(R.string.processing_notification_ready, "${type?.label ?: ""} — $date".trim(' ', '—'))
        val open = PendingIntent.getActivity(
            context,
            id.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_NOTE, fileName)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            id.hashCode(),
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_aura_mono)
                .setContentTitle(title)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    /** Looks the type up in every language's built-ins: a note keeps its type id whatever language the UI uses now. */
    fun typeById(id: String): NoteType? =
        (listOf(Locale.getDefault().language) + listOf("es", "en", "gl")).firstNotNullOfOrNull { lang ->
            NoteTypeCatalogs.defaults(lang).types.firstOrNull { it.id == id }
        }

    private fun versionName(): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

    companion object {
        private const val CHANNEL = "processing"
        private val runLock = Mutex() // one recording at a time, in order of arrival (FR-003-06)

        fun idOf(fileName: String): String = fileName.substringBeforeLast('.')
    }
}

class ProcessingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val fileName = inputData.getString(KEY_FILE) ?: return Result.failure()
        return when (val outcome = applicationContext.container.processing.process(fileName)) {
            is ProcessingOutcome.RetryLater -> Result.retry()
            is ProcessingOutcome.Finished, null -> Result.success()
        }
    }

    companion object {
        const val KEY_FILE = "file"
    }
}
