package io.github.abeleiras.aura.domain.secrets

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SecretBoxTest {
    private fun newKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val secret = "AIzaSyExampleExampleExampleExample12345".toByteArray()

    // FR-002-05
    @Test
    fun `round trip`() {
        val box = SecretBox(newKey())
        assertContentEquals(secret, box.open(box.seal(secret)))
    }

    @Test
    fun `blob does not contain the plaintext and differs between seals`() {
        val box = SecretBox(newKey())
        val a = box.seal(secret)
        val b = box.seal(secret)
        assertFalse(a.contentEquals(b))
        assertFalse(String(a, Charsets.ISO_8859_1).contains("AIza"))
    }

    @Test
    fun `tampered blob opens as null`() {
        val box = SecretBox(newKey())
        val blob = box.seal(secret)
        blob[blob.size - 1] = (blob.last().toInt() xor 1).toByte()
        assertNull(box.open(blob))
    }

    @Test
    fun `other key opens as null`() {
        val blob = SecretBox(newKey()).seal(secret)
        assertNull(SecretBox(newKey()).open(blob))
    }

    @Test
    fun `too short blob opens as null`() {
        assertNull(SecretBox(newKey()).open(ByteArray(5)))
    }
}
