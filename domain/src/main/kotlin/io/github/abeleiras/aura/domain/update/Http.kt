package io.github.abeleiras.aura.domain.update

import io.github.abeleiras.aura.domain.net.awaitResponse
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/** Updater flavour of [awaitResponse]: network failures become an [UpdateException]. */
internal suspend fun okhttp3.OkHttpClient.await(request: Request): Response =
    try {
        awaitResponse(request)
    } catch (e: IOException) {
        throw UpdateException(UpdateFailure.NETWORK, e.message, e)
    }

internal fun Response.failureOrNull(): UpdateException? = when {
    isSuccessful -> null
    code == 403 || code == 429 -> UpdateException(UpdateFailure.RATE_LIMITED, "HTTP $code")
    else -> UpdateException(UpdateFailure.BAD_RESPONSE, "HTTP $code")
}
