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
import io.github.abeleiras.aura.data.Recording
import io.github.abeleiras.aura.data.RecordingRepository
import io.github.abeleiras.aura.data.export.ExportFile
import io.github.abeleiras.aura.data.ai.ProviderController
import io.github.abeleiras.aura.data.prefs.AppSettings
import io.github.abeleiras.aura.domain.ai.GeminiProvider
import io.github.abeleiras.aura.domain.notes.NoteRenderer
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

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
    /** Runs after a note was drafted so transcript and note reach the export folder (spec 004). */
    private val afterDrafted: suspend () -> Unit = {},
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
        schedule(fileName, typeId = null, ExistingWorkPolicy.KEEP)
    }

    /** Queues every recording that has no result yet or failed; returns how many. */
    suspend fun enqueuePending(): Int {
        if (!canProcess) return 0
        val jobs = store.loadAllJobs()
        val pending = recordings.all().filter { jobs[idOf(it.fileName)].let { job -> job == null || job.status == JobStatus.ERROR } }
        pending.forEach { enqueue(it.fileName) }
        return pending.size
    }

    /**
     * HU-003-4. [typeId] drafts with that note type instead of classifying; [transcribeAgain] also discards the
     * transcript (the caller has shown the cost warning). Whatever stays is reused, so the cheap options really
     * are cheap (FR-003-11).
     */
    fun regenerate(fileName: String, typeId: String? = null, transcribeAgain: Boolean = false) {
        if (!canProcess) return
        val id = idOf(fileName)
        if (transcribeAgain) store.deleteTranscript(id)
        store.deleteNote(id)
        store.saveJob(id, JobRecord(JobStatus.QUEUED, updatedAt = Instant.now().toString()))
        schedule(fileName, typeId, ExistingWorkPolicy.REPLACE)
    }

    private fun schedule(fileName: String, typeId: String?, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<ProcessingWorker>()
            .setInputData(workDataOf(ProcessingWorker.KEY_FILE to fileName, ProcessingWorker.KEY_TYPE to typeId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("process-${idOf(fileName)}", policy, request)
    }

    /** Recording deleted by the user: stop its work and forget everything derived from it. */
    fun forget(fileName: String) {
        val id = idOf(fileName)
        WorkManager.getInstance(context).cancelUniqueWork("process-$id")
        store.delete(id)
    }

    /** Runs the pipeline for one recording; returns what the worker should answer. */
    internal suspend fun process(fileName: String, forcedTypeId: String? = null): ProcessingOutcome? = runLock.withLock {
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
            pipeline.run(
                ProcessingInput(id, recording.file, recording.durationMillis / 1000.0),
                forcedType = forcedTypeId?.let { typeById(it) },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A bug or a disk problem, not a provider failure: show an error instead of leaving "Transcribing…" forever.
            Log.e("Processing", "Unexpected failure for $fileName", e)
            store.saveJob(id, JobRecord(JobStatus.ERROR, error = ErrorReason.UNKNOWN, message = "${e.javaClass.simpleName}: ${e.message}", updatedAt = Instant.now().toString()))
            ProcessingOutcome.Finished(JobStatus.ERROR, ErrorReason.UNKNOWN)
        }
        if (outcome is ProcessingOutcome.Finished && outcome.error == ErrorReason.MODEL_UNAVAILABLE && provider.refreshModelsAfterUnavailable()) {
            // The model was retired: a different one was picked from what the key has, so run again with it.
            store.saveJob(id, JobRecord(JobStatus.QUEUED, updatedAt = Instant.now().toString()))
            return@withLock ProcessingOutcome.RetryLater(15.seconds)
        }
        if (outcome is ProcessingOutcome.Finished && outcome.status != JobStatus.ERROR) {
            if (outcome.status == JobStatus.READY) notifyReady(recording.fileName, recording.startedAtMillis, id)
            afterDrafted()
        }
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

    /** Transcript and note as files for the export folder, laid out like aura-transcribe's output (FR-004-08). */
    suspend fun exportFilesFor(recording: Recording): List<ExportFile> = withContext(Dispatchers.IO) {
        val id = idOf(recording.fileName)
        val transcript = store.loadTranscript(id) ?: return@withContext emptyList()
        val stem = id
        val files = mutableListOf(
            ExportFile(listOf("transcription"), "$stem.json", "application/json", transcript.toJson().toByteArray(Charsets.UTF_8)),
        )
        val stored = store.loadNote(id)
        val type = stored?.let { typeById(it.typeId) }
        if (stored != null && type != null) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(recording.startedAtMillis))
            val markdown = NoteRenderer.render(
                type, date, transcript, stored.body,
                generatedBy = "aura-app ${versionName()}",
                genericSpeaker = context.getString(R.string.speaker_generic),
            )
            val path = NoteRenderer.exportPaths(recording.fileName, type, date).note.split('/')
            files += ExportFile(path.dropLast(1), path.last(), "text/markdown", markdown.toByteArray(Charsets.UTF_8))
        }
        files
    }

    /** The built-in types in the UI language (the user's own types arrive with spec 005's editor). */
    fun availableTypes(): List<NoteType> = NoteTypeCatalogs.defaults(Locale.getDefault().language).types

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
        return when (val outcome = applicationContext.container.processing.process(fileName, inputData.getString(KEY_TYPE))) {
            is ProcessingOutcome.RetryLater -> Result.retry()
            is ProcessingOutcome.Finished, null -> Result.success()
        }
    }

    companion object {
        const val KEY_FILE = "file"
        const val KEY_TYPE = "type"
    }
}
