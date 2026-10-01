package info.rbuck.billiardscoreboard.domain.onepocket

/**
 * Pure functions over [OnePocketMatchState] - as with the Simple and Straight engines, the action
 * log is the only stored state; score, whose turn it is, and the foul streak are all re-derived by
 * folding the log, so undo is just "drop the last action".
 */
object OnePocketMatchEngine {

    const val NO_WINNER = -1

    /** A foul costs one ball: a previously scored ball is spotted back (or, if the player hasn't
     * scored any yet, they simply owe one) - floored at 0 rather than tracked negative, since a
     * digital scoreboard has no use for a more exact "owed" count. */
    private const val FOUL_PENALTY = 1

    /** Official rule: three consecutive fouls by the same player forfeits the game. */
    private const val FOULS_TO_FORFEIT = 3

    private data class Derived(
        val scores: IntArray,
        val foulStreaks: IntArray,
        val currentPlayer: Int,
        /** Index of the player who just forfeited via three consecutive fouls, or -1. */
        val forfeitedBy: Int,
    )

    private fun derive(state: OnePocketMatchState): Derived {
        val scores = intArrayOf(0, 0)
        val streaks = intArrayOf(0, 0)
        var current = state.settings.firstBreakPlayer
        var forfeitedBy = -1
        for (action in state.actions) {
            when (action) {
                is OnePocketAction.Pocket -> {
                    scores[current] += action.count
                    streaks[current] = 0
                }
                is OnePocketAction.Foul -> {
                    scores[current] = (scores[current] - FOUL_PENALTY).coerceAtLeast(0)
                    streaks[current] += 1
                    if (streaks[current] >= FOULS_TO_FORFEIT && forfeitedBy == -1) forfeitedBy = current
                    current = 1 - current
                }
                is OnePocketAction.EndTurn -> {
                    streaks[current] = 0
                    current = 1 - current
                }
            }
        }
        return Derived(scores, streaks, current, forfeitedBy)
    }

    fun score(state: OnePocketMatchState, playerIndex: Int): Int = derive(state).scores[playerIndex]

    fun foulStreak(state: OnePocketMatchState, playerIndex: Int): Int = derive(state).foulStreaks[playerIndex]

    fun currentPlayerIndex(state: OnePocketMatchState): Int = derive(state).currentPlayer

    fun winnerIndex(state: OnePocketMatchState): Int {
        val d = derive(state)
        if (d.forfeitedBy != -1) return 1 - d.forfeitedBy
        val raceTo = state.settings.raceTo
        return when {
            d.scores[0] >= raceTo -> 0
            d.scores[1] >= raceTo -> 1
            else -> NO_WINNER
        }
    }

    fun isOver(state: OnePocketMatchState): Boolean = winnerIndex(state) != NO_WINNER

    /** True if the *next* foul for [playerIndex] (assuming it's their shot) would be their 3rd in a row. */
    fun wouldBeThirdFoul(state: OnePocketMatchState, playerIndex: Int): Boolean {
        val d = derive(state)
        return d.currentPlayer == playerIndex && d.foulStreaks[playerIndex] >= FOULS_TO_FORFEIT - 1
    }

    fun addPocket(state: OnePocketMatchState, count: Int = 1): OnePocketMatchState =
        if (isOver(state) || count <= 0) state else state.copy(actions = state.actions + OnePocketAction.Pocket(count))

    fun addFoul(state: OnePocketMatchState): OnePocketMatchState =
        if (isOver(state)) state else state.copy(actions = state.actions + OnePocketAction.Foul)

    fun endTurn(state: OnePocketMatchState): OnePocketMatchState =
        if (isOver(state)) state else state.copy(actions = state.actions + OnePocketAction.EndTurn)

    fun canUndo(state: OnePocketMatchState): Boolean = state.actions.isNotEmpty()

    fun undo(state: OnePocketMatchState): OnePocketMatchState =
        if (canUndo(state)) state.copy(actions = state.actions.dropLast(1)) else state
}
