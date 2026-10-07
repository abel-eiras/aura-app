package io.github.abeleiras.aura.domain.transcript

/** Speaker handling: stable labels, user-given names, merging (FR-003-03, FR-004-02, FR-004-03). */
object Speakers {
    private val labelPattern = Regex("SPEAKER_(\\d{2})")

    fun label(index: Int): String = "SPEAKER_%02d".format(index)

    /** Zero-based position encoded in a `SPEAKER_NN` label, or null if it isn't one. */
    fun indexOf(label: String): Int? = labelPattern.matchEntire(label)?.groupValues?.get(1)?.toInt()

    /** `full_text` as defined by the contract: one `SPEAKER_00: text` line per segment. */
    fun buildFullText(segments: List<Segment>): String =
        segments.joinToString("\n") { "${it.speaker}: ${it.text}" }

    /**
     * Relabels whatever the provider returned to `SPEAKER_00`, `SPEAKER_01`... in
     * order of first appearance, drops blank segments, renumbers ids, and
     * recomputes `num_speakers_detected` and `full_text` (FR-003-03).
     */
    fun normalize(transcript: Transcript): Transcript {
        val mapping = linkedMapOf<String, String>()
        val segments = transcript.segments
            .filter { it.text.isNotBlank() }
            .mapIndexed { i, s ->
                val label = mapping.getOrPut(s.speaker) { label(mapping.size) }
                s.copy(id = i, speaker = label, text = s.text.trim())
            }
        val names = transcript.metadata.speakerNames
            ?.mapNotNull { (old, name) -> mapping[old]?.let { it to name } }
            ?.toMap()
            ?.takeIf { it.isNotEmpty() }
        return transcript.copy(
            metadata = transcript.metadata.copy(numSpeakersDetected = mapping.size, speakerNames = names),
            segments = segments,
            fullText = buildFullText(segments),
        )
    }

    fun speakers(transcript: Transcript): List<String> = transcript.segments.map { it.speaker }.distinct().sorted()

    /** Name to show for [speaker]: the user's name, or "<generic> N" (N is 1-based). */
    fun displayName(transcript: Transcript, speaker: String, generic: String = "Hablante"): String {
        transcript.metadata.speakerNames?.get(speaker)?.takeIf { it.isNotBlank() }?.let { return it }
        val n = indexOf(speaker)?.plus(1) ?: (speakers(transcript).indexOf(speaker) + 1)
        return "$generic $n"
    }

    /** Sets or clears (blank name) the name of one speaker. */
    fun rename(transcript: Transcript, speaker: String, name: String): Transcript {
        require(transcript.segments.any { it.speaker == speaker }) { "Unknown speaker $speaker" }
        val names = (transcript.metadata.speakerNames ?: emptyMap()).toMutableMap()
        if (name.isBlank()) names.remove(speaker) else names[speaker] = name.trim()
        return transcript.copy(
            metadata = transcript.metadata.copy(speakerNames = names.takeIf { it.isNotEmpty() }),
        )
    }

    /** Makes [from] and [into] the same speaker; [into] keeps its name (FR-004-02). */
    fun merge(transcript: Transcript, from: String, into: String): Transcript {
        require(from != into) { "Cannot merge a speaker with itself" }
        val present = transcript.segments.map { it.speaker }.toSet()
        require(from in present && into in present) { "Unknown speaker" }
        val segments = transcript.segments.map { if (it.speaker == from) it.copy(speaker = into) else it }
        val names = transcript.metadata.speakerNames
            ?.let { it - from }
            ?.takeIf { it.isNotEmpty() }
        return transcript.copy(
            metadata = transcript.metadata.copy(
                numSpeakersDetected = segments.map { it.speaker }.distinct().size,
                speakerNames = names,
            ),
            segments = segments,
            fullText = buildFullText(segments),
        )
    }

    /** Replaces every `SPEAKER_NN` token in [text] with the display name (FR-004-03). */
    fun applyNames(transcript: Transcript, text: String, generic: String = "Hablante"): String =
        labelPattern.replace(text) { displayName(transcript, it.value, generic) }

    /** Display names of the speakers present, in label order, as exported in `hablantes_nombres`. */
    fun displayNames(transcript: Transcript, generic: String = "Hablante"): List<String> =
        speakers(transcript).map { displayName(transcript, it, generic) }
}
