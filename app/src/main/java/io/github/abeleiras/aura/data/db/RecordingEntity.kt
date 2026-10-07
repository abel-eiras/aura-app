package io.github.abeleiras.aura.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A finished recording. The audio lives in `filesDir/recordings/<fileName>` (FR-001-07). */
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val fileName: String,
    val startedAtMillis: Long,
    val durationMillis: Long,
    val sizeBytes: Long,
    val quality: String,
)
