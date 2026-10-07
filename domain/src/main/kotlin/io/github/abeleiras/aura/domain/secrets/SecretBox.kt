package io.github.abeleiras.aura.domain.secrets

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM sealing of small secrets such as API keys (FR-002-05). The key is supplied by the caller:
 * on Android it is a non-exportable Android Keystore key; tests use a software key.
 *
 * The IV is never chosen here: Keystore keys refuse caller-provided IVs, and letting the cipher generate a
 * random one is also what makes two seals of the same secret differ. Blob = `iv (12 bytes) || ciphertext+tag`.
 */
class SecretBox(private val key: SecretKey) {

    fun seal(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        check(iv.size == IV_BYTES) { "Unexpected IV size ${iv.size}" }
        return iv + cipher.doFinal(plain)
    }

    /** The secret, or null if the blob is corrupt, was tampered with, or was sealed with another key. */
    fun open(blob: ByteArray): ByteArray? {
        if (blob.size <= IV_BYTES) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, blob.copyOfRange(0, IV_BYTES)))
            cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES)
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
