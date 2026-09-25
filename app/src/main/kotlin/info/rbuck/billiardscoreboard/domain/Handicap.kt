package info.rbuck.billiardscoreboard.domain

import kotlinx.serialization.Serializable

/**
 * Head-start points granted to one player. [playerIndex] is 0 or 1.
 */
@Serializable
data class Handicap(
    val playerIndex: Int,
    val points: Int,
)
