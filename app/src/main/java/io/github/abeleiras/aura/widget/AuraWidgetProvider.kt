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
 * Home-screen widget: the Aura logo on a square that changes with the state (lime idle, purple
 * recording, grey paused). Tapping toggles recording like the tile (FR-001-03). The service calls
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
        private const val LOGO_ON_LIME = 0xFF0D0D0D.toInt()
        private const val LOGO_ON_PURPLE = 0xFFFFFFFF.toInt()

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AuraWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val status = context.container.recordingStateHolder.status.value
            val background = when (status) {
                RecordingStatus.Idle -> R.drawable.widget_bg_idle
                is RecordingStatus.Recording -> R.drawable.widget_bg_recording
                is RecordingStatus.Paused -> R.drawable.widget_bg_paused
            }
            val color = if (status is RecordingStatus.Recording) LOGO_ON_PURPLE else LOGO_ON_LIME
            val toggle = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, AuraWidgetProvider::class.java).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val views = RemoteViews(context.packageName, R.layout.widget_aura).apply {
                setInt(R.id.widget_root, "setBackgroundResource", background)
                setInt(R.id.widget_image, "setColorFilter", color)
                setOnClickPendingIntent(R.id.widget_image, toggle)
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
