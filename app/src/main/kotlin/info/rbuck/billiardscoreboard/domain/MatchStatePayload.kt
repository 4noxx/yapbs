package info.rbuck.billiardscoreboard.domain

import info.rbuck.billiardscoreboard.domain.onepocket.OnePocketMatchState
import info.rbuck.billiardscoreboard.domain.simple.SimpleMatchState
import info.rbuck.billiardscoreboard.domain.straight.StraightMatchState
import kotlinx.serialization.Serializable

/** Wraps any match-type's state so a single JSON column can hold all of them. */
@Serializable
sealed class MatchStatePayload {
    @Serializable
    data class Simple(val state: SimpleMatchState) : MatchStatePayload()

    @Serializable
    data class Straight(val state: StraightMatchState) : MatchStatePayload()

    @Serializable
    data class OnePocket(val state: OnePocketMatchState) : MatchStatePayload()
}
