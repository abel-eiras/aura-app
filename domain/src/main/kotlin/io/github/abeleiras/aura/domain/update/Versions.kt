package io.github.abeleiras.aura.domain.update

/** Semantic version `MAJOR.MINOR.PATCH[-prerelease]` (spec 006). */
data class Version(val major: Int, val minor: Int, val patch: Int, val preRelease: String? = null) :
    Comparable<Version> {

    val isPreRelease: Boolean get() = preRelease != null

    /** Monotonic `versionCode`: `MAJOR*10000 + MINOR*100 + PATCH` (FR-006-03). */
    val versionCode: Int
        get() {
            require(minor in 0..99 && patch in 0..99) { "minor and patch must be 0..99" }
            return major * 10_000 + minor * 100 + patch
        }

    override fun compareTo(other: Version): Int {
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch }).let { if (it != 0) return it }
        return when {
            preRelease == other.preRelease -> 0
            preRelease == null -> 1 // a release is newer than its own pre-releases
            other.preRelease == null -> -1
            else -> comparePreRelease(preRelease, other.preRelease)
        }
    }

    override fun toString(): String = "$major.$minor.$patch" + (preRelease?.let { "-$it" } ?: "")

    companion object {
        private val pattern = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?$")

        fun parseOrNull(text: String): Version? {
            val m = pattern.matchEntire(text.trim()) ?: return null
            val (major, minor, patch) = m.groupValues.slice(1..3).map { it.toIntOrNull() ?: return null }
            return Version(major, minor, patch, m.groupValues[4].ifEmpty { null })
        }

        fun parse(text: String): Version = requireNotNull(parseOrNull(text)) { "Not a semver: '$text'" }

        // Dot-separated identifiers: numeric ones compare as numbers and sort below alphanumeric ones.
        private fun comparePreRelease(a: String, b: String): Int {
            val x = a.split('.')
            val y = b.split('.')
            for (i in 0 until minOf(x.size, y.size)) {
                val xn = x[i].toIntOrNull()
                val yn = y[i].toIntOrNull()
                val c = when {
                    xn != null && yn != null -> xn.compareTo(yn)
                    xn != null -> -1
                    yn != null -> 1
                    else -> x[i].compareTo(y[i])
                }
                if (c != 0) return c
            }
            return x.size.compareTo(y.size)
        }
    }
}
