package io.github.abeleiras.aura.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

// exportSchema stays off until the first public release; from then on every schema
// change needs a migration and exported schemas (see specs/001-grabacion/plan.md).
@Database(entities = [RecordingEntity::class], version = 1, exportSchema = false)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}
