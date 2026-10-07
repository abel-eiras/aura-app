package io.github.abeleiras.aura.domain.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionsTest {
    @Test
    fun `FR-006-03 versionCode grows with the version`() {
        assertEquals(10_200, Version.parse("1.2.0").versionCode)
        assertEquals(10_304, Version.parse("v1.3.4").versionCode)
        assertTrue(Version.parse("1.10.0").versionCode > Version.parse("1.9.9").versionCode)
    }

    @Test
    fun `semver ordering, pre-releases sort below their release`() {
        val order = listOf("0.9.0", "1.0.0-alpha", "1.0.0-alpha.2", "1.0.0-alpha.10", "1.0.0-beta.1", "1.0.0", "1.0.1", "1.10.0")
            .map(Version::parse)
        assertEquals(order, order.shuffled().sorted())
    }

    @Test
    fun `parseOrNull rejects non-semver tags`() {
        assertNull(Version.parseOrNull("latest"))
        assertNull(Version.parseOrNull("1.2"))
        assertEquals("1.3.0-beta.1", Version.parse("v1.3.0-beta.1").toString())
    }
}
