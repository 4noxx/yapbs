package info.rbuck.billiardscoreboard.data

import info.rbuck.billiardscoreboard.domain.GameType
import info.rbuck.billiardscoreboard.domain.MatchStatePayload
import info.rbuck.billiardscoreboard.domain.simple.SimpleMatchEngine
import info.rbuck.billiardscoreboard.domain.simple.SimpleMatchState
import info.rbuck.billiardscoreboard.domain.straight.StraightMatchEngine
import info.rbuck.billiardscoreboard.domain.straight.StraightMatchState
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MatchRepository(private val dao: MatchDao) {

    private val json = Json { ignoreUnknownKeys = true }

    fun observeArchived(): Flow<List<MatchRecordEntity>> = dao.observeArchived()

    fun observeFinishedBetween(playerAId: String, playerBId: String): Flow<List<MatchRecordEntity>> =
        dao.observeFinishedBetween(playerAId, playerBId)

    suspend fun loadPayload(id: String): MatchStatePayload? {
        val entity = dao.getById(id) ?: return null
        return json.decodeFromString(MatchStatePayload.serializer(), entity.stateJson)
    }

    /** Whether this match is already in the archive (History). Used to open it read-only. */
    suspend fun isArchived(id: String): Boolean = dao.getById(id)?.archived == true

    suspend fun save(state: SimpleMatchState, archived: Boolean) {
        val winner = SimpleMatchEngine.winnerIndex(state)
        val entity = MatchRecordEntity(
            id = state.id,
            gameType = state.settings.gameType.name,
            createdAt = state.createdAt,
            player1Id = state.playerIds.getOrElse(0) { "" },
            player2Id = state.playerIds.getOrElse(1) { "" },
            finished = SimpleMatchEngine.isOver(state),
            archived = archived,
            summary = "${SimpleMatchEngine.score(state, 0)} : ${SimpleMatchEngine.score(state, 1)}",
            winnerIndex = winner,
            stateJson = json.encodeToString(MatchStatePayload.serializer(), MatchStatePayload.Simple(state)),
        )
        dao.upsert(entity)
    }

    suspend fun save(state: StraightMatchState, archived: Boolean) {
        val winner = StraightMatchEngine.winnerIndex(state)
        val entity = MatchRecordEntity(
            id = state.id,
            gameType = GameType.STRAIGHT_POOL.name,
            createdAt = state.createdAt,
            player1Id = state.playerIds.getOrElse(0) { "" },
            player2Id = state.playerIds.getOrElse(1) { "" },
            finished = StraightMatchEngine.isOver(state),
            archived = archived,
            summary = "${StraightMatchEngine.score(state, 0)} : ${StraightMatchEngine.score(state, 1)}",
            winnerIndex = winner,
            stateJson = json.encodeToString(MatchStatePayload.serializer(), MatchStatePayload.Straight(state)),
        )
        dao.upsert(entity)
    }

    suspend fun delete(entity: MatchRecordEntity) = dao.delete(entity)

    suspend fun pruneOlderThan(cutoffMillis: Long) = dao.deleteArchivedOlderThan(cutoffMillis)

    suspend fun clearHistory() = dao.deleteAllArchived()
}
