package io.github.abeleiras.aura.domain.update

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Runs [request] and suspends; cancelling the coroutine cancels the HTTP call. */
internal suspend fun okhttp3.OkHttpClient.await(request: Request): Response =
    suspendCancellableCoroutine { continuation ->
        val call: Call = newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) {
                    continuation.resumeWithException(UpdateException(UpdateFailure.NETWORK, e.message, e))
                }
            }
        })
    }

internal fun Response.failureOrNull(): UpdateException? = when {
    isSuccessful -> null
    code == 403 || code == 429 -> UpdateException(UpdateFailure.RATE_LIMITED, "HTTP $code")
    else -> UpdateException(UpdateFailure.BAD_RESPONSE, "HTTP $code")
}
