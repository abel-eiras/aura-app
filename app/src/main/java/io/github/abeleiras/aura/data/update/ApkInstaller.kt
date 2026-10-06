package io.github.abeleiras.aura.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File

/** Installs a verified APK with the system [PackageInstaller] (FR-006-07). */
object ApkInstaller {
    const val ACTION_INSTALL_RESULT = "io.github.abeleiras.aura.action.INSTALL_RESULT"

    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Settings page where the user lets Aura install apps; asked only on the first update. */
    fun permissionSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Later updates can go through without a confirmation tap where Android allows it.
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("aura.apk", 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            // Explicit target (required for a mutable PendingIntent); the installer fills in the status extras.
            val resultIntent = Intent(context, InstallResultReceiver::class.java).setAction(ACTION_INSTALL_RESULT)
            val pending = PendingIntent.getBroadcast(context, sessionId, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            session.commit(pending.intentSender)
        }
    }
}
