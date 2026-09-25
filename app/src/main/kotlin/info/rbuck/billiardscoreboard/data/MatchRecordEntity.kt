package info.rbuck.billiardscoreboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One saved match. [stateJson] holds the full, resumable [info.rbuck.billiardscoreboard.domain.MatchStatePayload];
 * the other columns are a denormalized cache for fast archive-list rendering without decoding it.
 */
@Entity(tableName = "matches")
data class MatchRecordEntity(
    @PrimaryKey val id: String,
    val gameType: String,
    val createdAt: Long,
    val player1Id: String,
    val player2Id: String,
    val finished: Boolean,
    val archived: Boolean,
    val summary: String,
    /** -1 = ongoing/no winner yet, -2 = draw, 0/1 = winning player slot. */
    val winnerIndex: Int,
    val stateJson: String,
)
