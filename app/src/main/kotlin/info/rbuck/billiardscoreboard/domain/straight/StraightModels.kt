package info.rbuck.billiardscoreboard.domain.straight

import info.rbuck.billiardscoreboard.domain.Handicap
import kotlinx.serialization.Serializable

enum class InningEndType { NONE, FOUL, MISS, SAFE }

@Serializable
sealed class StraightAction {
    /** Balls potted (+) or a correction (-) during the current inning. Auto re-racks when the table empties. */
    @Serializable
    data class Score(val diff: Int) : StraightAction()

    /**
     * A foul. [breakFoul] = a break foul on the opening break (or a mandatory re-break): flat 2-pt
     * penalty, never counts toward "three fouls in a row" (rule 7.11(1)).
     * [reRack] combines with [breakFoul]:
     *  - breakFoul && reRack  -> opponent required a re-break (7.3(2)b): -2, rack refilled, the
     *    same player stays on for another opening break, the inning does NOT end.
     *  - !breakFoul && reRack -> third consecutive foul (7.11): -1 then -15, rack refilled, the
     *    inning ends, and the SAME player must re-break.
     *  - reRack == false      -> ordinary foul (or opponent accepting the table after a break
     *    foul): penalty applied, inning ends, turn passes to the opponent.
     */
    @Serializable
    data class Foul(val breakFoul: Boolean, val reRack: Boolean) : StraightAction()

    /** Ends the current inning without a foul (miss, safety, or a plain manual switch). */
    @Serializable
    data class EndTurn(val endType: InningEndType) : StraightAction()

    /** Manual re-rack mid-inning ("no ball left") - does not end the turn. */
    @Serializable
    data object Rack : StraightAction()
}

@Serializable
data class StraightSettings(
    val raceTo: Int,
    val maxInnings: Int? = null,
    val handicap: Handicap? = null,
    val firstPlayerIndex: Int = 0,
)

@Serializable
data class StraightMatchState(
    val id: String,
    val settings: StraightSettings,
    val playerIds: List<String>,
    val actions: List<StraightAction> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

/** One inning (turn) of play, derived from folding the action log - never persisted directly. */
data class StraightRecord(
    val playerIndex: Int,
    val ballCount: Int = 0,
    val foulPenalty: Int = 0,
    val breakFoulCount: Int = 0,
    val fullRackCount: Int = 0,
    val inningEndType: InningEndType = InningEndType.NONE,
    /** This turn is an opening break (game start, or a mandatory re-break after a three-foul
     * re-rack, or still awaiting a valid break after an opponent-required re-break) - the only
     * time a break foul can be committed. */
    val openingBreakShot: Boolean = false,
) {
    val net: Int get() = ballCount - foulPenalty
}

data class PlayerStats(
    val totalPoints: Int,
    val innings: Int,
    val average: Float,
    val averageWithoutSafe: Float,
    val highestBreak: Int,
    val fouls: Int,
    val misses: Int,
    val safes: Int,
)
