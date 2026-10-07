package io.github.abeleiras.aura.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.abeleiras.aura.AppContainer
import io.github.abeleiras.aura.data.processing.ProcessingRepository
import io.github.abeleiras.aura.domain.notes.NoteType
import io.github.abeleiras.aura.domain.processing.StoredNote
import io.github.abeleiras.aura.domain.recording.RecordingNaming
import io.github.abeleiras.aura.domain.transcript.Speakers
import io.github.abeleiras.aura.domain.transcript.Transcript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NoteUiState(val transcript: Transcript, val note: StoredNote, val type: NoteType?, val date: String)

sealed interface NoteScreenState {
    data object Loading : NoteScreenState
    data object Missing : NoteScreenState
    data class Ready(val value: NoteUiState) : NoteScreenState
}

/** Shows one processed recording and lets the user name its speakers (spec 004, HU-004-1/2). */
class NoteViewModel(private val container: AppContainer, fileName: String) : ViewModel() {
    private val id = ProcessingRepository.idOf(fileName)
    private val startedAt = RecordingNaming.startedAtMillis(fileName) ?: 0L
    private val store = container.processing.store()

    private val _state = MutableStateFlow<NoteScreenState>(NoteScreenState.Loading)
    val state: StateFlow<NoteScreenState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val transcript = store.loadTranscript(id)
            val note = store.loadNote(id)
            _state.value = if (transcript == null || note == null) {
                NoteScreenState.Missing
            } else {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(startedAt))
                NoteScreenState.Ready(NoteUiState(transcript, note, container.processing.typeById(note.typeId), date))
            }
        }
    }

    /** A blank [name] clears it. The note text keeps `SPEAKER_NN` and is shown with names applied (FR-004-03). */
    fun rename(speaker: String, name: String) {
        val current = (_state.value as? NoteScreenState.Ready)?.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val renamed = Speakers.rename(current.transcript, speaker, name)
            store.saveTranscript(id, renamed)
            container.appScope.launch { container.exports.exportPending() } // names changed: refresh the exported files
            _state.value = NoteScreenState.Ready(current.copy(transcript = renamed))
        }
    }
}
