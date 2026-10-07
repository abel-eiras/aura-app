package io.github.abeleiras.aura.domain.notes

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Result of validating a `categorias.json` (FR-005-05, FR-005-06). */
sealed interface CatalogParseResult {
    data class Valid(val catalog: NoteTypeCatalog) : CatalogParseResult
    data class Invalid(val problems: List<String>) : CatalogParseResult
}

object NoteTypeCatalogs {
    private val json = Json {
        ignoreUnknownKeys = false
        prettyPrint = true
        encodeDefaults = false
    }

    private val idPattern = Regex("^[a-z0-9_]+$")
    private val tagPattern = Regex("^[a-z0-9-]+$")

    /** The three built-in types, read from the shared contract file (FR-005-01). */
    fun defaults(): NoteTypeCatalog {
        val stream = checkNotNull(NoteTypeCatalogs::class.java.getResourceAsStream("/contracts/categorias.default.json")) {
            "categorias.default.json missing from resources"
        }
        val text = stream.use { it.readBytes().toString(Charsets.UTF_8) }
        return (parse(text) as CatalogParseResult.Valid).catalog
    }

    fun parse(text: String): CatalogParseResult {
        val catalog = try {
            json.decodeFromString(NoteTypeCatalog.serializer(), text)
        } catch (e: SerializationException) {
            return CatalogParseResult.Invalid(listOf("Formato no válido: ${e.message?.lineSequence()?.firstOrNull()}"))
        } catch (e: IllegalArgumentException) {
            return CatalogParseResult.Invalid(listOf("Formato no válido: ${e.message?.lineSequence()?.firstOrNull()}"))
        }
        val problems = validate(catalog)
        return if (problems.isEmpty()) CatalogParseResult.Valid(catalog) else CatalogParseResult.Invalid(problems)
    }

    fun validate(catalog: NoteTypeCatalog): List<String> {
        val problems = mutableListOf<String>()
        if (catalog.version != 1) problems += "Versión no soportada: ${catalog.version}"
        if (catalog.types.isEmpty()) problems += "Debe haber al menos un tipo de nota"
        catalog.types.groupBy { it.id }.filterValues { it.size > 1 }.keys
            .forEach { problems += "Identificador duplicado: $it" }
        catalog.types.forEach { type ->
            if (!idPattern.matches(type.id)) problems += "Identificador no válido: '${type.id}'"
            if (!tagPattern.matches(type.tag)) problems += "Tag no válido en '${type.id}': '${type.tag}'"
            if (type.label.isBlank()) problems += "'${type.id}': falta la etiqueta"
            if (type.folder.isBlank()) problems += "'${type.id}': falta la carpeta"
            if (type.criterion.isBlank()) problems += "'${type.id}': falta el criterio"
            if (type.draftingPrompt.isBlank()) problems += "'${type.id}': falta el prompt de redacción"
        }
        if (catalog.types.count { it.isDefault } > 1) problems += "Solo un tipo puede ser el predeterminado"
        return problems
    }

    fun toJson(catalog: NoteTypeCatalog): String = json.encodeToString(NoteTypeCatalog.serializer(), catalog)
}
