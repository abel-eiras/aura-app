package io.github.abeleiras.aura.domain.update

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateCheckerTest {
    private fun release(tag: String, pre: Boolean = false, draft: Boolean = false, assets: Boolean = true) = GitHubRelease(
        tagName = tag, body = "notas $tag", prerelease = pre, draft = draft, htmlUrl = "https://example/r/$tag",
        assets = if (!assets) emptyList() else listOf(
            GitHubAsset("aura-${tag.removePrefix("v")}.apk", "https://example/$tag.apk", 1000),
            GitHubAsset("aura-${tag.removePrefix("v")}.apk.sha256", "https://example/$tag.sha", 90),
        ),
    )

    private val installed = Version.parse("1.1.0")

    @Test
    fun `FR-006-05 offers the newest stable release and its notes`() {
        val u = UpdateChecker.findUpdate(listOf(release("v1.2.0"), release("v1.3.0"), release("v1.0.0")), installed, false)!!
        assertEquals(Version.parse("1.3.0"), u.version)
        assertEquals("notas v1.3.0", u.notes)
        assertEquals("https://example/v1.3.0.apk", u.apkUrl)
        assertEquals("https://example/v1.3.0.sha", u.sha256Url)
    }

    @Test
    fun `FR-006-05 pre-releases only when opted in, drafts never`() {
        val rs = listOf(release("v1.2.0"), release("v1.3.0-beta.1", pre = true), release("v9.0.0", draft = true))
        assertEquals(Version.parse("1.2.0"), UpdateChecker.findUpdate(rs, installed, false)!!.version)
        assertEquals(Version.parse("1.3.0-beta.1"), UpdateChecker.findUpdate(rs, installed, true)!!.version)
    }

    @Test
    fun `spec 006 never downgrades, including from a pre-release`() {
        assertNull(UpdateChecker.findUpdate(listOf(release("v1.0.0")), installed, false))
        assertNull(UpdateChecker.findUpdate(listOf(release("v1.1.0")), installed, false))
        val beta = Version.parse("1.2.0-beta.1")
        assertNull(UpdateChecker.findUpdate(listOf(release("v1.1.0")), beta, true))
        assertEquals(Version.parse("1.2.0"), UpdateChecker.findUpdate(listOf(release("v1.2.0")), beta, false)!!.version)
    }

    @Test
    fun `FR-006-01 releases without apk and sha256 assets are skipped`() {
        assertNull(UpdateChecker.findUpdate(listOf(release("v1.2.0", assets = false)), installed, false))
    }

    @Test
    fun `parses the GitHub releases payload`() {
        val json = """[{"tag_name":"v1.2.0","name":"x","body":"b","draft":false,"prerelease":false,"html_url":"u","extra":1,
            "assets":[{"name":"aura-1.2.0.apk","browser_download_url":"a","size":5,"id":3},
                      {"name":"aura-1.2.0.apk.sha256","browser_download_url":"s","size":1}]}]"""
        assertEquals("a", UpdateChecker.findUpdate(UpdateChecker.parseReleases(json), installed, false)!!.apkUrl)
    }

    @Test
    fun `FR-006-06 sha256 helpers`() {
        val hex = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        assertEquals(hex, UpdateChecker.sha256Hex("abc".toByteArray()))
        assertEquals(hex, UpdateChecker.sha256Hex(ByteArrayInputStream("abc".toByteArray())))
        assertEquals(hex, UpdateChecker.parseSha256File("${hex.uppercase()}  aura-1.2.0.apk\n"))
        assertNull(UpdateChecker.parseSha256File("<html>404</html>"))
    }
}
