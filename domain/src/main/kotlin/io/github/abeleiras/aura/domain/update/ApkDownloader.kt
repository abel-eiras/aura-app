package io.github.abeleiras.aura.domain.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * Downloads a release APK and only hands it over if its SHA-256 matches the one published
 * next to it (FR-006-06). A bad or interrupted download never leaves a file behind.
 */
class ApkDownloader(
    private val http: OkHttpClient,
    private val releases: ReleasesClient,
) {
    /**
     * @param onProgress bytes read so far and the expected total (0 if unknown); called at most
     *   a few times per megabyte.
     */
    suspend fun download(update: AvailableUpdate, destination: File, onProgress: (Long, Long) -> Unit = { _, _ -> }): File {
        val expected = UpdateChecker.parseSha256File(releases.text(update.sha256Url))
            ?: throw UpdateException(UpdateFailure.BAD_RESPONSE, "Malformed .sha256 file")

        destination.parentFile?.mkdirs()
        val partial = File(destination.path + ".part")
        try {
            val actual = fetchTo(update, partial, onProgress)
            if (actual != expected) throw UpdateException(UpdateFailure.HASH_MISMATCH, "expected $expected, got $actual")
            withContext(Dispatchers.IO) {
                destination.delete()
                if (!partial.renameTo(destination)) throw IOException("Could not move $partial to $destination")
            }
            return destination
        } catch (e: Exception) {
            partial.delete()
            if (e is IOException) throw UpdateException(UpdateFailure.NETWORK, e.message, e)
            throw e
        }
    }

    private suspend fun fetchTo(update: AvailableUpdate, target: File, onProgress: (Long, Long) -> Unit): String {
        http.await(Request.Builder().url(update.apkUrl).build()).use { response ->
            response.failureOrNull()?.let { throw it }
            val body = response.body ?: throw UpdateException(UpdateFailure.BAD_RESPONSE, "Empty body")
            val total = body.contentLength().takeIf { it > 0 } ?: update.apkSize
            val digest = MessageDigest.getInstance("SHA-256")
            val context = coroutineContext
            return withContext(Dispatchers.IO) {
                var read = 0L
                var reported = 0L
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            context.ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            digest.update(buffer, 0, n)
                            read += n
                            if (read - reported >= REPORT_EVERY_BYTES) {
                                reported = read
                                onProgress(read, total)
                            }
                        }
                    }
                }
                onProgress(read, total)
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        }
    }

    private companion object {
        const val REPORT_EVERY_BYTES = 256L * 1024
    }
}
