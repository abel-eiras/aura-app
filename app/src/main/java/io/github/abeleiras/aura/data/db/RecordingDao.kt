package io.github.abeleiras.aura.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY startedAtMillis DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE fileName = :fileName")
    suspend fun delete(fileName: String)

    @Query("SELECT fileName FROM recordings")
    suspend fun fileNames(): List<String>
}
