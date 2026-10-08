package io.github.abeleiras.aura.ui.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.ui.components.AuraOutlinedButton
import io.github.abeleiras.aura.ui.components.AuraButton
import io.github.abeleiras.aura.domain.ai.ProcessingMode
import io.github.abeleiras.aura.data.ai.ProviderController

/** Settings → Processing (FR-002-09). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderScreen(controller: ProviderController, onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.provider_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            ProviderSetup(controller)
            val state by controller.state.collectAsStateWithLifecycle()
            if (state.mode == ProcessingMode.GEMINI) AdvancedModels(controller)
        }
    }
}

/** Model names are free text on purpose: new models appear faster than app releases (FR-002-07). */
@Composable
private fun AdvancedModels(controller: ProviderController) {
    var transcribe by remember { mutableStateOf(controller.transcribeModel) }
    var draft by remember { mutableStateOf(controller.draftModel) }
    val state by controller.state.collectAsStateWithLifecycle()
    Column(modifier = Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.provider_advanced), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = transcribe,
            onValueChange = { transcribe = it },
            label = { Text(stringResource(R.string.provider_model_transcribe)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text(stringResource(R.string.provider_model_draft)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.models.isNotEmpty()) {
            Text(stringResource(R.string.provider_models_available, state.models.take(8).joinToString(", ")), style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AuraButton(onClick = { controller.saveModels(transcribe, draft) }) { Text(stringResource(R.string.provider_models_save)) }
            AuraOutlinedButton(onClick = {
                controller.saveModels("", "")
                transcribe = controller.transcribeModel
                draft = controller.draftModel // shows the alias until the new check picks a model
            }) { Text(stringResource(R.string.provider_models_defaults)) }
        }
    }
}
