package info.rbuck.billiardscoreboard.domain.tournament

import kotlinx.serialization.Serializable

/**
 * How the table rotates between games. LOSER_STAYS/WINNER_STAYS/SUDDEN_DEATH all use a simple
 * FIFO waiting queue (SUDDEN_DEATH never re-queues a loser - they're out for good); ROUND_ROBIN
 * instead works through a fixed, precomputed schedule of every unique pair (see
 * [TournamentEngine.roundRobinSchedule]); SINGLE_ELIMINATION works through a seeded bracket,
 * recomputed round by round from the results log the same way. PARTNER_ROTATION is different
 * again - a minimum of 4 players, playing 2v2 doubles; with exactly 4, everyone plays every round
 * and their pairing cycles through the 3 possible team combinations (AB-CD, AC-BD, AD-BC) every 3
 * rounds. With more than 4, exactly 4 are active each round and the rest sit out, rotating fairly
 * so everyone waits about equally often (see [TournamentEngine.partnerRotationActivePlayers]) -
 * both members of the winning team score a point, recorded as two results per round (see
 * [TournamentEngine.recordTeamResult]).
 */
enum class TournamentMode { LOSER_STAYS, WINNER_STAYS, ROUND_ROBIN, SUDDEN_DEATH, SINGLE_ELIMINATION, PARTNER_ROTATION }

@Serializable
data class TournamentSettings(
    val mode: TournamentMode,
    /** Encounters (see [gamesPerEncounter]) needed to become champion. Used by
     * LOSER_STAYS/WINNER_STAYS/PARTNER_ROTATION only - null for every other mode, which each have
     * their own, different end condition. */
    val targetWins: Int? = null,
    /** Games needed to decide a single encounter between whoever's currently at the table - "1"
     * (the default) is today's behaviour where a single tap immediately decides it; higher values
     * make each encounter its own best-of-N mini race (see [TournamentEngine.recordEncounterGame]),
     * still worth exactly one win/point in the outer tournament once decided. Applies to every mode. */
    val gamesPerEncounter: Int = 1,
)

/** One completed rack: who won, who lost. No ball-by-ball detail - this tracks table
 * rotation and standings, not an individual game's score. */
@Serializable
data class TournamentGameResult(val winnerId: String, val loserId: String)

@Serializable
data class TournamentState(
    val id: String,
    val settings: TournamentSettings,
    /** Fixed order, randomized once when the tournament is created. For LOSER_STAYS/WINNER_STAYS
     * this is table[0] vs table[1] first, then the rest as the initial waiting queue. For
     * ROUND_ROBIN this is the input to the circle-method schedule. */
    val playerIds: List<String>,
    /** One entry per *completed* encounter (or two, for PARTNER_ROTATION - one per winning
     * teammate) - this is what [TournamentEngine.currentTable]/[TournamentEngine.currentTeams]/
     * standings/rotation/brackets are all derived from, same as before [gamesPerEncounter] existed. */
    val results: List<TournamentGameResult> = emptyList(),
    /** Games played so far in the *current*, not-yet-decided encounter - reset to empty every time
     * an encounter completes and folds into [results]. Purely a sub-tally; never affects rotation,
     * standings or brackets directly. */
    val currentEncounterGames: List<TournamentGameResult> = emptyList(),
    /** (winner's games, loser's games) for the completed encounter at the same index in [results] -
     * e.g. a 2:1 best-of-3 encounter records (2, 1) here. Only meaningful when
     * [TournamentSettings.gamesPerEncounter] > 1; index-aligned with (and same length as) [results],
     * including the doubled entries PARTNER_ROTATION appends per encounter. Purely a display stat
     * for [info.rbuck.billiardscoreboard.domain.tournament.TournamentEngine.gameTally] - never
     * affects standings/rotation, which stay based on [results] alone. */
    val encounterGameTallies: List<Pair<Int, Int>> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)
