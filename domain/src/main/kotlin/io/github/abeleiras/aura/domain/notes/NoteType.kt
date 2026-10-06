package io.github.abeleiras.aura.domain.notes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A kind of note (meeting, client visit, personal note...). Field names mirror
 * `[[notas.categorias]]` in aura-transcribe's config.toml (FR-005-02).
 */
@Serializable
data class NoteType(
    val id: String,
    @SerialName("etiqueta") val label: String,
    @SerialName("carpeta") val folder: String,
    val tag: String,
    @SerialName("criterio") val criterion: String,
    @SerialName("prompt_redaccion") val draftingPrompt: String,
    @SerialName("modelo") val model: String? = null,
    @SerialName("por_defecto") val isDefault: Boolean = false,
)

@Serializable
data class NoteTypeCatalog(
    val version: Int = 1,
    @SerialName("categorias") val types: List<NoteType>,
)
