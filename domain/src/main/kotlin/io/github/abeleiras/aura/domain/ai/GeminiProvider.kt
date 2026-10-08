package io.github.abeleiras.aura.domain.ai

import io.github.abeleiras.aura.domain.net.awaitResponse
import io.github.abeleiras.aura.domain.processing.AiProvider
import io.github.abeleiras.aura.domain.processing.ErrorReason
import io.github.abeleiras.aura.domain.processing.LanguageHints
import io.github.abeleiras.aura.domain.processing.ProviderException
import io.github.abeleiras.aura.domain.transcript.Segment
import io.github.abeleiras.aura.domain.transcript.Speakers
import io.github.abeleiras.aura.domain.transcript.Transcript
import io.github.abeleiras.aura.domain.transcript.TranscriptMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Gemini as an [AiProvider] (spec 003, ADR-0002): audio goes through the Files API (works for any size, unlike
 * inline data), transcription asks for JSON with speakers under a response schema, and the file is deleted from
 * Google's side when done. The key travels in a header only.
 */
class GeminiProvider(
    private val http: OkHttpClient,
    private val apiKey: String,
    private val transcribeModel: String = ProviderDefaults.GEMINI_TRANSCRIBE_MODEL,
    private val draftModel: String = ProviderDefaults.GEMINI_DRAFT_MODEL,
    private val pipelineVersion: String = "aura-app",
    private val baseUrl: HttpUrl = GeminiClient.DEFAULT_BASE_URL.toHttpUrl(),
    private val now: () -> Instant = { Instant.now() },
    private val pollInterval: Duration = 2.seconds,
    private val maxPolls: Int = 60,
) : AiProvider {

    override suspend fun transcribe(audio: File, hints: LanguageHints): Transcript {
        val started = System.nanoTime()
        val mime = mimeTypeOf(audio)
        val uploaded = upload(audio, mime)
        try {
            awaitActive(uploaded)
            val body = generate(
                model = transcribeModel,
                parts = buildJsonArray {
                    add(buildJsonObject { put("text", transcriptionPrompt(hints)) })
                    add(buildJsonObject { putJsonObject("file_data") { put("mime_type", mime); put("file_uri", uploaded.uri) } })
                },
                system = null,
                temperature = 0.0,
                schema = TRANSCRIPT_SCHEMA,
            )
            return parseTranscript(body, audio.name, hints, (System.nanoTime() - started) / 1e9)
        } finally {
            withContext(NonCancellable) { deleteQuietly(uploaded.name) }
        }
    }

    override suspend fun complete(system: String, user: String, model: String?, temperature: Double): String {
        val body = generate(
            model = model ?: draftModel,
            parts = buildJsonArray { add(buildJsonObject { put("text", user) }) },
            system = system,
            temperature = temperature,
            schema = null,
        )
        return candidateText(body).takeIf { it.isNotBlank() }
            ?: throw ProviderException(ErrorReason.INVALID_RESPONSE, transient = true, message = "Empty answer")
    }

    // ---- Files API ----

    private class UploadedFile(val name: String, val uri: String, val state: String)

    private suspend fun upload(audio: File, mime: String): UploadedFile {
        val start = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("upload/v1beta/files").build())
            .header("x-goog-api-key", apiKey)
            .header("X-Goog-Upload-Protocol", "resumable")
            .header("X-Goog-Upload-Command", "start")
            .header("X-Goog-Upload-Header-Content-Length", audio.length().toString())
            .header("X-Goog-Upload-Header-Content-Type", mime)
            .post("""{"file":{"display_name":"aura-recording"}}""".toRequestBody(JSON_MEDIA))
            .build()
        val uploadUrl = call(start) { response ->
            checkSuccess(response)
            response.header("x-goog-upload-url") ?: throw ProviderException(ErrorReason.INVALID_RESPONSE, true, "No upload URL")
        }
        val finalize = Request.Builder()
            .url(uploadUrl)
            .header("X-Goog-Upload-Offset", "0")
            .header("X-Goog-Upload-Command", "upload, finalize")
            .post(audio.asRequestBody(mime.toMediaType()))
            .build()
        return call(finalize) { response ->
            val text = checkSuccess(response)
            val file = parseObject(text)["file"]?.jsonObject
                ?: throw ProviderException(ErrorReason.INVALID_RESPONSE, true, "No file in upload answer")
            UploadedFile(
                name = file["name"]?.jsonPrimitive?.contentOrNull ?: throw ProviderException(ErrorReason.INVALID_RESPONSE, true),
                uri = file["uri"]?.jsonPrimitive?.contentOrNull ?: throw ProviderException(ErrorReason.INVALID_RESPONSE, true),
                state = file["state"]?.jsonPrimitive?.contentOrNull ?: "ACTIVE",
            )
        }
    }

    private suspend fun awaitActive(file: UploadedFile) {
        var state = file.state
        var polls = 0
        while (state != "ACTIVE") {
            if (state == "FAILED") {
                // Typically an audio format Gemini can't read (spec 003 plan: Plan B converts before uploading).
                throw ProviderException(ErrorReason.UNKNOWN, transient = false, message = "Audio file rejected by Gemini")
            }
            if (++polls > maxPolls) throw ProviderException(ErrorReason.PROVIDER_UNAVAILABLE, transient = true, message = "File not ready")
            delay(pollInterval)
            val request = Request.Builder()
                .url(baseUrl.newBuilder().addPathSegments("v1beta/${file.name}").build())
                .header("x-goog-api-key", apiKey)
                .build()
            state = call(request) { parseObject(checkSuccess(it))["state"]?.jsonPrimitive?.contentOrNull ?: "ACTIVE" }
        }
    }

    private suspend fun deleteQuietly(name: String) {
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("v1beta/$name").build())
            .header("x-goog-api-key", apiKey)
            .delete()
            .build()
        try {
            http.awaitResponse(request).close()
        } catch (_: IOException) {
            // Google deletes uploads after 48 h anyway.
        }
    }

    // ---- generateContent ----

    private suspend fun generate(model: String, parts: JsonArray, system: String?, temperature: Double, schema: JsonObject?): JsonObject {
        val payload = buildJsonObject {
            putJsonArray("contents") { add(buildJsonObject { put("role", "user"); put("parts", parts) }) }
            if (system != null) {
                putJsonObject("systemInstruction") { putJsonArray("parts") { add(buildJsonObject { put("text", system) }) } }
            }
            putJsonObject("generationConfig") {
                put("temperature", temperature)
                if (schema != null) {
                    put("responseMimeType", "application/json")
                    put("responseSchema", schema)
                }
            }
        }
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("v1beta/models/$model:generateContent").build())
            .header("x-goog-api-key", apiKey)
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .build()
        return call(request) { parseObject(checkSuccess(it)) }
    }

    private fun candidateText(body: JsonObject): String {
        val candidate = (body["candidates"] as? JsonArray)?.firstOrNull()?.jsonObject
        if (candidate == null) {
            val blocked = body["promptFeedback"]?.jsonObject?.get("blockReason")?.jsonPrimitive?.contentOrNull
            throw ProviderException(ErrorReason.UNKNOWN, transient = false, message = "No answer" + (blocked?.let { " (blocked: $it)" } ?: ""))
        }
        val parts = candidate["content"]?.jsonObject?.get("parts") as? JsonArray ?: JsonArray(emptyList())
        return parts.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }.joinToString("")
    }

    private fun parseTranscript(body: JsonObject, sourceFile: String, hints: LanguageHints, processingSeconds: Double): Transcript {
        val text = candidateText(body)
        val root = try {
            Json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            throw ProviderException(ErrorReason.INVALID_RESPONSE, transient = true, message = "Answer is not valid JSON", cause = e)
        }
        val raw = (root["segments"] as? JsonArray)
            ?: throw ProviderException(ErrorReason.INVALID_RESPONSE, transient = true, message = "No segments in answer")
        val segments = raw.mapIndexedNotNull { i, element ->
            val s = element as? JsonObject ?: return@mapIndexedNotNull null
            val start = s["start"].number()
            Segment(
                id = i,
                speaker = s["speaker"]?.jsonPrimitive?.contentOrNull ?: "SPEAKER_00",
                start = start,
                end = s["end"].number().coerceAtLeast(start),
                text = s["text"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            )
        }
        val language = root["language"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val draft = Transcript(
            metadata = TranscriptMetadata(
                sourceFile = sourceFile,
                processedAt = now().toString(),
                // The orchestrator replaces this with the real duration of the file; the last segment is the best guess here.
                durationSeconds = segments.maxOfOrNull { it.end } ?: 0.0,
                languageDetected = language,
                languageUsed = language ?: hints.primary,
                numSpeakersDetected = 0,
                pipelineVersion = pipelineVersion,
                processingSeconds = processingSeconds,
                provider = "gemini",
                transcriptionModel = transcribeModel,
            ),
            segments = segments,
            fullText = "",
        )
        return Speakers.normalize(draft)
    }

    private fun JsonElement?.number(): Double = (this as? JsonPrimitive)?.doubleOrNull ?: 0.0

    // ---- HTTP plumbing ----

    private suspend fun <T> call(request: Request, handle: suspend (Response) -> T): T = try {
        http.awaitResponse(request).use { handle(it) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        throw ProviderException(ErrorReason.NETWORK, transient = true, message = "Network error: ${e.javaClass.simpleName}: ${e.message}", cause = e)
    }

    private suspend fun checkSuccess(response: Response): String {
        val body = withContext(Dispatchers.IO) { response.body?.string().orEmpty() }
        if (response.isSuccessful) return body
        throw errorFor(response.code, body, response.request.url.encodedPath)
    }

    private fun parseObject(text: String): JsonObject = try {
        Json.parseToJsonElement(text).jsonObject
    } catch (e: Exception) {
        throw ProviderException(ErrorReason.INVALID_RESPONSE, transient = true, message = "Unreadable answer", cause = e)
    }

    companion object {
        private val JSON_MEDIA = "application/json".toMediaType()

        fun mimeTypeOf(file: File): String = when (file.extension.lowercase()) {
            "ogg", "oga", "opus" -> "audio/ogg"
            "wav" -> "audio/wav"
            "mp3" -> "audio/mp3"
            "flac" -> "audio/flac"
            "aac", "m4a" -> "audio/aac"
            else -> "audio/ogg"
        }

        fun transcriptionPrompt(hints: LanguageHints): String {
            val languages = listOfNotNull(hints.primary, hints.secondary).joinToString(", ")
            return "Transcribe this audio verbatim. The expected language(s): $languages (others may appear; transcribe each in the language spoken, " +
                "and report the main language you heard as an ISO 639-1 code in \"language\"). " +
                "Identify who speaks and label speakers SPEAKER_00, SPEAKER_01, ... in order of first appearance, consistently across the whole recording. " +
                "Give the start and end of each segment in seconds from the start of the audio. " +
                "Do not summarise, translate or add anything that was not said. If there is no speech, return an empty segments list. " +
                "Answer with JSON only."
        }

        private val LONG_WAIT = 10.minutes
        private val retryInText = Regex("retry in (?:(\\d+)h)?(?:(\\d+)m)?(?:([\\d.]+)s)?", RegexOption.IGNORE_CASE)
        private val retryDelayJson = Regex("\"retryDelay\"\\s*:\\s*\"([\\d.]+)s\"")

        /** "Please retry in 5h30m37.3s" or `"retryDelay": "19s"` from Google's error, as a duration. */
        fun parseRetryDelay(text: String): Duration? {
            retryInText.find(text)?.let { m ->
                val (h, min, sec) = m.destructured
                if (h.isNotEmpty() || min.isNotEmpty() || sec.isNotEmpty()) {
                    return ((h.toLongOrNull() ?: 0L) * 3600 + (min.toLongOrNull() ?: 0L) * 60 + (sec.toDoubleOrNull() ?: 0.0)).seconds
                }
            }
            return retryDelayJson.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.seconds
        }

        /** Maps an HTTP error to what the user should be told and whether retrying can help (FR-003-07). */
        fun errorFor(code: Int, body: String, where: String = ""): ProviderException {
            val message = try {
                Json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull.orEmpty()
            } catch (_: Exception) {
                ""
            }
            val lower = message.lowercase()
            val retry = parseRetryDelay(message + " " + body)
            // What the user can copy from the error details to report it: never contains the key (it travels in a header).
            val detail = "HTTP $code" + (if (where.isNotBlank()) " on $where" else "") + (if (message.isNotBlank()) ": $message" else "")
            return when {
                "location is not supported" in lower ->
                    ProviderException(ErrorReason.PROVIDER_UNAVAILABLE, false, "Gemini is not available in this country ($detail)")
                code == 404 || "no longer available" in lower || "is not found" in lower || "is not supported for" in lower ->
                    ProviderException(ErrorReason.MODEL_UNAVAILABLE, false, detail)
                // A long wait means the daily quota is gone; retrying every few minutes only burns requests.
                code == 429 && ("per day" in lower || "daily" in lower || "perday" in lower || (retry != null && retry >= LONG_WAIT)) ->
                    ProviderException(ErrorReason.QUOTA_EXHAUSTED, false, detail, retryAfter = retry)
                code == 429 -> ProviderException(ErrorReason.RATE_LIMITED, true, detail, retryAfter = retry)
                code == 401 || code == 403 || (code == 400 && "api key" in lower) ->
                    ProviderException(ErrorReason.INVALID_CREDENTIAL, false, detail)
                code == 400 && ("too long" in lower || "exceeds" in lower || "duration" in lower) ->
                    ProviderException(ErrorReason.AUDIO_TOO_LONG, false, detail)
                code >= 500 -> ProviderException(ErrorReason.PROVIDER_UNAVAILABLE, true, detail)
                else -> ProviderException(ErrorReason.UNKNOWN, false, detail)
            }
        }

        private val TRANSCRIPT_SCHEMA: JsonObject = buildJsonObject {
            put("type", "OBJECT")
            putJsonArray("required") { add(JsonPrimitive("language")); add(JsonPrimitive("segments")) }
            putJsonObject("properties") {
                putJsonObject("language") { put("type", "STRING") }
                putJsonObject("segments") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonArray("required") { listOf("speaker", "start", "end", "text").forEach { add(JsonPrimitive(it)) } }
                        putJsonObject("properties") {
                            putJsonObject("speaker") { put("type", "STRING") }
                            putJsonObject("start") { put("type", "NUMBER") }
                            putJsonObject("end") { put("type", "NUMBER") }
                            putJsonObject("text") { put("type", "STRING") }
                        }
                    }
                }
            }
        }
    }
}
