package io.github.abeleiras.aura.ui.notes

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.ui.components.AuraTextButton
import io.github.abeleiras.aura.domain.notes.NoteRenderer
import io.github.abeleiras.aura.domain.notes.NoteType
import io.github.abeleiras.aura.domain.processing.JobRecord
import io.github.abeleiras.aura.domain.processing.JobStatus
import io.github.abeleiras.aura.domain.recording.RecordingPolicy
import io.github.abeleiras.aura.domain.transcript.Speakers
import io.github.abeleiras.aura.ui.recordings.processingLabel

private enum class NoteDialog { None, ChangeType, ConfirmTranscribe }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(viewModel: NoteViewModel, onBackClick: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val canProcess by viewModel.canProcess.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val generic = stringResource(R.string.speaker_generic)
    val ready = (state as? NoteScreenState.Ready)?.value
    val job = (state as? NoteScreenState.Ready)?.job
    var speakerDialog by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf(NoteDialog.None) }
    var menuOpen by remember { mutableStateOf(false) }

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
                        if (canProcess) {
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.note_more))
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.note_change_type)) },
                                        onClick = { menuOpen = false; dialog = NoteDialog.ChangeType },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.note_rewrite)) },
                                        onClick = { menuOpen = false; viewModel.rewrite() },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.note_transcribe_again)) },
                                        onClick = { menuOpen = false; dialog = NoteDialog.ConfirmTranscribe },
                                    )
                                }
                            }
                        }
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
            is NoteScreenState.Ready -> NoteContent(
                ui = s.value,
                job = job,
                generic = generic,
                onSpeaker = { speakerDialog = it },
                modifier = Modifier.padding(padding),
            )
        }
    }

    val target = speakerDialog
    if (target != null && ready != null) {
        SpeakerDialog(
            speakerLabel = Speakers.displayName(ready.transcript, target, generic),
            initial = ready.transcript.metadata.speakerNames?.get(target).orEmpty(),
            others = Speakers.speakers(ready.transcript).filter { it != target }
                .map { it to Speakers.displayName(ready.transcript, it, generic) },
            onSave = {
                viewModel.rename(target, it)
                speakerDialog = null
            },
            onMerge = {
                viewModel.merge(from = target, into = it)
                speakerDialog = null
            },
            onDismiss = { speakerDialog = null },
        )
    }
    when (dialog) {
        NoteDialog.None -> Unit
        NoteDialog.ChangeType -> ChangeTypeDialog(
            types = viewModel.types,
            current = ready?.type?.id,
            onPick = {
                dialog = NoteDialog.None
                viewModel.changeType(it)
            },
            onDismiss = { dialog = NoteDialog.None },
        )
        NoteDialog.ConfirmTranscribe -> AlertDialog(
            onDismissRequest = { dialog = NoteDialog.None },
            title = { Text(stringResource(R.string.note_transcribe_again)) },
            text = { Text(stringResource(R.string.note_transcribe_again_warning)) },
            confirmButton = {
                AuraTextButton(onClick = {
                    dialog = NoteDialog.None
                    viewModel.transcribeAgain()
                }) { Text(stringResource(R.string.note_transcribe_again_confirm)) }
            },
            dismissButton = { AuraTextButton(onClick = { dialog = NoteDialog.None }) { Text(stringResource(R.string.provider_cancel)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteContent(ui: NoteUiState, job: JobRecord?, generic: String, onSpeaker: (String) -> Unit, modifier: Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { Text(ui.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
        if (job != null) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    processingLabel(job)?.let {
                        Text(it, color = if (job.status == JobStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                    if (job.status != JobStatus.ERROR) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        }
        items(Speakers.applyNames(ui.transcript, ui.note.body, generic).lines()) { line -> MarkdownLine(line) }
        item {
            Text(stringResource(R.string.note_speakers_section), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Speakers.speakers(ui.transcript).forEach { speaker ->
                    AssistChip(onClick = { onSpeaker(speaker) }, label = { Text(Speakers.displayName(ui.transcript, speaker, generic)) })
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
private fun SpeakerDialog(
    speakerLabel: String,
    initial: String,
    others: List<Pair<String, String>>,
    onSave: (String) -> Unit,
    onMerge: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.note_rename_title, speakerLabel)) },
        text = {
            Column {
                Text(stringResource(R.string.note_rename_hint), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.padding(top = 8.dp))
                if (others.isNotEmpty()) {
                    Text(stringResource(R.string.note_merge_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
                    Text(stringResource(R.string.note_merge_hint), style = MaterialTheme.typography.bodySmall)
                    others.forEach { (label, display) ->
                        AuraTextButton(onClick = { onMerge(label) }) { Text(stringResource(R.string.note_merge_into, display)) }
                    }
                }
            }
        },
        confirmButton = { AuraTextButton(onClick = { onSave(name) }) { Text(stringResource(R.string.note_rename_save)) } },
        dismissButton = { AuraTextButton(onClick = onDismiss) { Text(stringResource(R.string.provider_cancel)) } },
    )
}

@Composable
private fun ChangeTypeDialog(types: List<NoteType>, current: String?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.note_change_type)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                types.forEach { type ->
                    Row(
                        modifier = Modifier.fillMaxWidth().selectable(selected = type.id == current, onClick = { onPick(type.id) }, role = Role.RadioButton).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = type.id == current, onClick = null)
                        Text(type.label, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { AuraTextButton(onClick = onDismiss) { Text(stringResource(R.string.provider_cancel)) } },
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
