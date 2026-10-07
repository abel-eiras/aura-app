package io.github.abeleiras.aura.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.container
import io.github.abeleiras.aura.domain.recording.RecordingStatus
import io.github.abeleiras.aura.recording.RecordingService

/**
 * Home-screen widget: just the Aura logo, tinted by state (grey idle, violet recording, light
 * violet paused). Tapping toggles recording like the tile (FR-001-03). The service calls
 * [refresh] on every state change.
 */
class AuraWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            if (context.container.recordingStateHolder.status.value is RecordingStatus.Idle) {
                RecordingService.start(context.applicationContext)
            } else {
                RecordingService.stop(context.applicationContext)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context)
    }

    companion object {
        private const val ACTION_TOGGLE = "io.github.abeleiras.aura.action.WIDGET_TOGGLE"
        private const val COLOR_IDLE = 0xFF9AA0AC.toInt()
        private const val COLOR_RECORDING = 0xFF6E5BFF.toInt()
        private const val COLOR_PAUSED = 0xFF9C8CFF.toInt()

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AuraWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val color = when (context.container.recordingStateHolder.status.value) {
                RecordingStatus.Idle -> COLOR_IDLE
                is RecordingStatus.Recording -> COLOR_RECORDING
                is RecordingStatus.Paused -> COLOR_PAUSED
            }
            val toggle = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, AuraWidgetProvider::class.java).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val views = RemoteViews(context.packageName, R.layout.widget_aura).apply {
                setInt(R.id.widget_image, "setColorFilter", color)
                setOnClickPendingIntent(R.id.widget_image, toggle)
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
