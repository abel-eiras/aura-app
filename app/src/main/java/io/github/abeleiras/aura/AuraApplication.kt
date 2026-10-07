package io.github.abeleiras.aura

import android.app.Application
import kotlinx.coroutines.launch

class AuraApplication : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Audio left on disk by a crash or kill is added to the list, never lost (Constitution VI).
        container.appScope.launch { container.recordings.recoverOrphans() }
    }
}
