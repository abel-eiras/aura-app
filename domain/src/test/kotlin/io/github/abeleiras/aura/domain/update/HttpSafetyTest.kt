package io.github.abeleiras.aura.domain.update

import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HttpSafetyTest {
    private lateinit var server: MockWebServer
    private val uncaught = CopyOnWriteArrayList<Throwable>()
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    @BeforeTest
    fun setUp() {
        server = MockWebServer().apply { start() }
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> uncaught += e }
    }

    @AfterTest
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        server.shutdown()
    }

    /** What an unpermitted DNS lookup does: a RuntimeException deep inside the interceptor chain. */
    private val denied = Interceptor { throw SecurityException("Permission denied (missing INTERNET permission?)") }

    private fun releases(client: OkHttpClient) = ReleasesClient(client, server.url("/repos/x/y"))

    private fun waitForUncaught(millis: Long): Boolean {
        val end = System.currentTimeMillis() + millis
        while (uncaught.isEmpty() && System.currentTimeMillis() < end) Thread.sleep(20)
        return uncaught.isNotEmpty()
    }

    @Test
    fun `FR-007-05 an unexpected runtime exception becomes a network failure and kills nothing`() = runTest {
        val client = OkHttpClient.Builder().addInterceptor(RuntimeExceptionsAsIoException).addInterceptor(denied).build()

        val failure = assertFailsWith<UpdateException> { releases(client).releases() }

        assertEquals(UpdateFailure.NETWORK, failure.failure)
        assertTrue(failure.message!!.contains("SecurityException"))
        assertTrue(!waitForUncaught(500), "nothing may reach the uncaught-exception handler: $uncaught")
    }

    @Test
    fun `without the interceptor OkHttp rethrows on its own thread, which is what crashed 0 1 0`() = runTest {
        val client = OkHttpClient.Builder().addInterceptor(denied).build()

        assertFailsWith<UpdateException> { releases(client).releases() }

        assertTrue(waitForUncaught(2_000), "this guards the test above: it must be able to detect the problem")
        assertTrue(uncaught.any { it is SecurityException })
    }
}
