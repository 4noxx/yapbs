package info.rbuck.billiardscoreboard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE active = 1 ORDER BY name COLLATE NOCASE")
    fun observeActivePlayers(): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getById(id: String): Player?

    @Upsert
    suspend fun upsert(player: Player)

    @Delete
    suspend fun delete(player: Player)
}
