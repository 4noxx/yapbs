package info.rbuck.billiardscoreboard.ui.headtohead

import info.rbuck.billiardscoreboard.data.MatchRecordEntity
import info.rbuck.billiardscoreboard.domain.GameType

/** One finished match, viewed from player A's side (scores/winner already un-swapped from the
 * DB's raw player1/player2 slot order). [winner]: 0 = A, 1 = B, -1 = draw. */
data class H2HGame(
    val matchId: String,
    val createdAt: Long,
    val gameType: GameType?,
    val scoreA: Int,
    val scoreB: Int,
    val winner: Int,
)

data class DisciplineRecord(val winsA: Int, val draws: Int, val winsB: Int) {
    val total get() = winsA + draws + winsB
}

/** Aggregated head-to-head record, from A's perspective. All lists are oldest-first. */
data class HeadToHead(
    val games: List<H2HGame>,
    val winsA: Int,
    val draws: Int,
    val winsB: Int,
    /** Sum of each side's rack scores across 8/9/10-Ball games only (14.1 points aren't racks). */
    val racksA: Int,
    val racksB: Int,
    val byDiscipline: Map<GameType, DisciplineRecord>,
    /** Player currently on a winning streak (0 = A, 1 = B), or -1 if the last game was a draw / no games. */
    val streakPlayer: Int,
    val streakLength: Int,
) {
    val total get() = games.size
    val last: H2HGame? get() = games.lastOrNull()
    val first: H2HGame? get() = games.firstOrNull()

    /** Running win-difference after each game (A wins +1, B wins -1, draw ±0). Length = [total]. */
    val momentum: List<Int>
        get() {
            var acc = 0
            return games.map { g ->
                acc += when (g.winner) {
                    0 -> 1
                    1 -> -1
                    else -> 0
                }
                acc
            }
        }

    companion object {
        val EMPTY = HeadToHead(emptyList(), 0, 0, 0, 0, 0, emptyMap(), -1, 0)
    }
}

/** 14.1-only per-player aggregates over the pairing's Straight Pool matches. Needs the match
 * payloads decoded (see [HeadToHeadViewModel.loadStraightDetail]), unlike everything else here. */
data class StraightH2HDetail(
    val matchCount: Int,
    val avgA: Float,
    val avgB: Float,
    val highestBreakA: Int,
    val highestBreakB: Int,
    val foulsA: Int,
    val foulsB: Int,
)

/** Un-swaps the raw match rows into A-perspective [H2HGame]s. [matches] is expected to already be
 * filtered to finished games between exactly these two players (MatchDao.observeFinishedBetween). */
fun matchRowsToGames(matches: List<MatchRecordEntity>, playerAId: String, playerBId: String): List<H2HGame> {
    if (playerAId == playerBId) return emptyList()
    return matches.sortedBy { it.createdAt }.mapNotNull { row ->
        val aIsSlot0 = row.player1Id == playerAId
        if (!aIsSlot0 && row.player1Id != playerBId) return@mapNotNull null

        val (s0, s1) = row.summary.split(":").let {
            (it.getOrNull(0)?.trim()?.toIntOrNull() ?: 0) to (it.getOrNull(1)?.trim()?.toIntOrNull() ?: 0)
        }
        val winner = when {
            row.winnerIndex == DRAW -> -1
            row.winnerIndex < 0 -> -1
            (row.winnerIndex == 0) == aIsSlot0 -> 0
            else -> 1
        }
        H2HGame(
            matchId = row.id,
            createdAt = row.createdAt,
            gameType = GameType.entries.firstOrNull { it.name == row.gameType },
            scoreA = if (aIsSlot0) s0 else s1,
            scoreB = if (aIsSlot0) s1 else s0,
            winner = winner,
        )
    }
}

/** Folds an (already filtered) A-perspective game list into a [HeadToHead]. */
fun aggregateHeadToHead(games: List<H2HGame>): HeadToHead {
    if (games.isEmpty()) return HeadToHead.EMPTY

    val winsA = games.count { it.winner == 0 }
    val winsB = games.count { it.winner == 1 }
    val draws = games.count { it.winner == -1 }

    val racksA = games.filter { it.gameType?.isStraightPool == false }.sumOf { it.scoreA }
    val racksB = games.filter { it.gameType?.isStraightPool == false }.sumOf { it.scoreB }

    val byDiscipline = games
        .mapNotNull { g -> g.gameType?.let { it to g } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, gs) ->
            DisciplineRecord(
                winsA = gs.count { it.winner == 0 },
                draws = gs.count { it.winner == -1 },
                winsB = gs.count { it.winner == 1 },
            )
        }

    var streakPlayer = -1
    var streakLength = 0
    for (g in games.asReversed()) {
        if (g.winner == -1) break
        if (streakPlayer == -1) {
            streakPlayer = g.winner
            streakLength = 1
        } else if (g.winner == streakPlayer) {
            streakLength++
        } else {
            break
        }
    }

    return HeadToHead(games, winsA, draws, winsB, racksA, racksB, byDiscipline, streakPlayer, streakLength)
}

fun computeHeadToHead(matches: List<MatchRecordEntity>, playerAId: String, playerBId: String): HeadToHead =
    aggregateHeadToHead(matchRowsToGames(matches, playerAId, playerBId))

/** Matches [MatchRecordEntity]'s "-2 = draw" convention. */
private const val DRAW = -2
