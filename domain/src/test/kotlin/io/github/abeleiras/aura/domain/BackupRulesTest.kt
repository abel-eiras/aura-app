package io.github.abeleiras.aura.domain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** CE-002-3 / FR-002-05: credentials must never travel in backups or device transfers. */
class BackupRulesTest {
    private val rules = listOf(
        "../app/src/main/res/xml/backup_rules.xml",
        "../app/src/main/res/xml/data_extraction_rules.xml",
    ).map { File(it) }

    @Test
    fun `the secrets preferences file is excluded from every backup path`() {
        rules.forEach { file ->
            val text = file.readText()
            val excluded = Regex("""<exclude\s+domain="sharedpref"\s+path="secrets\.xml"""").findAll(text).count()
            val sections = Regex("<(full-backup-content|cloud-backup|device-transfer)>").findAll(text).count()
            assertTrue(excluded >= sections, "${file.name} must exclude sharedpref secrets.xml in each of its $sections section(s)")
        }
    }

    @Test
    fun `the credential store writes to the excluded file`() {
        val source = File("../app/src/main/java/io/github/abeleiras/aura/data/ai/CredentialStore.kt").readText()
        assertTrue("getSharedPreferences(\"secrets\"" in source, "CredentialStore must use the preferences file named secrets")
    }
}
