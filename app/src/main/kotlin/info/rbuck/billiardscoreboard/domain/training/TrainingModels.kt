package info.rbuck.billiardscoreboard.domain.training

/** The 5 solo practice drills offered under Training. All are 14.1-based. */
enum class TrainingExercise(
    val displayName: String,
    /** Number of attempts (racks) in a session; null = open-ended (HighRun). */
    val attemptCount: Int?,
    /** Cap on balls counted within one attempt. */
    val ballsPerAttempt: Int,
    /** Errors allowed per attempt before it ends; unused for HighRun. */
    val livesPerAttempt: Int,
    /** Session points target shown for comparison; null for HighRun. */
    val targetPoints: Int?,
    /** Reference average (GD) shown for comparison; null for HighRun. */
    val referenceAverage: Float?,
    /** Level 4 only: the rack button sets the count straight to 14, and counts above 14 display as "14+N". */
    val breakballBonus: Boolean,
    /** Shown in the rules info dialog on the score screen. */
    val ruleDescription: String,
) {
    HIGH_RUN(
        "14.1 HighRun",
        null,
        Int.MAX_VALUE,
        Int.MAX_VALUE,
        null,
        null,
        false,
        "Play continuous 14.1: rack after rack, with no inning limit and no lives. Pot as many balls in a " +
            "row as you can - the goal is simply your longest run. Track your current run and your session's " +
            "high run at the bottom of the card.",
    ),
    EQUAL_OFFENSE_1(
        "Equal Offense Level 1",
        10,
        15,
        2,
        120,
        4.0f,
        false,
        "Setup: rack all 15 balls, then break from the kitchen - the break shot doesn't count as a normal " +
            "shot, and any balls it pots are re-spotted on the foot spot, so all 15 balls are always back on " +
            "the table after the break. You start your attempt with ball-in-hand anywhere on the table. Equal " +
            "Offense is a call-shot game (call ball and pocket, except for obvious shots).\n\n" +
            "10 innings, one rack of 15 balls each. You get 2 lives per inning - the inning only ends early once " +
            "you've missed 3 times (2 lives lost + the miss that ends it). After a miss or foul you keep " +
            "playing from the cue ball's current position (unless it's potted or leaves the table - then " +
            "ball-in-hand anywhere again). Aim for a session total of 120 points or more - a 4.0 average per " +
            "inning is the reference to beat.",
    ),
    EQUAL_OFFENSE_2(
        "Equal Offense Level 2",
        10,
        15,
        1,
        120,
        6.0f,
        false,
        "Setup: rack all 15 balls, then break from the kitchen - the break shot doesn't count as a normal " +
            "shot, and any balls it pots are re-spotted on the foot spot, so all 15 balls are always back on " +
            "the table after the break. You start your attempt with ball-in-hand anywhere on the table. Equal " +
            "Offense is a call-shot game (call ball and pocket, except for obvious shots).\n\n" +
            "10 innings, one rack of 15 balls each. You get 1 life per inning - the inning ends after your " +
            "second miss or foul. After a miss or foul you keep playing from the cue ball's current position " +
            "(unless it's potted or leaves the table - then ball-in-hand anywhere again). Aim for a session " +
            "total of 120 points or more - a 6.0 average per inning is the reference to beat.",
    ),
    EQUAL_OFFENSE_3(
        "Equal Offense Level 3",
        10,
        15,
        0,
        120,
        12.0f,
        false,
        "Setup: rack all 15 balls, then break from the kitchen - the break shot doesn't count as a normal " +
            "shot, and any balls it pots are re-spotted on the foot spot, so all 15 balls are always back on " +
            "the table after the break. You start your attempt with ball-in-hand anywhere on the table. Equal " +
            "Offense is a call-shot game (call ball and pocket, except for obvious shots).\n\n" +
            "10 innings, one rack of 15 balls each. No lives - a single miss or foul ends the inning " +
            "immediately. After a miss or foul you keep playing from the cue ball's current position (unless " +
            "it's potted or leaves the table - then ball-in-hand anywhere again). Aim for a session total of " +
            "120 points or more - a 12.0 average per inning is the reference to beat.",
    ),
    EQUAL_OFFENSE_4(
        "Equal Offense Level 4",
        10,
        20,
        0,
        170,
        17.0f,
        true,
        "Setup: same as the other levels - rack all 15 balls, then break from the kitchen; any balls the break " +
            "pots are re-spotted on the foot spot, so all 15 are always back on the table after it. The only " +
            "difference from the other levels: you may only place the cue ball within the kitchen, not anywhere " +
            "on the table. Equal Offense stays a call-shot game (call ball and pocket, except for obvious shots).\n\n" +
            "10 innings, no lives - a single miss or foul ends the inning immediately. Once you reach 14 balls, " +
            "the rack button racks the remaining ball as a breakball instead of re-spotting all 15, and your " +
            "count continues past 14 (\"14+1\", \"14+2\", ...) up to 20 balls per inning. Aim for a session " +
            "total of 170 points or more - a 17.0 average per inning is the reference to beat.",
    ),
    ;

    val isOpenEnded: Boolean get() = this == HIGH_RUN
    val hasLives: Boolean get() = this != HIGH_RUN && livesPerAttempt > 0
    val showsAttemptGrid: Boolean get() = attemptCount != null
}

sealed class TrainingAction {
    /** Balls potted (+) or a correction (-) during the current attempt. */
    data class Score(val diff: Int) : TrainingAction()

    /** Mid-attempt re-rack. [forceTo14] (Level 4 only) sets the attempt's count straight to 14. */
    data class Rack(val forceTo14: Boolean = false) : TrainingAction()

    /** One error/foul/miss. Ends the attempt once the exercise's allowed-errors count is exceeded. */
    data object Error : TrainingAction()

    /** Player voluntarily ends the current attempt (checkmark). */
    data object EndAttempt : TrainingAction()
}

data class TrainingMatchState(
    val id: String,
    val exercise: TrainingExercise,
    val playerId: String,
    val actions: List<TrainingAction> = emptyList(),
)

/** One attempt (rack), derived from folding the action log - never persisted directly. */
data class TrainingAttempt(
    val index: Int,
    val ballCount: Int = 0,
    val livesLost: Int = 0,
    val ended: Boolean = false,
)

data class TrainingStats(
    val totalPoints: Int,
    val attemptsCompleted: Int,
    val average: Float,
    val highRun: Int,
    val innings: Int,
)
