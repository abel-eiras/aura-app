package io.github.abeleiras.aura.domain.recording

import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioFormatsTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    // FR-009-02
    @Test
    fun `the extension comes from the name when supported, else from the mime type`() {
        assertEquals("opus", AudioFormats.extensionFor("PTT-20261006-WA0001.opus", null))
        assertEquals("m4a", AudioFormats.extensionFor("Nota de voz 12.M4A", "audio/mp4"))
        assertEquals("mp3", AudioFormats.extensionFor("sin_extension", "audio/mpeg"))
        assertEquals("ogg", AudioFormats.extensionFor("audio", "audio/ogg; codecs=opus"))
        assertEquals("wav", AudioFormats.extensionFor("grabacion.bin", "audio/x-wav"))
    }

    @Test
    fun `unsupported formats are rejected`() {
        assertNull(AudioFormats.extensionFor("clip.mp4", "video/mp4"))
        assertNull(AudioFormats.extensionFor("documento.pdf", "application/pdf"))
        assertNull(AudioFormats.extensionFor(null, null))
        assertFalse(AudioFormats.isSupported("amr"))
        assertTrue(AudioFormats.isSupported("FLAC"))
    }

    @Test
    fun `each supported extension has a provider mime type`() {
        assertEquals("audio/ogg", AudioFormats.mimeType("opus"))
        assertEquals("audio/mp3", AudioFormats.mimeType("mp3"))
        assertEquals("audio/aac", AudioFormats.mimeType("m4a"))
        AudioFormats.supportedExtensions.forEach { assertTrue(AudioFormats.mimeType(it).startsWith("audio/")) }
    }

    // FR-009-03, edge case: two audios with the same date
    @Test
    fun `imported names follow the recording pattern and never collide`() {
        val t0 = java.time.LocalDateTime.of(2026, 10, 6, 10, 15, 0).atZone(madrid).toInstant().toEpochMilli()
        val (first, firstAt) = RecordingNaming.uniqueFileName(t0, "m4a", emptySet(), madrid)
        assertEquals("aura_20261006_101500.m4a", first)
        assertEquals(t0, firstAt)
        val (second, secondAt) = RecordingNaming.uniqueFileName(t0, "m4a", setOf(first), madrid)
        assertEquals("aura_20261006_101501.m4a", second)
        assertEquals(t0 + 1000, secondAt)
        // a different extension at the same second is a different file
        assertEquals("aura_20261006_101500.mp3", RecordingNaming.uniqueFileName(t0, "mp3", setOf(first), madrid).first)
    }

    @Test
    fun `only supported audio with the recording pattern is a recording file`() {
        assertTrue(RecordingNaming.isRecordingFile("aura_20261006_101500.ogg"))
        assertTrue(RecordingNaming.isRecordingFile("aura_20261006_101500.m4a"))
        assertFalse(RecordingNaming.isRecordingFile("aura_20261006_101500.ogg.part"))
        assertFalse(RecordingNaming.isRecordingFile("aura_20261006_101500.txt"))
        assertFalse(RecordingNaming.isRecordingFile("nota.ogg"))
    }
}
