package io.github.abeleiras.aura.domain.ai

import io.github.abeleiras.aura.domain.net.awaitResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class GeminiModel(val id: String, val displayName: String)

/** Outcome of "Check" in the provider setup (FR-002-03): always something the UI can explain. */
sealed interface CredentialCheck {
    /** The key works. [models] are those that can generate content, for the model picker. */
    data class Valid(val models: List<GeminiModel>) : CredentialCheck

    data object InvalidKey : CredentialCheck

    /** Google doesn't offer the API in the user's country. */
    data object RegionNotSupported : CredentialCheck

    /** The key is fine but its quota is used up for now. */
    data object RateLimited : CredentialCheck

    data object Network : CredentialCheck

    data class Unavailable(val httpCode: Int) : CredentialCheck
}

/**
 * Talks to the Gemini API with the user's own key (ADR-0002). The key goes in a header, never in the URL,
 * so it can't end up in a log of requested URLs.
 *
 * Transcription (spec 003) is added next to [checkCredential]; this file starts with what the setup needs.
 */
class GeminiClient(
    private val http: OkHttpClient,
    private val apiKey: String,
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
) {
    suspend fun checkCredential(): CredentialCheck {
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("v1beta/models").addQueryParameter("pageSize", "200").build())
            .header("x-goog-api-key", apiKey)
            .build()
        return try {
            http.awaitResponse(request).use { response ->
                val body = withContext(Dispatchers.IO) { response.body?.string().orEmpty() }
                classify(response.code, body)
            }
        } catch (_: IOException) {
            CredentialCheck.Network
        }
    }

    private fun classify(code: Int, body: String): CredentialCheck {
        val error = parseError(body)
        return when {
            code == 200 -> CredentialCheck.Valid(parseModels(body))
            error.message.contains("location is not supported", ignoreCase = true) -> CredentialCheck.RegionNotSupported
            code == 429 -> CredentialCheck.RateLimited
            code in listOf(400, 401, 403) -> CredentialCheck.InvalidKey
            else -> CredentialCheck.Unavailable(code)
        }
    }

    private class ApiError(val message: String)

    private fun parseError(body: String): ApiError = try {
        ApiError(JSON.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content.orEmpty())
    } catch (_: Exception) {
        ApiError("")
    }

    private fun parseModels(body: String): List<GeminiModel> = try {
        val models = JSON.parseToJsonElement(body).jsonObject["models"] as? JsonArray ?: JsonArray(emptyList())
        models.map { it.jsonObject }
            .filter { m -> m["supportedGenerationMethods"].stringList().contains("generateContent") }
            .map { m ->
                val id = m["name"]!!.jsonPrimitive.content.removePrefix("models/")
                GeminiModel(id, m["displayName"]?.jsonPrimitive?.content ?: id)
            }
    } catch (_: Exception) {
        emptyList()
    }

    private fun JsonElement?.stringList(): List<String> =
        (this as? JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList()

    companion object {
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com"
        private val JSON = Json { ignoreUnknownKeys = true }
    }
}
