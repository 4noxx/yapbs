package info.rbuck.billiardscoreboard.domain.simple

import info.rbuck.billiardscoreboard.domain.GameType
import info.rbuck.billiardscoreboard.domain.Handicap
import kotlinx.serialization.Serializable

enum class BreakMode {
    /** The player breaks first in rack 1, then breaks alternate every rack. */
    ALTERNATE,

    /** Whoever won the previous rack breaks the next one. */
    WINNER_BREAKS,
}

/** One rack win recorded for [playerIndex] (0 or 1). [isRunout] marks a clean break-and-run. */
@Serializable
data class SimpleAction(
    val playerIndex: Int,
    val isRunout: Boolean = false,
)

@Serializable
data class SimpleSettings(
    val gameType: GameType,
    val raceTo: Int,
    val breakMode: BreakMode,
    val firstBreakPlayer: Int,
    val handicap: Handicap? = null,
)

@Serializable
data class SimpleMatchState(
    val id: String,
    val settings: SimpleSettings,
    val playerIds: List<String>,
    val actions: List<SimpleAction> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)
