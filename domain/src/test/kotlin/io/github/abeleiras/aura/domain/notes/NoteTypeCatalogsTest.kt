package io.github.abeleiras.aura.domain.notes

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NoteTypeCatalogsTest {

    @Test
    fun `FR-005-01 built-in catalog has the three predefined types`() {
        val types = NoteTypeCatalogs.defaults().types
        assertEquals(listOf("reunion", "visita_cliente", "nota_personal"), types.map { it.id })
        assertEquals("nota_personal", types.single { it.isDefault }.id)
    }

    @Test
    fun `FR-005-07 predefined prompts tell the model to write in the transcript language`() {
        NoteTypeCatalogs.defaults().types.forEach {
            assertTrue("idioma que la transcripción" in it.draftingPrompt, it.id)
        }
    }

    @Test
    fun `FR-005-02 defaults are the same file as the spec contract`() {
        val spec = File("../specs/005-tipos-de-nota/contracts/categorias.default.json").readText()
        val parsed = (NoteTypeCatalogs.parse(spec) as CatalogParseResult.Valid).catalog
        assertEquals(parsed, NoteTypeCatalogs.defaults())
    }

    @Test
    fun `FR-005-06 export then import round-trips`() {
        val catalog = NoteTypeCatalogs.defaults()
        val again = NoteTypeCatalogs.parse(NoteTypeCatalogs.toJson(catalog))
        assertEquals(catalog, assertIs<CatalogParseResult.Valid>(again).catalog)
    }

    @Test
    fun `FR-005-06 rejects duplicate ids, empty catalogs and blank prompts without partial results`() {
        val dup = """{"version":1,"categorias":[
            {"id":"a","etiqueta":"A","carpeta":"A","tag":"a","criterio":"c","prompt_redaccion":"p"},
            {"id":"a","etiqueta":"B","carpeta":"B","tag":"b","criterio":"c","prompt_redaccion":" "}]}"""
        val result = assertIs<CatalogParseResult.Invalid>(NoteTypeCatalogs.parse(dup))
        assertTrue(result.problems.any { "duplicado" in it })
        assertTrue(result.problems.any { "prompt" in it })

        val empty = assertIs<CatalogParseResult.Invalid>(NoteTypeCatalogs.parse("""{"version":1,"categorias":[]}"""))
        assertTrue(empty.problems.any { "al menos un tipo" in it })
    }

    @Test
    fun `FR-005-06 rejects malformed json and unknown fields`() {
        assertIs<CatalogParseResult.Invalid>(NoteTypeCatalogs.parse("no es json"))
        val extra = """{"version":1,"categorias":[
            {"id":"a","etiqueta":"A","carpeta":"A","tag":"a","criterio":"c","prompt_redaccion":"p","sorpresa":1}]}"""
        assertIs<CatalogParseResult.Invalid>(NoteTypeCatalogs.parse(extra))
    }

    @Test
    fun `FR-005-05 at most one default type and ids must be slug-like`() {
        val two = """{"version":1,"categorias":[
            {"id":"a","etiqueta":"A","carpeta":"A","tag":"a","criterio":"c","prompt_redaccion":"p","por_defecto":true},
            {"id":"B b","etiqueta":"B","carpeta":"B","tag":"b","criterio":"c","prompt_redaccion":"p","por_defecto":true}]}"""
        val problems = assertIs<CatalogParseResult.Invalid>(NoteTypeCatalogs.parse(two)).problems
        assertTrue(problems.any { "predeterminado" in it })
        assertTrue(problems.any { "Identificador no válido" in it })
    }

    // FR-005-01, HU-005-1 scenario 2
    @Test
    fun `translated defaults keep ids, tags and default flag and translate the rest`() {
        val es = NoteTypeCatalogs.defaults("es").types
        listOf("en", "gl").forEach { lang ->
            val translated = NoteTypeCatalogs.defaults(lang).types
            assertEquals(es.map { it.id }, translated.map { it.id })
            assertEquals(es.map { it.tag }, translated.map { it.tag })
            assertEquals(es.map { it.isDefault }, translated.map { it.isDefault })
            translated.zip(es).forEach { (t, base) ->
                assertNotEquals(base.criterion, t.criterion, "$lang criterion of ${t.id}")
                assertNotEquals(base.draftingPrompt, t.draftingPrompt, "$lang prompt of ${t.id}")
                assertTrue("same language as the transcript" in t.draftingPrompt || "mesmo idioma" in t.draftingPrompt, "FR-005-07 $lang ${t.id}")
            }
        }
    }

    @Test
    fun `unknown language falls back to Spanish`() {
        assertEquals(NoteTypeCatalogs.defaults("es"), NoteTypeCatalogs.defaults("fr"))
    }
}
