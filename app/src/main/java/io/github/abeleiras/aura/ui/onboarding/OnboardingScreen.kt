package io.github.abeleiras.aura.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.data.ai.ProviderController
import io.github.abeleiras.aura.data.prefs.AppSettings
import io.github.abeleiras.aura.ui.provider.ProviderSetup
import java.util.Locale

/** Languages offered as a hint for transcription (FR-002-06). Names are shown in their own language. */
private val LANGUAGE_TAGS = listOf("es", "gl", "en", "ca", "eu", "pt", "fr", "de", "it")

/**
 * First-run wizard (FR-002-01): welcome → processing mode and provider → languages. Every step can be skipped;
 * skipping leaves the app in "Record only".
 */
@Composable
fun OnboardingScreen(settings: AppSettings, provider: ProviderController, onFinished: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    fun finish() {
        settings.onboardingDone = true
        onFinished()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding().padding(24.dp)) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (step) {
                    0 -> Welcome()
                    1 -> ProviderSetup(provider)
                    else -> Languages(settings)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = ::finish) { Text(stringResource(R.string.onboarding_skip)) }
                Button(onClick = { if (step < 2) step++ else finish() }) {
                    Text(
                        stringResource(
                            when (step) {
                                0 -> R.string.onboarding_start
                                1 -> R.string.onboarding_next
                                else -> R.string.onboarding_finish
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Welcome() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 48.dp)) {
        Text(stringResource(R.string.onboarding_welcome_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onboarding_welcome_body), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Languages(settings: AppSettings) {
    val system = Locale.getDefault().language
    // The preselected value is persisted too, so the choice shown is the choice stored.
    var primary by remember {
        val initial = settings.primaryLanguage ?: system.takeIf { it in LANGUAGE_TAGS } ?: "en"
        settings.primaryLanguage = initial
        mutableStateOf(initial)
    }
    var secondary by remember { mutableStateOf(settings.secondaryLanguage) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.language_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.language_hint), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.language_primary), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        LanguageOptions(options = LANGUAGE_TAGS, selected = primary, noneLabel = null) {
            primary = it!!
            settings.primaryLanguage = it
            if (secondary == it) {
                secondary = null
                settings.secondaryLanguage = null
            }
        }
        Text(stringResource(R.string.language_secondary), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        LanguageOptions(options = LANGUAGE_TAGS - primary, selected = secondary, noneLabel = R.string.language_none) {
            secondary = it
            settings.secondaryLanguage = it
        }
    }
}

@Composable
private fun LanguageOptions(options: List<String>, selected: String?, noneLabel: Int?, onSelect: (String?) -> Unit) {
    Column(modifier = Modifier.selectableGroup()) {
        if (noneLabel != null) LanguageRow(stringResource(noneLabel), selected == null) { onSelect(null) }
        options.forEach { tag ->
            val locale = Locale.forLanguageTag(tag)
            LanguageRow(locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }, selected == tag) { onSelect(tag) }
        }
    }
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 12.dp))
    }
}
