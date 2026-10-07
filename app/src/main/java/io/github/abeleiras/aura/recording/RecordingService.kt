package io.github.abeleiras.aura.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.StatFs
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.abeleiras.aura.MainActivity
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.container
import io.github.abeleiras.aura.domain.recording.AudioQuality
import io.github.abeleiras.aura.domain.recording.RecordingPolicy
import io.github.abeleiras.aura.domain.recording.RecordingStatus
import io.github.abeleiras.aura.widget.AuraWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Foreground service (microphone type) that owns the recorder, so recording survives the
 * app going to the background or the screen turning off (FR-001-01). While it runs there is
 * always a fixed notification (FR-001-02, Constitution V).
 */
class RecordingService : Service() {

    private val holder get() = container.recordingStateHolder
    private lateinit var engine: AudioRecorderEngine
    private lateinit var callMonitor: CallStateMonitor
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var storageWatch: Job? = null
    private var pausedForCall = false
    private var stopping = false
    private var quality = AudioQuality.DEFAULT
    private var startedAtMillis = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        engine = AudioRecorderEngine(this)
        callMonitor = CallStateMonitor(this)
        createNotificationChannels()
    }

    override fun onDestroy() {
        super.onDestroy()
        callMonitor.stop()
        serviceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_START -> startRecording()
                ACTION_PAUSE -> pauseRecording(byUser = true)
                ACTION_RESUME -> resumeRecording()
                ACTION_STOP -> stopRecording(lowStorage = false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle ${intent?.action}", e)
            holder.stop()
            AuraWidgetProvider.refresh(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        // startForegroundService() requires startForeground() promptly, before anything can fail.
        startForeground(paused = false)
        if (holder.status.value.isActive) return
        val now = System.currentTimeMillis()
        quality = container.settings.audioQuality
        startedAtMillis = now
        stopping = false
        engine.start(container.recordings.directory(), quality, now)
        holder.start(now)
        callMonitor.start(onCallActive = ::onCallActive, onCallEnded = ::onCallEnded)
        watchStorage()
        AuraWidgetProvider.refresh(this)
    }

    private fun pauseRecording(byUser: Boolean) {
        if (holder.status.value !is RecordingStatus.Recording) return
        engine.pause()
        holder.pause()
        if (byUser) pausedForCall = false
        notify(NOTIFICATION_ID, buildNotification(paused = true))
        AuraWidgetProvider.refresh(this)
    }

    private fun resumeRecording() {
        if (holder.status.value !is RecordingStatus.Paused) return
        pausedForCall = false
        engine.resume()
        holder.resume()
        notify(NOTIFICATION_ID, buildNotification(paused = false))
        AuraWidgetProvider.refresh(this)
    }

    /** Auto-pause for a call, distinct from a user pause so only that one auto-resumes (FR-001-06). */
    private fun onCallActive() {
        if (holder.status.value is RecordingStatus.Recording) {
            pauseRecording(byUser = false)
            pausedForCall = true
        }
    }

    private fun onCallEnded() {
        if (pausedForCall) resumeRecording()
    }

    private fun watchStorage() {
        storageWatch?.cancel()
        storageWatch = serviceScope.launch {
            while (isActive) {
                delay(STORAGE_CHECK_MILLIS)
                val free = StatFs(container.recordings.directory().path).availableBytes
                if (RecordingPolicy.isStorageLow(free)) {
                    stopRecording(lowStorage = true)
                    return@launch
                }
            }
        }
    }

    /**
     * Closes the file and registers it in the database BEFORE the state goes back to idle,
     * so nothing reacts to a recording that isn't persisted yet (FR-001-07, Constitution VI).
     */
    private fun stopRecording(lowStorage: Boolean) {
        val status = holder.status.value
        if (stopping || !status.isActive) {
            finish()
            return
        }
        stopping = true
        callMonitor.stop()
        storageWatch?.cancel()
        pausedForCall = false

        val durationMillis = status.elapsedMillis(System.currentTimeMillis())
        val file: File? = engine.stop()
        if (lowStorage) notify(ALERT_NOTIFICATION_ID, buildLowStorageNotification())

        when {
            file == null -> finish()
            RecordingPolicy.isTooShort(durationMillis) -> {
                file.delete() // accidental tap (FR-001-11)
                finish()
            }
            else -> container.appScope.launch {
                try {
                    container.recordings.register(file, startedAtMillis, durationMillis, quality)
                    container.processing.onRecordingRegistered(file.name)
                } catch (e: Exception) {
                    // The audio is on disk; recoverOrphans() will list it on the next start.
                    Log.e(TAG, "Could not register ${file.name}", e)
                }
                withContext(Dispatchers.Main.immediate) { finish() }
                // After the service is released: copying to the export folder must not delay stopping.
                container.exports.exportPending()
            }
        }
    }

    private fun finish() {
        holder.stop()
        AuraWidgetProvider.refresh(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForeground(paused: Boolean) {
        startForeground(NOTIFICATION_ID, buildNotification(paused), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
    }

    private fun notify(id: Int, notification: Notification) {
        getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private fun buildNotification(paused: Boolean): Notification {
        val toggle = if (paused) {
            NotificationCompat.Action(R.drawable.ic_aura_mono, getString(R.string.notification_action_resume), serviceIntent(ACTION_RESUME))
        } else {
            NotificationCompat.Action(R.drawable.ic_aura_mono, getString(R.string.notification_action_pause), serviceIntent(ACTION_PAUSE))
        }
        val stop = NotificationCompat.Action(R.drawable.ic_aura_mono, getString(R.string.notification_action_stop), serviceIntent(ACTION_STOP))
        return NotificationCompat.Builder(this, CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_aura_mono)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(if (paused) R.string.notification_text_paused else R.string.notification_text_recording))
            .setContentIntent(openAppIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(toggle)
            .addAction(stop)
            .build()
    }

    private fun buildLowStorageNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_aura_mono)
            .setContentTitle(getString(R.string.notification_low_storage_title))
            .setContentText(getString(R.string.notification_low_storage_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_low_storage_text)))
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun serviceIntent(action: String): PendingIntent = PendingIntent.getService(
        this, action.hashCode(), Intent(this, RecordingService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_RECORDING, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW)
                .apply { description = getString(R.string.notification_channel_description) },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, getString(R.string.notification_alerts_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = getString(R.string.notification_alerts_channel_description) },
        )
    }

    companion object {
        const val ACTION_START = "io.github.abeleiras.aura.action.START_RECORDING"
        const val ACTION_PAUSE = "io.github.abeleiras.aura.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "io.github.abeleiras.aura.action.RESUME_RECORDING"
        const val ACTION_STOP = "io.github.abeleiras.aura.action.STOP_RECORDING"

        private const val CHANNEL_RECORDING = "aura_recording"
        private const val CHANNEL_ALERTS = "aura_alerts"
        private const val NOTIFICATION_ID = 1001
        private const val ALERT_NOTIFICATION_ID = 1002
        private const val STORAGE_CHECK_MILLIS = 10_000L
        private const val TAG = "RecordingService"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, RecordingService::class.java).setAction(ACTION_START))
        }

        /** Pause, resume and stop target a running service; they never start a new foreground service. */
        fun pause(context: Context) = send(context, ACTION_PAUSE)
        fun resume(context: Context) = send(context, ACTION_RESUME)
        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String) {
            runCatching { context.startService(Intent(context, RecordingService::class.java).setAction(action)) }
                .onFailure { Log.w(TAG, "Could not send $action", it) }
        }
    }
}
