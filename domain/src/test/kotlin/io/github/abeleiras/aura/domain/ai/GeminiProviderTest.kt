package io.github.abeleiras.aura.domain.ai

import io.github.abeleiras.aura.domain.processing.ErrorReason
import io.github.abeleiras.aura.domain.processing.LanguageHints
import io.github.abeleiras.aura.domain.processing.ProviderException
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class GeminiProviderTest {
    private lateinit var server: MockWebServer
    private lateinit var tmp: File
    private lateinit var audio: File
    private val requests = mutableListOf<RecordedRequest>()

    private var fileStates = ArrayDeque(listOf("ACTIVE"))
    private var generateResponse: () -> MockResponse = { MockResponse().setBody(answer(SEGMENTS)) }

    private fun answer(inner: String) =
        """{"candidates":[{"content":{"parts":[{"text":${kotlinx.serialization.json.JsonPrimitive(inner)}}]},"finishReason":"STOP"}]}"""

    @BeforeTest
    fun setUp() {
        tmp = Files.createTempDirectory("aura-gemini").toFile()
        audio = File(tmp, "aura_20261007_101500.ogg").apply { writeBytes(ByteArray(5000) { (it % 200).toByte() }) }
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val path = request.path.orEmpty()
                return when {
                    request.method == "POST" && path.startsWith("/upload/v1beta/files") ->
                        MockResponse().setHeader("x-goog-upload-url", server.url("/upload-session/1").toString())
                    request.method == "POST" && path.startsWith("/upload-session/1") ->
                        MockResponse().setBody("""{"file":{"name":"files/abc","uri":"https://files/abc","state":"${fileStates.removeFirst().also { fileStates.addFirst(it) }}"}}""")
                    request.method == "GET" && path.startsWith("/v1beta/files/abc") ->
                        MockResponse().setBody("""{"state":"${fileStates.let { if (it.size > 1) it.removeFirst() else it.first() }}"}""")
                    request.method == "DELETE" -> MockResponse().setBody("{}")
                    request.method == "POST" && path.contains(":generateContent") -> generateResponse()
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
        tmp.deleteRecursively()
    }

    private fun provider() = GeminiProvider(
        OkHttpClient(), "AIzaTESTKEY", baseUrl = server.url("/"), pipelineVersion = "aura-app test",
        now = { Instant.parse("2026-10-07T10:00:00Z") }, pollInterval = 1.milliseconds,
    )

    private val hints = LanguageHints("es", "gl")

    // FR-003-02, FR-003-03, FR-003-04
    @Test
    fun `transcribes through the files api and normalizes speakers`() = runTest {
        fileStates = ArrayDeque(listOf("PROCESSING", "ACTIVE"))
        val transcript = provider().transcribe(audio, hints)

        assertEquals(listOf("SPEAKER_00", "SPEAKER_01", "SPEAKER_00"), transcript.segments.map { it.speaker })
        assertEquals(2, transcript.metadata.numSpeakersDetected)
        assertEquals("es", transcript.metadata.languageUsed)
        assertEquals("gemini", transcript.metadata.provider)
        assertEquals("aura_20261007_101500.ogg", transcript.metadata.sourceFile)
        assertTrue(transcript.fullText.startsWith("SPEAKER_00: Buenos días"))
        assertEquals(9.0, transcript.metadata.durationSeconds)
    }

    // FR-003-14, FR-002-05
    @Test
    fun `sends only the audio and the prompt, with the key in a header and the file deleted afterwards`() = runTest {
        provider().transcribe(audio, hints)

        assertTrue(requests.all { it.getHeader("x-goog-api-key") == "AIzaTESTKEY" || it.path!!.startsWith("/upload-session") })
        assertFalse(requests.any { it.path!!.contains("AIza") })
        val start = requests.first { it.path!!.startsWith("/upload/v1beta/files") }
        assertEquals("audio/ogg", start.getHeader("X-Goog-Upload-Header-Content-Type"))
        assertEquals("5000", start.getHeader("X-Goog-Upload-Header-Content-Length"))
        val finalize = requests.first { it.path!!.startsWith("/upload-session/1") }
        assertEquals(5000L, finalize.bodySize)
        val generate = requests.first { it.path!!.contains(":generateContent") }.body.readUtf8()
        assertTrue("https://files/abc" in generate)
        assertTrue("es, gl" in generate)
        assertEquals("DELETE", requests.last().method)
        assertEquals("/v1beta/files/abc", requests.last().path)
    }

    @Test
    fun `silence yields a transcript without content`() = runTest {
        generateResponse = { MockResponse().setBody(answer("""{"language":"es","segments":[]}""")) }
        val transcript = provider().transcribe(audio, hints)
        assertFalse(transcript.hasContent)
        assertEquals(0, transcript.metadata.numSpeakersDetected)
    }

    // spec 003, edge case: provider answer not in the expected format -> retried
    @Test
    fun `an answer that is not json is a transient invalid response`() = runTest {
        generateResponse = { MockResponse().setBody(answer("sorry, I can't")) }
        val e = assertFailsWith<ProviderException> { provider().transcribe(audio, hints) }
        assertEquals(ErrorReason.INVALID_RESPONSE, e.reason)
        assertTrue(e.transient)
        assertEquals("DELETE", requests.last().method) // cleaned up even on failure
    }

    @Test
    fun `a rejected file is a permanent failure`() = runTest {
        fileStates = ArrayDeque(listOf("FAILED"))
        val e = assertFailsWith<ProviderException> { provider().transcribe(audio, hints) }
        assertEquals(ErrorReason.UNKNOWN, e.reason)
        assertFalse(e.transient)
        assertEquals("DELETE", requests.last().method)
    }

    // FR-003-07
    @Test
    fun `http errors are classified as transient or permanent`() {
        fun check(code: Int, body: String, reason: ErrorReason, transient: Boolean) {
            val e = GeminiProvider.errorFor(code, body)
            assertEquals(reason, e.reason, "$code $body")
            assertEquals(transient, e.transient, "$code $body")
        }
        check(429, """{"error":{"message":"Quota exceeded for requests per minute"}}""", ErrorReason.RATE_LIMITED, true)
        check(429, """{"error":{"message":"Quota exceeded for metric ... per day"}}""", ErrorReason.QUOTA_EXHAUSTED, false)
        check(400, """{"error":{"message":"API key not valid. Please pass a valid API key."}}""", ErrorReason.INVALID_CREDENTIAL, false)
        check(403, "{}", ErrorReason.INVALID_CREDENTIAL, false)
        check(503, "overloaded", ErrorReason.PROVIDER_UNAVAILABLE, true)
        check(400, """{"error":{"message":"User location is not supported for the API use."}}""", ErrorReason.PROVIDER_UNAVAILABLE, false)
        check(400, """{"error":{"message":"Audio duration exceeds the limit"}}""", ErrorReason.AUDIO_TOO_LONG, false)
        check(404, "", ErrorReason.UNKNOWN, false)
    }

    @Test
    fun `a dropped connection is a transient network error`() = runTest {
        server.shutdown()
        val e = assertFailsWith<ProviderException> { provider().transcribe(audio, hints) }
        assertEquals(ErrorReason.NETWORK, e.reason)
        assertTrue(e.transient)
    }

    @Test
    fun `complete sends the system prompt and returns the text`() = runTest {
        generateResponse = { MockResponse().setBody(answer("## Notes\nhello")) }
        val text = provider().complete("be brief", "the transcript", null, 0.2)
        assertEquals("## Notes\nhello", text)
        val body = requests.first { it.path!!.contains(":generateContent") }.body.readUtf8()
        assertTrue("be brief" in body && "the transcript" in body && "systemInstruction" in body)
        assertTrue(requests.first().path!!.contains("models/${ProviderDefaults.GEMINI_DRAFT_MODEL}:generateContent"))
    }

    private companion object {
        const val SEGMENTS = """{"language":"es","segments":[
            {"speaker":"A","start":0.0,"end":3.5,"text":"Buenos días a todos"},
            {"speaker":"B","start":3.5,"end":6.0,"text":"Buenos días"},
            {"speaker":"A","start":6.0,"end":9.0,"text":"Empezamos"}]}"""
    }
}
