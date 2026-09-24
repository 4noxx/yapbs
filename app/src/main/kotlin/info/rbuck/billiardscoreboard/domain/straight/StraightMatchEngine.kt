package info.rbuck.billiardscoreboard.domain.straight

/**
 * Pure functions over [StraightMatchState]. As with the Simple engine, the action log is
 * the only stored state; innings, ball count, fouls and stats are all re-derived by folding
 * the log. Undo is therefore just "drop the last action".
 */
object StraightMatchEngine {

    const val NO_WINNER = -1
    const val DRAW = -2

    private const val FULL_RACK = 15
    private const val NORMAL_FOUL_PENALTY = 1
    private const val BREAK_FOUL_PENALTY = 2

    /** Official WPA rule: a player's 3rd consecutive foul without scoring costs 15 extra points. */
    private const val THREE_FOUL_EXTRA_PENALTY = 15

    private data class Derived(
        val completed: List<StraightRecord>,
        val current: StraightRecord,
        val ballsOnTable: Int,
    ) {
        val all: List<StraightRecord> get() = completed + current
    }

    /** Only a plain, scoreless *standard* foul feeds the "three fouls in a row" streak. Break
     * fouls are excluded (rule 7.11(1)), and the third-foul inning itself (which already carries
     * the +15 penalty) resets the streak (7.11(2)) so it can't chain straight into a new one. */
    private fun countsTowardThreeFoulStreak(record: StraightRecord): Boolean =
        record.inningEndType == InningEndType.FOUL &&
            record.ballCount == 0 &&
            record.breakFoulCount == 0 &&
            record.foulPenalty < NORMAL_FOUL_PENALTY + THREE_FOUL_EXTRA_PENALTY

    private fun isThirdConsecutiveScorelessFoul(completed: List<StraightRecord>, playerIndex: Int): Boolean {
        val streak = completed.filter { it.playerIndex == playerIndex }.takeLastWhile(::countsTowardThreeFoulStreak)
        return streak.size >= 2
    }

    private fun foulPenaltyFor(completed: List<StraightRecord>, playerIndex: Int, breakFoul: Boolean): Int = when {
        // A break foul is always a flat -2 and never the "third foul" (rule 7.11(1)).
        breakFoul -> BREAK_FOUL_PENALTY
        isThirdConsecutiveScorelessFoul(completed, playerIndex) -> NORMAL_FOUL_PENALTY + THREE_FOUL_EXTRA_PENALTY
        else -> NORMAL_FOUL_PENALTY
    }

    private fun derive(state: StraightMatchState): Derived {
        val completed = mutableListOf<StraightRecord>()
        var current = StraightRecord(playerIndex = state.settings.firstPlayerIndex, openingBreakShot = true)
        var balls = FULL_RACK

        fun endInning(endType: InningEndType) {
            completed.add(current.copy(inningEndType = endType))
            current = StraightRecord(playerIndex = 1 - current.playerIndex)
        }

        for (action in state.actions) {
            when (action) {
                is StraightAction.Score -> {
                    // Any pot means a legal break happened - no more break-foul question this turn.
                    current = current.copy(ballCount = current.ballCount + action.diff, openingBreakShot = false)
                    balls -= action.diff
                    if (balls <= 1) {
                        // Ran the rack down to the last ball: it stays on the table as the break
                        // ball for the next rack, which is reloaded to a full 15.
                        balls = FULL_RACK
                        current = current.copy(fullRackCount = current.fullRackCount + 1)
                    }
                }

                is StraightAction.Foul -> {
                    val penalty = foulPenaltyFor(completed, current.playerIndex, action.breakFoul)
                    current = current.copy(
                        foulPenalty = current.foulPenalty + penalty,
                        breakFoulCount = current.breakFoulCount + if (action.breakFoul) 1 else 0,
                    )
                    if (action.reRack) balls = FULL_RACK
                    when {
                        action.breakFoul && action.reRack -> {
                            // Opponent required a re-break (7.3(2)b / 7.10(1)): the same player's
                            // opening turn continues, the -2 just accumulates - inning does NOT end.
                        }
                        action.reRack -> {
                            // Third consecutive foul (7.11): the -16 inning ends, then the SAME
                            // player must re-break (7.11(3)).
                            completed.add(current.copy(inningEndType = InningEndType.FOUL))
                            current = StraightRecord(playerIndex = current.playerIndex, openingBreakShot = true)
                        }
                        else -> endInning(InningEndType.FOUL)
                    }
                }

                is StraightAction.EndTurn -> endInning(action.endType)

                StraightAction.Rack -> {
                    val completesRun = balls <= 1
                    balls = FULL_RACK
                    if (completesRun) current = current.copy(fullRackCount = current.fullRackCount + 1)
                }
            }
        }
        return Derived(completed, current, balls)
    }

    fun records(state: StraightMatchState): List<StraightRecord> = derive(state).all

    /**
     * Every inning that has actually ended (via a foul, a miss, a safety, or a voluntary switch),
     * excluding the still-open current one. Unlike [records], every entry here is a real turn -
     * even one where the player potted nothing before switching, which still legitimately ends an
     * inning and must count for it (previously conflated with the not-yet-played current turn).
     */
    fun completedRecords(state: StraightMatchState): List<StraightRecord> = derive(state).completed

    /** The still-open inning being played right now, or null if nothing has happened in it yet. */
    fun currentRecord(state: StraightMatchState): StraightRecord? {
        val current = derive(state).current
        return if (attempted(current)) current else null
    }

    fun ballsOnTable(state: StraightMatchState): Int = derive(state).ballsOnTable

    fun currentPlayerIndex(state: StraightMatchState): Int = derive(state).current.playerIndex

    /** Balls potted so far in the current, still-open inning; 0 for the player who isn't up. */
    fun currentRun(state: StraightMatchState, playerIndex: Int): Int {
        val current = derive(state).current
        return if (current.playerIndex == playerIndex) current.ballCount else 0
    }

    fun score(state: StraightMatchState, playerIndex: Int, withHandicap: Boolean = true): Int {
        val raw = derive(state).all.filter { it.playerIndex == playerIndex }.sumOf { it.net }
        val handicapPoints = state.settings.handicap
            ?.takeIf { withHandicap && it.playerIndex == playerIndex }
            ?.points ?: 0
        return raw + handicapPoints
    }

    fun winnerIndex(state: StraightMatchState): Int {
        val raceTo = state.settings.raceTo
        if (raceTo > 0) {
            val s0 = score(state, 0)
            val s1 = score(state, 1)
            if (s0 >= raceTo || s1 >= raceTo) {
                return if (s0 >= raceTo && s1 >= raceTo) DRAW else if (s0 >= raceTo) 0 else 1
            }
        }
        val maxInnings = state.settings.maxInnings
        if (maxInnings != null) {
            // Ends when player 2 (index 1) completes an innings checkpoint (maxInnings, then +5, +10, ...)
            // unless the score is tied at that point, in which case play continues to the next checkpoint.
            val p1Turns = derive(state).completed.count { it.playerIndex == 1 }
            if (p1Turns >= maxInnings && (p1Turns - maxInnings) % 5 == 0) {
                val s0 = score(state, 0)
                val s1 = score(state, 1)
                if (s0 != s1) return if (s0 > s1) 0 else 1
            }
        }
        return NO_WINNER
    }

    fun isOver(state: StraightMatchState): Boolean = winnerIndex(state) != NO_WINNER

    /** +N for balls potted, -N to correct a mis-tap. Clamped so it can't pot more than are on the table or overshoot the race target. */
    fun addBalls(state: StraightMatchState, diff: Int): StraightMatchState {
        if (diff == 0 || isOver(state)) return state
        val derived = derive(state)
        val bounded = diff.coerceIn(-derived.current.ballCount, derived.ballsOnTable)
        if (bounded == 0) return state

        val playerIndex = derived.current.playerIndex
        val remaining = state.settings.raceTo - score(state, playerIndex)
        val finalDiff = if (state.settings.raceTo > 0 && bounded > remaining) remaining.coerceAtLeast(0) else bounded
        if (finalDiff == 0) return state
        return state.copy(actions = state.actions + StraightAction.Score(finalDiff))
    }

    fun foul(state: StraightMatchState, breakFoul: Boolean, reRack: Boolean): StraightMatchState {
        if (isOver(state)) return state
        return state.copy(actions = state.actions + StraightAction.Foul(breakFoul, reRack))
    }

    fun endTurn(state: StraightMatchState, endType: InningEndType): StraightMatchState {
        if (isOver(state)) return state
        return state.copy(actions = state.actions + StraightAction.EndTurn(endType))
    }

    fun rack(state: StraightMatchState): StraightMatchState {
        if (isOver(state)) return state
        return state.copy(actions = state.actions + StraightAction.Rack)
    }

    fun undo(state: StraightMatchState): StraightMatchState {
        if (state.actions.isEmpty()) return state
        return state.copy(actions = state.actions.dropLast(1))
    }

    fun canUndo(state: StraightMatchState): Boolean = state.actions.isNotEmpty()

    /** True while the current turn is an opening break with nothing yet potted - the only time a
     * break foul can happen. Covers the game's first shot, a mandatory re-break after a three-foul
     * re-rack (7.11(3)), and every further attempt after an opponent-required re-break (7.3(3)). */
    fun isBreakFoulPossible(state: StraightMatchState): Boolean {
        val derived = derive(state)
        return derived.current.openingBreakShot && derived.current.ballCount == 0
    }

    /** True if the *next* foul for the current player would be their 3rd consecutive scoreless one. */
    fun wouldBeThirdConsecutiveFoul(state: StraightMatchState): Boolean {
        val derived = derive(state)
        return isThirdConsecutiveScorelessFoul(derived.completed, derived.current.playerIndex)
    }

    fun maxHandicapFor(raceTo: Int): Int = (raceTo - 1).coerceAtLeast(0)

    private fun attempted(record: StraightRecord): Boolean =
        record.ballCount != 0 || record.foulPenalty != 0 || record.inningEndType != InningEndType.NONE

    fun stats(state: StraightMatchState, playerIndex: Int): PlayerStats {
        val derived = derive(state)
        // Every completed inning always counts, however it ended - including a switch-turn with
        // nothing potted. Only the still-open current inning needs the attempted() check, so the
        // not-yet-played placeholder for the upcoming turn doesn't count before anything happens in it.
        val completedForPlayer = derived.completed.filter { it.playerIndex == playerIndex }
        val currentAttempted = derived.current.playerIndex == playerIndex && attempted(derived.current)
        val forPlayer = if (currentAttempted) completedForPlayer + derived.current else completedForPlayer
        val total = forPlayer.sumOf { it.net }
        // The displayed inning count is "which inning is this player on", so it includes their
        // current turn the moment it becomes their shot - even before they've potted anything -
        // unlike forPlayer above, which only counts turns that actually had something happen.
        val innings = completedForPlayer.size + if (derived.current.playerIndex == playerIndex) 1 else 0
        val nonSafety = forPlayer.count { it.inningEndType != InningEndType.SAFE }
        return PlayerStats(
            totalPoints = total,
            innings = innings,
            average = if (forPlayer.isNotEmpty()) total.toFloat() / forPlayer.size else 0f,
            averageWithoutSafe = if (nonSafety > 0) total.toFloat() / nonSafety else total.toFloat(),
            highestBreak = forPlayer.maxOfOrNull { it.net.coerceAtLeast(0) } ?: 0,
            fouls = forPlayer.count { it.inningEndType == InningEndType.FOUL },
            misses = forPlayer.count { it.inningEndType == InningEndType.MISS },
            safes = forPlayer.count { it.inningEndType == InningEndType.SAFE },
        )
    }
}
