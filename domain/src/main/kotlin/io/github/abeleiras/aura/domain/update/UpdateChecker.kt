package io.github.abeleiras.aura.domain.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/** Subset of GitHub's "list releases" response that the updater needs. */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0,
)

/** A newer version the user can install (FR-006-05). */
data class AvailableUpdate(
    val version: Version,
    val notes: String,
    val releaseUrl: String,
    val apkUrl: String,
    val apkSize: Long,
    val sha256Url: String,
    val isPreRelease: Boolean,
)

object UpdateChecker {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseReleases(text: String): List<GitHubRelease> = json.decodeFromString(text)

    /**
     * Picks the newest installable release strictly newer than [installed]
     * (FR-006-04/05, spec 006 edge cases: never downgrades, drafts are ignored,
     * pre-releases only if [includePreReleases]). Releases without the
     * `aura-<version>.apk` and `.apk.sha256` assets are skipped (FR-006-01).
     */
    fun findUpdate(
        releases: List<GitHubRelease>,
        installed: Version,
        includePreReleases: Boolean,
    ): AvailableUpdate? = releases.asSequence()
        .filter { !it.draft && (includePreReleases || !it.prerelease) }
        .mapNotNull { release ->
            val version = Version.parseOrNull(release.tagName) ?: return@mapNotNull null
            if (version.isPreRelease && !includePreReleases) return@mapNotNull null
            if (version <= installed) return@mapNotNull null
            val apk = release.assets.firstOrNull { it.name == "aura-$version.apk" } ?: return@mapNotNull null
            val sha = release.assets.firstOrNull { it.name == "aura-$version.apk.sha256" } ?: return@mapNotNull null
            AvailableUpdate(
                version = version,
                notes = release.body.orEmpty(),
                releaseUrl = release.htmlUrl,
                apkUrl = apk.downloadUrl,
                apkSize = apk.size,
                sha256Url = sha.downloadUrl,
                isPreRelease = version.isPreRelease,
            )
        }
        .maxByOrNull { it.version }

    /** Extracts the hex digest from a `sha256sum`-style file ("<hex>  <name>"). */
    fun parseSha256File(text: String): String? =
        text.trim().lineSequence().firstOrNull()?.trim()?.split(Regex("\\s+"))?.firstOrNull()
            ?.lowercase()?.takeIf { it.matches(Regex("[0-9a-f]{64}")) }

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /** Streaming variant for large APKs. */
    fun sha256Hex(stream: java.io.InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val n = stream.read(buffer)
            if (n < 0) break
            digest.update(buffer, 0, n)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
