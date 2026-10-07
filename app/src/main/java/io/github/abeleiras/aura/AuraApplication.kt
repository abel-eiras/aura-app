package io.github.abeleiras.aura

import android.app.Application
import io.github.abeleiras.aura.crash.CrashReport
import kotlinx.coroutines.launch

class AuraApplication : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // The error screen runs in the ":crash" process: it must not start background work or
        // install the crash handler (FR-007-02).
        if (getProcessName() != packageName) return
        CrashReport.install(this)
        // Audio left on disk by a crash or kill is added to the list, never lost (Constitution VI).
        container.appScope.launch {
            container.recordings.recoverOrphans()
            container.exports.exportPending() // retries whatever is still pending (spec 004, scenario 4)
        }
    }
}
