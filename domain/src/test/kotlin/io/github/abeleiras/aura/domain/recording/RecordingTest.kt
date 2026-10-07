package io.github.abeleiras.aura.domain.recording

import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordingTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    // 2026-10-06 10:15:00 Madrid (UTC+2)
    private val t0 = 1_791_274_500_000L

    @Test
    fun `FR-001-08 file name uses local start time`() {
        assertEquals("aura_20261006_101500.ogg", RecordingNaming.fileName(t0, madrid))
    }

    @Test
    fun `FR-001-08 file name round-trips and rejects foreign names`() {
        assertEquals(t0, RecordingNaming.startedAtMillis("aura_20261006_101500.ogg", madrid))
        assertNull(RecordingNaming.startedAtMillis("aura_20261006_101500.wav", madrid))
        assertNull(RecordingNaming.startedAtMillis("nota.ogg", madrid))
        assertNull(RecordingNaming.startedAtMillis("aura_20269999_999999.ogg", madrid))
        assertEquals("2026-10-06", RecordingNaming.date(t0, madrid))
    }

    @Test
    fun `FR-001-04 two qualities, normal by default`() {
        assertEquals(AudioQuality.NORMAL, AudioQuality.DEFAULT)
        assertEquals(32_000, AudioQuality.NORMAL.bitRate)
        assertEquals(64_000, AudioQuality.HIGH.bitRate)
        assertEquals(AudioQuality.HIGH, AudioQuality.fromNameOrDefault("HIGH"))
        assertEquals(AudioQuality.NORMAL, AudioQuality.fromNameOrDefault("LOSSLESS"))
        assertEquals(AudioQuality.NORMAL, AudioQuality.fromNameOrDefault(null))
    }

    @Test
    fun `CE-001-3 an hour at normal quality fits in 20 MB`() {
        val bytesPerHour = AudioQuality.NORMAL.bitRate / 8L * 3600
        assertTrue(bytesPerHour <= 20L * 1024 * 1024, "was $bytesPerHour")
    }

    @Test
    fun `FR-001-05 pause and resume exclude paused time from the elapsed time`() {
        var s: RecordingStatus = RecordingStatus.Idle
        s = RecordingTransitions.start(s, 1_000)
        assertEquals(4_000, s.elapsedMillis(5_000))
        s = RecordingTransitions.pause(s, 5_000)
        assertEquals(4_000, s.elapsedMillis(60_000)) // frozen while paused
        s = RecordingTransitions.resume(s, 65_000)
        assertEquals(RecordingStatus.Recording(1_000, accumulatedPausedMillis = 60_000), s)
        assertEquals(14_000, s.elapsedMillis(75_000))
        s = RecordingTransitions.pause(s, 75_000)
        s = RecordingTransitions.resume(s, 80_000)
        assertEquals(65_000, (s as RecordingStatus.Recording).accumulatedPausedMillis)
    }

    @Test
    fun `invalid transitions are ignored`() {
        val idle = RecordingStatus.Idle
        assertEquals(idle, RecordingTransitions.pause(idle, 1))
        assertEquals(idle, RecordingTransitions.resume(idle, 1))
        val rec = RecordingTransitions.start(idle, 10)
        assertEquals(rec, RecordingTransitions.start(rec, 99))
        assertEquals(rec, RecordingTransitions.resume(rec, 99))
        assertEquals(idle, RecordingTransitions.stop(rec))
        assertFalse(idle.isActive)
        assertEquals(0, idle.elapsedMillis(99))
    }

    @Test
    fun `FR-001-11 recordings under one second are discarded`() {
        assertTrue(RecordingPolicy.isTooShort(999))
        assertFalse(RecordingPolicy.isTooShort(1_000))
    }

    @Test
    fun `FR-001-12 low storage threshold`() {
        assertTrue(RecordingPolicy.isStorageLow(50L * 1024 * 1024))
        assertFalse(RecordingPolicy.isStorageLow(RecordingPolicy.LOW_STORAGE_BYTES))
    }

    @Test
    fun `archive size warning above 2 GB`() {
        assertFalse(RecordingPolicy.isArchiveLarge(2L * 1024 * 1024 * 1024))
        assertTrue(RecordingPolicy.isArchiveLarge(2L * 1024 * 1024 * 1024 + 1))
    }

    @Test
    fun `duration formatting`() {
        assertEquals("0:00", RecordingPolicy.formatDuration(0))
        assertEquals("1:05", RecordingPolicy.formatDuration(65_400))
        assertEquals("1:01:01", RecordingPolicy.formatDuration(3_661_000))
        assertEquals("0:00", RecordingPolicy.formatDuration(-5))
    }
}
