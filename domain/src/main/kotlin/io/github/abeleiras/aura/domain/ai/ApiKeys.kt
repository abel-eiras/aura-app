package io.github.abeleiras.aura.domain.ai

object ApiKeys {
    private val geminiKey = Regex("^AIza[0-9A-Za-z_\\-]{35}$")

    /**
     * A Google AI Studio key found in [text] (e.g. the clipboard after the user copied it), or null.
     * Strict on purpose: it is used to offer "paste" automatically, so it must not match random text.
     * Whatever the user types by hand is accepted and then checked against the API (FR-002-03).
     */
    fun geminiKeyOrNull(text: String?): String? = text?.trim()?.takeIf { geminiKey.matches(it) }
}
