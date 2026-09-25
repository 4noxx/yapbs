package info.rbuck.billiardscoreboard.domain

import info.rbuck.billiardscoreboard.domain.simple.SimpleMatchState
import info.rbuck.billiardscoreboard.domain.straight.StraightMatchState
import kotlinx.serialization.Serializable

/** Wraps either match-type's state so a single JSON column can hold both. */
@Serializable
sealed class MatchStatePayload {
    @Serializable
    data class Simple(val state: SimpleMatchState) : MatchStatePayload()

    @Serializable
    data class Straight(val state: StraightMatchState) : MatchStatePayload()
}
