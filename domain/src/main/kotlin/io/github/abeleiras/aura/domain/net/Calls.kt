package io.github.abeleiras.aura.domain.net

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Runs [request] and suspends for the response; cancelling the coroutine cancels the HTTP call.
 * Failures arrive as [IOException]. (Unexpected runtime exceptions are turned into IOExceptions by
 * `RuntimeExceptionsAsIoException`, which every client the app builds installs first.)
 */
suspend fun OkHttpClient.awaitResponse(request: Request): Response =
    suspendCancellableCoroutine { continuation ->
        val call: Call = newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) continuation.resumeWithException(e)
            }
        })
    }
