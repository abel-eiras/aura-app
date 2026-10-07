package io.github.abeleiras.aura.crash

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import io.github.abeleiras.aura.domain.crash.CrashReportFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Shows the user what went wrong when the app dies (spec 007). Everything stays on the phone:
 * nothing is sent anywhere unless the person copies or shares the report (Constitution IV).
 */
object CrashReport {
    /** Main process only: the `:crash` process must not install it, or a failure there would loop (FR-007-02). */
    fun install(app: Application) {
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val report = CrashReportFormat.build(
                    timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT).format(Date()),
                    appVersion = runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull() ?: "?",
                    device = "${Build.MANUFACTURER} ${Build.MODEL}",
                    androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    thread = thread.name,
                    stackTrace = Log.getStackTraceString(error),
                )
                app.startActivity(
                    Intent(app, CrashActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(CrashActivity.EXTRA_REPORT, report),
                )
            } catch (_: Throwable) {
                // Nothing left to do: fall through and die.
            }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }
}
