package io.github.abeleiras.aura.domain.export

/** Where one recording stands with respect to the export folder (spec 004, "Entidades clave"). */
enum class ExportStatus {
    /** No folder configured, or the recording predates the choice "only export new ones". */
    NONE,

    /** Waiting for the next export attempt (also what a failed one becomes after a restart). */
    PENDING,

    /** The last attempt in this session failed; the user can retry. */
    ERROR,

    EXPORTED,
}

object ExportPolicy {
    /**
     * Derived state, so nothing needs a database migration (see plan): a recording is exported
     * if it is in the ledger, and otherwise pending if it falls in the range chosen by the user.
     *
     * @param exportFromMillis recordings that started before this are left alone (0 = export all)
     */
    fun status(
        folderConfigured: Boolean,
        fileName: String,
        startedAtMillis: Long,
        exportFromMillis: Long,
        exported: Set<String>,
        failed: Set<String>,
    ): ExportStatus = when {
        fileName in exported -> ExportStatus.EXPORTED
        !folderConfigured || startedAtMillis < exportFromMillis -> ExportStatus.NONE
        fileName in failed -> ExportStatus.ERROR
        else -> ExportStatus.PENDING
    }
}

object ExportNaming {
    /**
     * [desired] if free; otherwise `name_1.ext`, `name_2.ext`... A file that isn't ours is
     * never overwritten (spec 004, edge cases).
     */
    fun uniqueName(desired: String, existing: Set<String>): String {
        if (desired !in existing) return desired
        val dot = desired.lastIndexOf('.')
        val (stem, ext) = if (dot > 0) desired.substring(0, dot) to desired.substring(dot) else desired to ""
        var n = 1
        while ("${stem}_$n$ext" in existing) n++
        return "${stem}_$n$ext"
    }

    /** Suffix of an in-progress copy; the final name appears only once the content is complete (FR-004-09). */
    const val PARTIAL_SUFFIX = ".part"

    fun isPartial(name: String): Boolean = name.endsWith(PARTIAL_SUFFIX)

    /** Hex SHA-256: the exporter writes a transcript or note again only when this differs from the last export. */
    fun contentHash(content: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }
}
