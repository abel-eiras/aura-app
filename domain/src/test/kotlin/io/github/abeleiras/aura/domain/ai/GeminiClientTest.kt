package io.github.abeleiras.aura.domain.ai

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeminiClientTest {
    private lateinit var server: MockWebServer

    @BeforeTest
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @AfterTest
    fun tearDown() = server.shutdown()

    private fun client(key: String = "AIzaTESTKEY") = GeminiClient(OkHttpClient(), key, server.url("/"))

    private fun check(code: Int, body: String): CredentialCheck {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
        var result: CredentialCheck? = null
        runTest { result = client().checkCredential() }
        return result!!
    }

    // FR-002-03
    @Test
    fun `valid key returns only models that can generate content`() {
        val body = """{"models":[
            {"name":"models/gemini-2.5-flash","displayName":"Gemini 2.5 Flash","supportedGenerationMethods":["generateContent"]},
            {"name":"models/embedding-001","displayName":"Embedding","supportedGenerationMethods":["embedContent"]}]}"""
        val result = assertIs<CredentialCheck.Valid>(check(200, body))
        assertEquals(listOf(GeminiModel("gemini-2.5-flash", "Gemini 2.5 Flash")), result.models)
    }

    // FR-002-03 / FR-002-05: the key travels in a header, never in the URL
    @Test
    fun `key is sent in a header and not in the url`() {
        check(200, """{"models":[]}""")
        val request = server.takeRequest()
        assertEquals("AIzaTESTKEY", request.getHeader("x-goog-api-key"))
        assertFalse(request.path!!.contains("AIza"))
        assertTrue(request.path!!.startsWith("/v1beta/models"))
    }

    @Test
    fun `400 is an invalid key`() {
        assertEquals(CredentialCheck.InvalidKey, check(400, """{"error":{"message":"API key not valid."}}"""))
    }

    @Test
    fun `403 is an invalid key`() {
        assertEquals(CredentialCheck.InvalidKey, check(403, "{}"))
    }

    @Test
    fun `unsupported location is reported as region problem`() {
        val body = """{"error":{"message":"User location is not supported for the API use."}}"""
        assertEquals(CredentialCheck.RegionNotSupported, check(400, body))
    }

    @Test
    fun `429 is rate limited`() {
        assertEquals(CredentialCheck.RateLimited, check(429, "{}"))
    }

    @Test
    fun `5xx is unavailable with its code`() {
        assertEquals(CredentialCheck.Unavailable(503), check(503, "not json"))
    }

    @Test
    fun `dropped connection is a network error`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        runTest { assertEquals(CredentialCheck.Network, client().checkCredential()) }
    }
}
