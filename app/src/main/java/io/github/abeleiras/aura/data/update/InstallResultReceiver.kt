package io.github.abeleiras.aura.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import io.github.abeleiras.aura.container

/** Receives the installer's verdict. On success the system restarts the app, so only failures matter. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirm != null) context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            else -> {
                Log.w(TAG, "Install failed: status=$status ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
                context.container.updates.onInstallFailed()
            }
        }
    }

    private companion object {
        const val TAG = "InstallResultReceiver"
    }
}
