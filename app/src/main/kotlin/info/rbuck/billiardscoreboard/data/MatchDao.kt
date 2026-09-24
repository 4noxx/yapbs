package info.rbuck.billiardscoreboard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches WHERE archived = 1 ORDER BY createdAt DESC")
    fun observeArchived(): Flow<List<MatchRecordEntity>>

    /** Every finished match between exactly these two players, oldest first - for the head-to-head
     * screen. Either player can be in either slot (a rematch swaps them). */
    @Query(
        "SELECT * FROM matches WHERE finished = 1 AND " +
            "((player1Id = :a AND player2Id = :b) OR (player1Id = :b AND player2Id = :a)) " +
            "ORDER BY createdAt ASC",
    )
    fun observeFinishedBetween(a: String, b: String): Flow<List<MatchRecordEntity>>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getById(id: String): MatchRecordEntity?

    @Upsert
    suspend fun upsert(match: MatchRecordEntity)

    @Delete
    suspend fun delete(match: MatchRecordEntity)

    /** Retention prune - only finished, archived matches; resumable ones are never touched. */
    @Query("DELETE FROM matches WHERE archived = 1 AND createdAt < :cutoff")
    suspend fun deleteArchivedOlderThan(cutoff: Long)

    @Query("DELETE FROM matches WHERE archived = 1")
    suspend fun deleteAllArchived()
}
