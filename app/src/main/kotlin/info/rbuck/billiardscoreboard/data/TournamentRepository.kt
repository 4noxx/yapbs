package info.rbuck.billiardscoreboard.data

import info.rbuck.billiardscoreboard.domain.tournament.TournamentEngine
import info.rbuck.billiardscoreboard.domain.tournament.TournamentState
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TournamentRepository(private val dao: TournamentDao) {

    private val json = Json { ignoreUnknownKeys = true }

    fun observeAll(): Flow<List<TournamentRecordEntity>> = dao.observeAll()

    suspend fun loadState(id: String): TournamentState? {
        val entity = dao.getById(id) ?: return null
        return json.decodeFromString(TournamentState.serializer(), entity.stateJson)
    }

    suspend fun save(state: TournamentState) {
        val entity = TournamentRecordEntity(
            id = state.id,
            mode = state.settings.mode.name,
            createdAt = state.createdAt,
            playerIds = state.playerIds.joinToString(","),
            finished = TournamentEngine.isOver(state),
            championId = TournamentEngine.championId(state),
            stateJson = json.encodeToString(TournamentState.serializer(), state),
        )
        dao.upsert(entity)
    }

    suspend fun delete(entity: TournamentRecordEntity) = dao.delete(entity)

    suspend fun pruneOlderThan(cutoffMillis: Long) = dao.deleteFinishedOlderThan(cutoffMillis)

    suspend fun clearHistory() = dao.deleteAllFinished()
}
