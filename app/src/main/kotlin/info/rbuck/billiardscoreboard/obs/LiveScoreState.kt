package info.rbuck.billiardscoreboard.obs

import kotlinx.serialization.Serializable

/** Snapshot of one player's live stats, published by whichever match screen is currently open. */
@Serializable
data class LiveScorePlayer(
    val name: String,
    val club: String,
    val crestPath: String?,
    val score: Int,
    /** Null for disciplines that don't track this stat (currently: 8/9/10-Ball - only 14.1 Straight Pool does). */
    val currentRun: Int?,
    val highestBreak: Int?,
    val innings: Int?,
    val atTurn: Boolean,
)

/** Snapshot of the currently open match, published for [ObsWebSocketClient] to push to OBS. */
@Serializable
data class LiveScoreState(
    val discipline: String,
    val raceTo: Int,
    val player1: LiveScorePlayer,
    val player2: LiveScorePlayer,
)
