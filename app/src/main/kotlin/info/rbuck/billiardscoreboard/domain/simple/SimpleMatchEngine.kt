package info.rbuck.billiardscoreboard.domain.simple

/**
 * Pure functions over [SimpleMatchState]. The action log is the single source of truth;
 * score, winner and undo are all derived from (or edit) that list, so undo is just
 * "drop the last action" rather than a separate inverse operation per action type.
 */
object SimpleMatchEngine {

    const val NO_WINNER = -1
    const val DRAW = -2

    fun score(state: SimpleMatchState, playerIndex: Int, withHandicap: Boolean = true): Int {
        val raw = state.actions.count { it.playerIndex == playerIndex }
        val handicapPoints = state.settings.handicap
            ?.takeIf { withHandicap && it.playerIndex == playerIndex }
            ?.points ?: 0
        return raw + handicapPoints
    }

    fun runoutCount(state: SimpleMatchState, playerIndex: Int): Int =
        state.actions.count { it.playerIndex == playerIndex && it.isRunout }

    /** Index of the player who reached [SimpleSettings.raceTo] first, [DRAW], or [NO_WINNER]. */
    fun winnerIndex(state: SimpleMatchState): Int {
        val raceTo = state.settings.raceTo
        val reached = (0..1).filter { score(state, it) >= raceTo }
        return when {
            reached.size == 2 -> DRAW
            reached.size == 1 -> reached[0]
            else -> NO_WINNER
        }
    }

    fun isOver(state: SimpleMatchState): Boolean = winnerIndex(state) != NO_WINNER

    /** Who breaks the next (or, if the match just started, the first) rack. */
    fun nextBreakPlayer(state: SimpleMatchState): Int {
        val last = state.actions.lastOrNull() ?: return state.settings.firstBreakPlayer
        return when (state.settings.breakMode) {
            BreakMode.WINNER_BREAKS -> last.playerIndex
            BreakMode.ALTERNATE -> (state.actions.size + state.settings.firstBreakPlayer) % 2
        }
    }

    /** Records a rack win for [playerIndex]. Fails silently (returns the unchanged state) once the match is over. */
    fun addRackWin(state: SimpleMatchState, playerIndex: Int, isRunout: Boolean = false): SimpleMatchState {
        if (isOver(state)) return state
        if (score(state, playerIndex) >= state.settings.raceTo) return state
        return state.copy(actions = state.actions + SimpleAction(playerIndex, isRunout))
    }

    fun undo(state: SimpleMatchState): SimpleMatchState {
        if (state.actions.isEmpty()) return state
        return state.copy(actions = state.actions.dropLast(1))
    }

    fun canUndo(state: SimpleMatchState): Boolean = state.actions.isNotEmpty()

    /** Removes [playerIndex]'s own most recent rack win, wherever it falls in the shared log - a per-player "-1". */
    fun removeLastRackFor(state: SimpleMatchState, playerIndex: Int): SimpleMatchState {
        val lastIndex = state.actions.indexOfLast { it.playerIndex == playerIndex }
        if (lastIndex == -1) return state
        return state.copy(actions = state.actions.toMutableList().apply { removeAt(lastIndex) })
    }

    fun canRemoveLastRackFor(state: SimpleMatchState, playerIndex: Int): Boolean =
        state.actions.any { it.playerIndex == playerIndex }

    /** Maximum handicap allowed is one less than the race length (can't start already-won). */
    fun maxHandicapFor(raceTo: Int): Int = (raceTo - 1).coerceAtLeast(0)
}
