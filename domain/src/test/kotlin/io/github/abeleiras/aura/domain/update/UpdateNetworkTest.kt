package io.github.abeleiras.aura.domain.update

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateNetworkTest {
    private lateinit var server: MockWebServer
    private lateinit var tmp: File
    private val http = OkHttpClient()

    @BeforeTest
    fun setUp() {
        server = MockWebServer().apply { start() }
        tmp = Files.createTempDirectory("aura-update-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
        tmp.deleteRecursively()
    }

    private fun client() = ReleasesClient(http, server.url("/repos/abel-eiras/aura-app"))

    private val apkBytes = ByteArray(700 * 1024) { (it % 251).toByte() }
    private val apkSha = UpdateChecker.sha256Hex(apkBytes)

    private fun update() = AvailableUpdate(
        version = Version.parse("1.2.0"),
        notes = "",
        releaseUrl = "",
        apkUrl = server.url("/dl/aura-1.2.0.apk").toString(),
        apkSize = apkBytes.size.toLong(),
        sha256Url = server.url("/dl/aura-1.2.0.apk.sha256").toString(),
        isPreRelease = false,
    )

    private fun enqueueApk(sha: String = apkSha) {
        server.enqueue(MockResponse().setBody("$sha  aura-1.2.0.apk\n")) // .sha256 first
        server.enqueue(MockResponse().setBody(Buffer().write(apkBytes)))
    }

    @Test
    fun `FR-006-04 reads releases from the GitHub API with one anonymous request`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """[{"tag_name":"v1.2.0","body":"b","html_url":"u","assets":[
                    {"name":"aura-1.2.0.apk","browser_download_url":"a","size":5},
                    {"name":"aura-1.2.0.apk.sha256","browser_download_url":"s","size":1}]}]""",
            ),
        )
        val releases = client().releases()
        assertEquals("v1.2.0", releases.single().tagName)
        val request = server.takeRequest()
        assertEquals("/repos/abel-eiras/aura-app/releases?per_page=20", request.path)
        assertEquals("application/vnd.github+json", request.getHeader("Accept"))
        assertEquals(null, request.getHeader("Authorization"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `spec 006 rate limiting is reported as such, other errors as bad response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))
        assertEquals(UpdateFailure.RATE_LIMITED, assertFailsWith<UpdateException> { client().releases() }.failure)
        server.enqueue(MockResponse().setResponseCode(429))
        assertEquals(UpdateFailure.RATE_LIMITED, assertFailsWith<UpdateException> { client().releases() }.failure)
        server.enqueue(MockResponse().setResponseCode(500))
        assertEquals(UpdateFailure.BAD_RESPONSE, assertFailsWith<UpdateException> { client().releases() }.failure)
        server.enqueue(MockResponse().setBody("<html>nope</html>"))
        assertEquals(UpdateFailure.BAD_RESPONSE, assertFailsWith<UpdateException> { client().releases() }.failure)
    }

    @Test
    fun `an unreachable server is a network failure`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals(UpdateFailure.NETWORK, assertFailsWith<UpdateException> { client().releases() }.failure)
    }

    @Test
    fun `FR-006-06 downloads the apk, reports progress and verifies the sha256`() = runTest {
        enqueueApk()
        val progress = mutableListOf<Pair<Long, Long>>()
        val dest = File(tmp, "updates/aura-1.2.0.apk")

        val file = ApkDownloader(http, client()).download(update(), dest) { read, total -> progress += read to total }

        assertEquals(dest, file)
        assertTrue(apkBytes.contentEquals(file.readBytes()))
        assertFalse(File(dest.path + ".part").exists())
        assertEquals(apkBytes.size.toLong(), progress.last().first)
        assertEquals(apkBytes.size.toLong(), progress.last().second)
        assertTrue(progress.map { it.first }.zipWithNext().all { (a, b) -> a <= b })
    }

    @Test
    fun `FR-006-06 a hash mismatch leaves no file behind`() = runTest {
        enqueueApk(sha = "0".repeat(64))
        val dest = File(tmp, "aura-1.2.0.apk")
        val e = assertFailsWith<UpdateException> { ApkDownloader(http, client()).download(update(), dest) }
        assertEquals(UpdateFailure.HASH_MISMATCH, e.failure)
        assertFalse(dest.exists())
        assertFalse(File(dest.path + ".part").exists())
    }

    @Test
    fun `a malformed sha256 file is a bad response and nothing is downloaded`() = runTest {
        server.enqueue(MockResponse().setBody("<html>404</html>"))
        val e = assertFailsWith<UpdateException> { ApkDownloader(http, client()).download(update(), File(tmp, "a.apk")) }
        assertEquals(UpdateFailure.BAD_RESPONSE, e.failure)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a failed download keeps a previously downloaded apk untouched`() = runTest {
        val dest = File(tmp, "aura-1.2.0.apk").apply { writeText("old") }
        enqueueApk(sha = "f".repeat(64))
        assertFailsWith<UpdateException> { ApkDownloader(http, client()).download(update(), dest) }
        assertEquals("old", dest.readText())
    }
}
