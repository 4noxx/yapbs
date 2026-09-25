package info.rbuck.billiardscoreboard.domain.training

/**
 * Pure functions over [TrainingMatchState]. Same shape as StraightMatchEngine: the action log
 * is the only stored state, everything else (attempts, ball count, stats) is re-derived by
 * folding the log. Undo is therefore just "drop the last action".
 */
object TrainingMatchEngine {

    private const val FULL_RACK = 15

    private data class Derived(
        val completed: List<TrainingAttempt>,
        val current: TrainingAttempt,
        val ballsOnTable: Int,
    )

    private fun derive(state: TrainingMatchState): Derived {
        val exercise = state.exercise
        val completed = mutableListOf<TrainingAttempt>()
        var current = TrainingAttempt(index = 0)
        var balls = FULL_RACK

        fun endAttempt() {
            completed.add(current.copy(ended = true))
            current = TrainingAttempt(index = current.index + 1)
            balls = FULL_RACK
        }

        for (action in state.actions) {
            when (action) {
                is TrainingAction.Score -> {
                    val newCount = (current.ballCount + action.diff).coerceIn(0, exercise.ballsPerAttempt)
                    val applied = newCount - current.ballCount
                    current = current.copy(ballCount = newCount)
                    balls -= applied
                    if (balls <= 1) balls = FULL_RACK
                }

                is TrainingAction.Rack -> {
                    if (action.forceTo14) {
                        current = current.copy(ballCount = 14.coerceAtMost(exercise.ballsPerAttempt))
                    }
                    balls = FULL_RACK
                }

                TrainingAction.Error -> {
                    val lives = current.livesLost + 1
                    current = current.copy(livesLost = lives)
                    if (lives > exercise.livesPerAttempt) endAttempt()
                }

                TrainingAction.EndAttempt -> endAttempt()
            }
        }
        return Derived(completed, current, balls)
    }

    fun completedAttempts(state: TrainingMatchState): List<TrainingAttempt> = derive(state).completed

    fun currentAttempt(state: TrainingMatchState): TrainingAttempt = derive(state).current

    fun ballsOnTable(state: TrainingMatchState): Int = derive(state).ballsOnTable

    fun currentRun(state: TrainingMatchState): Int = derive(state).current.ballCount

    fun livesRemaining(state: TrainingMatchState): Int =
        (state.exercise.livesPerAttempt - derive(state).current.livesLost).coerceAtLeast(0)

    fun isSessionOver(state: TrainingMatchState): Boolean {
        val attemptCount = state.exercise.attemptCount ?: return false
        return derive(state).completed.size >= attemptCount
    }

    /** +N for balls potted, -N to correct a mis-tap. Clamped to what's on the table and the exercise's per-attempt cap. */
    fun addBalls(state: TrainingMatchState, diff: Int): TrainingMatchState {
        if (diff == 0 || isSessionOver(state)) return state
        val derived = derive(state)
        val maxAdd = minOf(derived.ballsOnTable, state.exercise.ballsPerAttempt - derived.current.ballCount)
        val bounded = diff.coerceIn(-derived.current.ballCount, maxAdd)
        if (bounded == 0) return state
        return state.copy(actions = state.actions + TrainingAction.Score(bounded))
    }

    /** Mid-attempt re-rack (triangle button): declares how many balls are left on the table (0 or 1, or all of
     * them for an Equal Offense "transfer everything" rack) and credits the balls potted since the last count -
     * same as tapping "+" that many times. [addBalls]'s own <=1 refill logic resets the table back to a full 15. */
    fun setBallsOnTable(state: TrainingMatchState, newCount: Int): TrainingMatchState {
        val diff = ballsOnTable(state) - newCount
        return addBalls(state, diff)
    }

    /** Level 4 only: sets the current attempt straight to 14 (full run, no breakball bonus yet). */
    fun forceBreakballRack(state: TrainingMatchState): TrainingMatchState {
        if (isSessionOver(state)) return state
        return state.copy(actions = state.actions + TrainingAction.Rack(forceTo14 = true))
    }

    /** Records a miss/foul. Ends the attempt once the exercise's allowed-errors count is exceeded. */
    fun recordError(state: TrainingMatchState): TrainingMatchState {
        if (isSessionOver(state)) return state
        return state.copy(actions = state.actions + TrainingAction.Error)
    }

    /** Player voluntarily ends the current attempt (checkmark). */
    fun endAttempt(state: TrainingMatchState): TrainingMatchState {
        if (isSessionOver(state)) return state
        return state.copy(actions = state.actions + TrainingAction.EndAttempt)
    }

    fun undo(state: TrainingMatchState): TrainingMatchState {
        if (state.actions.isEmpty()) return state
        return state.copy(actions = state.actions.dropLast(1))
    }

    fun canUndo(state: TrainingMatchState): Boolean = state.actions.isNotEmpty()

    fun stats(state: TrainingMatchState): TrainingStats {
        val derived = derive(state)
        val total = derived.completed.sumOf { it.ballCount }
        val attempts = derived.completed.size
        return TrainingStats(
            totalPoints = total,
            attemptsCompleted = attempts,
            average = if (attempts > 0) total.toFloat() / attempts else 0f,
            highRun = derived.completed.maxOfOrNull { it.ballCount } ?: 0,
            innings = attempts,
        )
    }

    /** Level 4 displays counts above 14 as "14+1" .. "14+6" instead of "15" .. "20". */
    fun formatBallCount(exercise: TrainingExercise, ballCount: Int): String =
        if (exercise.breakballBonus && ballCount > 14) "14+${ballCount - 14}" else ballCount.toString()
}
