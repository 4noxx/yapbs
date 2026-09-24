package info.rbuck.billiardscoreboard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TournamentRecordEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getById(id: String): TournamentRecordEntity?

    @Upsert
    suspend fun upsert(tournament: TournamentRecordEntity)

    @Delete
    suspend fun delete(tournament: TournamentRecordEntity)

    @Query("DELETE FROM tournaments WHERE finished = 1 AND createdAt < :cutoff")
    suspend fun deleteFinishedOlderThan(cutoff: Long)

    @Query("DELETE FROM tournaments WHERE finished = 1")
    suspend fun deleteAllFinished()
}
