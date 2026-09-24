package info.rbuck.billiardscoreboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One tournament. [stateJson] holds the full, resumable
 * [info.rbuck.billiardscoreboard.domain.tournament.TournamentState]; the other columns
 * are a denormalized cache for fast history-list rendering without decoding it -
 * mirrors [MatchRecordEntity]'s own split for the same reason.
 */
@Entity(tableName = "tournaments")
data class TournamentRecordEntity(
    @PrimaryKey val id: String,
    val mode: String,
    val createdAt: Long,
    /** Comma-joined player IDs, in tournament order - denormalized cache only; the source of truth is stateJson. */
    val playerIds: String,
    val finished: Boolean,
    /** Null while ongoing, or if a round-robin tournament ended in a tie. For PARTNER_ROTATION
     * this is one arbitrary member of the winning team - the history list shows just that one
     * name; opening the tournament shows both (see [TournamentEngine.championTeam]). */
    val championId: String?,
    val stateJson: String,
)
