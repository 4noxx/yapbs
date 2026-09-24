package info.rbuck.billiardscoreboard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ClubDao {
    @Query("SELECT * FROM clubs ORDER BY name COLLATE NOCASE")
    fun observeClubs(): Flow<List<Club>>

    @Query("SELECT * FROM clubs WHERE id = :id")
    suspend fun getById(id: String): Club?

    @Upsert
    suspend fun upsert(club: Club)

    @Delete
    suspend fun delete(club: Club)
}
