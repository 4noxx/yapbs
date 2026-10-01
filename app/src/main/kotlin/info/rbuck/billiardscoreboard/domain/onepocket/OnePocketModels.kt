package info.rbuck.billiardscoreboard.domain.onepocket

import kotlinx.serialization.Serializable

/**
 * One Pocket: each player scores only balls pocketed in their own assigned corner pocket (the
 * other 4 pockets are neutral and don't count either way), first to [OnePocketSettings.raceTo]
 * wins - 8 is the official BCA target. There's no "safety" call: every legal non-potting shot is
 * just a plain end of turn.
 */
@Serializable
sealed class OnePocketAction {
    /** One or more balls legally pocketed in the shooter's own pocket on a single stroke - the
     * shooter stays at the table. */
    @Serializable
    data class Pocket(val count: Int = 1) : OnePocketAction()

    /** A foul: one-ball penalty (a point is given back, or owed if the player has none yet), turn
     * ends. Three consecutive fouls by the same player loses the game outright. */
    @Serializable
    data object Foul : OnePocketAction()

    /** Turn ends without a pot or a foul (a legal miss or safety). */
    @Serializable
    data object EndTurn : OnePocketAction()
}

@Serializable
data class OnePocketSettings(
    val raceTo: Int = 8,
    val firstBreakPlayer: Int = 0,
)

@Serializable
data class OnePocketMatchState(
    val id: String,
    val settings: OnePocketSettings,
    val playerIds: List<String>,
    val actions: List<OnePocketAction> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)
