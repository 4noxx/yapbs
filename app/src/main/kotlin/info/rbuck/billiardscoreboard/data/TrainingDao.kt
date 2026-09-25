package info.rbuck.billiardscoreboard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDao {
    @Query("SELECT * FROM training_sessions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TrainingRecordEntity>>

    @Query("SELECT * FROM training_sessions WHERE id = :id")
    suspend fun getById(id: String): TrainingRecordEntity?

    @Upsert
    suspend fun upsert(session: TrainingRecordEntity)

    @Delete
    suspend fun delete(session: TrainingRecordEntity)

    @Query("DELETE FROM training_sessions WHERE createdAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM training_sessions")
    suspend fun deleteAll()
}
