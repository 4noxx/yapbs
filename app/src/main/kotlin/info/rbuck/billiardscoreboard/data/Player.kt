package info.rbuck.billiardscoreboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class Player(
    @PrimaryKey val id: String,
    val name: String,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val clubId: String? = null,
)
