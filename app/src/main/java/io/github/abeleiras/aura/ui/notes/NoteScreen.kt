package io.github.abeleiras.aura.ui.notes

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.domain.notes.NoteRenderer
import io.github.abeleiras.aura.domain.recording.RecordingPolicy
import io.github.abeleiras.aura.domain.transcript.Speakers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(viewModel: NoteViewModel, onBackClick: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val generic = stringResource(R.string.speaker_generic)
    val ready = (state as? NoteScreenState.Ready)?.value
    var renaming by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ready?.type?.label ?: stringResource(R.string.note_title_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                },
                actions = {
                    if (ready != null) {
                        IconButton(onClick = {
                            val text = ready.type?.let { NoteRenderer.render(it, ready.date, ready.transcript, ready.note.body, genericSpeaker = generic) }
                                ?: Speakers.applyNames(ready.transcript, ready.note.body, generic)
                            context.shareText(text, context.getString(R.string.note_share_note))
                        }) { Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.note_share_note)) }
                        IconButton(onClick = {
                            context.shareText(transcriptText(ready, generic), context.getString(R.string.note_share_transcript))
                        }) { Icon(Icons.Outlined.Description, contentDescription = stringResource(R.string.note_share_transcript)) }
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            NoteScreenState.Loading -> Box(Modifier.fillMaxSize().padding(padding))
            NoteScreenState.Missing -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.note_not_found))
            }
            is NoteScreenState.Ready -> NoteContent(s.value, generic, onRename = { renaming = it }, modifier = Modifier.padding(padding))
        }
    }

    val target = renaming
    if (target != null && ready != null) {
        RenameDialog(
            speakerLabel = Speakers.displayName(ready.transcript, target, generic),
            initial = ready.transcript.metadata.speakerNames?.get(target).orEmpty(),
            onSave = {
                viewModel.rename(target, it)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun NoteContent(ui: NoteUiState, generic: String, onRename: (String) -> Unit, modifier: Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { Text(ui.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
        items(Speakers.applyNames(ui.transcript, ui.note.body, generic).lines()) { line -> MarkdownLine(line) }
        item {
            Text(stringResource(R.string.note_speakers_section), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Speakers.speakers(ui.transcript).forEach { speaker ->
                    AssistChip(onClick = { onRename(speaker) }, label = { Text(Speakers.displayName(ui.transcript, speaker, generic)) })
                }
            }
            Text(stringResource(R.string.note_transcript_section), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
        }
        items(ui.transcript.segments, key = { it.id }) { segment ->
            SelectionContainer {
                Text(
                    text = "[${RecordingPolicy.formatDuration((segment.start * 1000).toLong())}] " +
                        "${Speakers.displayName(ui.transcript, segment.speaker, generic)}: ${segment.text}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
        item { Box(Modifier.padding(bottom = 32.dp)) }
    }
}

/** Just enough Markdown for the drafted notes: headings stand out, the rest is plain selectable text. */
@Composable
private fun MarkdownLine(line: String) {
    val heading = line.trimStart().takeIf { it.startsWith("#") }
    SelectionContainer {
        if (heading != null) {
            Text(
                text = heading.trimStart('#').trim(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            Text(text = line.replace("**", ""), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RenameDialog(speakerLabel: String, initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.note_rename_title, speakerLabel)) },
        text = {
            Column {
                Text(stringResource(R.string.note_rename_hint), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text(stringResource(R.string.note_rename_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.provider_cancel)) } },
    )
}

private fun transcriptText(ui: NoteUiState, generic: String): String =
    ui.transcript.segments.joinToString("\n") {
        "${Speakers.displayName(ui.transcript, it.speaker, generic)}: ${it.text}"
    }

private fun Context.shareText(text: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
