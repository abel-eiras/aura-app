package io.github.abeleiras.aura.domain.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** Reads the project's GitHub Releases anonymously: one request per check (FR-006-04). */
class ReleasesClient(
    private val http: OkHttpClient,
    private val repoApiUrl: HttpUrl = DEFAULT_REPO_API_URL.toHttpUrl(),
) {
    suspend fun releases(): List<GitHubRelease> {
        val url = repoApiUrl.newBuilder().addPathSegment("releases").addQueryParameter("per_page", "20").build()
        val text = getText(url)
        return try {
            UpdateChecker.parseReleases(text)
        } catch (e: SerializationException) {
            throw UpdateException(UpdateFailure.BAD_RESPONSE, "Unexpected releases payload", e)
        }
    }

    /** Small text file (the `.sha256` asset). */
    suspend fun text(url: String): String = getText(url.toHttpUrl())

    private suspend fun getText(url: HttpUrl): String {
        val request = Request.Builder().url(url).header("Accept", "application/vnd.github+json").build()
        http.await(request).use { response ->
            response.failureOrNull()?.let { throw it }
            return withContext(Dispatchers.IO) {
                try {
                    response.body?.string() ?: throw UpdateException(UpdateFailure.BAD_RESPONSE, "Empty body")
                } catch (e: IOException) {
                    throw UpdateException(UpdateFailure.NETWORK, e.message, e)
                }
            }
        }
    }

    companion object {
        const val DEFAULT_REPO_API_URL = "https://api.github.com/repos/abel-eiras/aura-app"
    }
}
