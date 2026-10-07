package io.github.abeleiras.aura.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.abeleiras.aura.AppContainer
import io.github.abeleiras.aura.data.processing.ProcessingRepository
import io.github.abeleiras.aura.domain.notes.NoteType
import io.github.abeleiras.aura.domain.processing.JobRecord
import io.github.abeleiras.aura.domain.processing.JobStatus
import io.github.abeleiras.aura.domain.processing.StoredNote
import io.github.abeleiras.aura.domain.recording.RecordingNaming
import io.github.abeleiras.aura.domain.transcript.Speakers
import io.github.abeleiras.aura.domain.transcript.Transcript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NoteUiState(val transcript: Transcript, val note: StoredNote, val type: NoteType?, val date: String)

sealed interface NoteScreenState {
    data object Loading : NoteScreenState
    data object Missing : NoteScreenState

    /** [job] is non-null while the note is being redone, or after that failed. */
    data class Ready(val value: NoteUiState, val job: JobRecord? = null) : NoteScreenState
}

/** Shows one processed recording and lets the user name its speakers (spec 004, HU-004-1/2). */
class NoteViewModel(private val container: AppContainer, private val fileName: String) : ViewModel() {
    private val id = ProcessingRepository.idOf(fileName)
    private val startedAt = RecordingNaming.startedAtMillis(fileName) ?: 0L
    private val store = container.processing.store()

    /** What is on disk; null before the first read. Kept while a redo is running so the screen doesn't blank. */
    private val loaded = MutableStateFlow<NoteUiState?>(null)
    private val loadedOnce = MutableStateFlow(false)

    val state: StateFlow<NoteScreenState> = combine(loaded, loadedOnce, container.processing.jobs) { ui, once, jobs ->
        val job = jobs[id]?.takeUnless { it.status == JobStatus.READY }
        when {
            ui != null -> NoteScreenState.Ready(ui, job)
            once && job == null -> NoteScreenState.Missing
            else -> NoteScreenState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteScreenState.Loading)

    val canProcess: StateFlow<Boolean> = container.provider.state
        .map { it.canSend }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Every note type the user can switch to. */
    val types: List<NoteType> get() = container.processing.availableTypes()

    init {
        reload()
        // A redo finished: read the new note.
        viewModelScope.launch {
            container.processing.jobs.collect { jobs -> if (jobs[id]?.status == JobStatus.READY) reload() }
        }
    }

    private fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            val transcript = store.loadTranscript(id)
            val note = store.loadNote(id)
            if (transcript != null && note != null) {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(startedAt))
                loaded.value = NoteUiState(transcript, note, container.processing.typeById(note.typeId), date)
            }
            loadedOnce.value = true
        }
    }

    fun changeType(typeId: String) = container.processing.regenerate(fileName, typeId = typeId)

    fun rewrite() = container.processing.regenerate(fileName)

    fun transcribeAgain() = container.processing.regenerate(fileName, transcribeAgain = true)

    /** [from] becomes the same person as [into]; the note text follows (FR-004-02). */
    fun merge(from: String, into: String) {
        val current = loaded.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val merged = Speakers.merge(current.transcript, from, into)
            val note = current.note.copy(body = current.note.body.replace(from, into))
            store.saveTranscript(id, merged)
            store.saveNote(id, note)
            container.appScope.launch { container.exports.exportPending() }
            loaded.value = current.copy(transcript = merged, note = note)
        }
    }

    /** A blank [name] clears it. The note text keeps `SPEAKER_NN` and is shown with names applied (FR-004-03). */
    fun rename(speaker: String, name: String) {
        val current = loaded.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val renamed = Speakers.rename(current.transcript, speaker, name)
            store.saveTranscript(id, renamed)
            container.appScope.launch { container.exports.exportPending() } // names changed: refresh the exported files
            loaded.value = current.copy(transcript = renamed)
        }
    }
}
