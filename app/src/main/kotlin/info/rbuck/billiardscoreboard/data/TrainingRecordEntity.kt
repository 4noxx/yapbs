package info.rbuck.billiardscoreboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One completed solo training session. Only written once the exercise was actually played through
 * (all innings finished, or - for the open-ended 14.1 HighRun - the player chose "Finish"), and
 * only while Settings > Training > "Save training to history" is on.
 *
 * Training is solo, so there is only one [playerId] and no winner column. [inningScoresJson] holds
 * the per-inning ball counts as a JSON int array, for the history detail breakdown; the other
 * columns are a denormalized cache for fast list rendering - mirrors [MatchRecordEntity]'s split.
 */
@Entity(tableName = "training_sessions")
data class TrainingRecordEntity(
    @PrimaryKey val id: String,
    val exercise: String,
    val createdAt: Long,
    val playerId: String,
    val totalPoints: Int,
    val average: Float,
    val highRun: Int,
    val innings: Int,
    /** Short display string for the list row, e.g. "96 / 120" or "HR 42". */
    val summary: String,
    /** JSON int array of each inning's ball count, in order. */
    val inningScoresJson: String,
)
