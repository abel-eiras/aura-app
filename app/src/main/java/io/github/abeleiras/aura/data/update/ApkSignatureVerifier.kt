package io.github.abeleiras.aura.data.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.github.abeleiras.aura.domain.update.UpdateChecker
import java.io.File

/**
 * An update must be signed with the same key as the installed app. Android would refuse
 * the install anyway, but failing here lets us explain why, and the check also guards
 * against a swapped file (FR-006-06).
 */
object ApkSignatureVerifier {
    fun matchesInstalled(context: Context, apk: File): Boolean {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNING_CERTIFICATES) ?: return false
        if (archive.packageName != context.packageName) return false
        val installed = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val archiveCerts = certificateDigests(archive)
        val installedCerts = certificateDigests(installed)
        return archiveCerts.isNotEmpty() && archiveCerts == installedCerts
    }

    private fun certificateDigests(info: PackageInfo): Set<String> =
        info.signingInfo?.apkContentsSigners.orEmpty().map { UpdateChecker.sha256Hex(it.toByteArray()) }.toSet()
}
