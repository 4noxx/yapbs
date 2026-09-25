package info.rbuck.billiardscoreboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clubs")
data class Club(
    @PrimaryKey val id: String,
    val name: String,
    /** Absolute path to a copy of the crest image in app-internal storage, or null if none set. */
    val crestPath: String? = null,
)
