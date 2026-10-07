package io.github.abeleiras.aura.domain.update

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * OkHttp reports an unexpected runtime exception (for instance the `SecurityException` thrown by a
 * DNS lookup when INTERNET isn't declared) to the callback and then rethrows it on its own
 * dispatcher thread, where nothing can catch it and the whole app dies (0.1.0 crashed on launch
 * this way). Turning it into an `IOException` keeps it on the ordinary failure path.
 *
 * Install it as the first application interceptor of every client the app builds.
 */
object RuntimeExceptionsAsIoException : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = try {
        chain.proceed(chain.request())
    } catch (e: IOException) {
        throw e
    } catch (e: RuntimeException) {
        throw IOException("Unexpected ${e.javaClass.simpleName}: ${e.message}", e)
    }
}
