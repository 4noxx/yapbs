package info.rbuck.billiardscoreboard.data

import info.rbuck.billiardscoreboard.domain.training.TrainingMatchEngine
import info.rbuck.billiardscoreboard.domain.training.TrainingMatchState
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class TrainingRecordRepository(private val dao: TrainingDao) {

    private val json = Json { ignoreUnknownKeys = true }

    fun observeAll(): Flow<List<TrainingRecordEntity>> = dao.observeAll()

    /** Idempotent - keyed on [TrainingMatchState.id], so calling it again for the same session
     * (e.g. results dialog re-composition) just overwrites the identical row. */
    suspend fun save(state: TrainingMatchState) {
        val exercise = state.exercise
        val stats = TrainingMatchEngine.stats(state)
        val inningScores = TrainingMatchEngine.completedAttempts(state).map { it.ballCount }
        val summary = if (exercise.targetPoints != null) {
            "${stats.totalPoints} / ${exercise.targetPoints}"
        } else {
            "HR ${stats.highRun}"
        }
        dao.upsert(
            TrainingRecordEntity(
                id = state.id,
                exercise = exercise.name,
                createdAt = System.currentTimeMillis(),
                playerId = state.playerId,
                totalPoints = stats.totalPoints,
                average = stats.average,
                highRun = stats.highRun,
                innings = stats.innings,
                summary = summary,
                inningScoresJson = json.encodeToString(ListSerializer(Int.serializer()), inningScores),
            ),
        )
    }

    fun inningScores(entity: TrainingRecordEntity): List<Int> =
        runCatching { json.decodeFromString(ListSerializer(Int.serializer()), entity.inningScoresJson) }.getOrDefault(emptyList())

    suspend fun delete(entity: TrainingRecordEntity) = dao.delete(entity)

    suspend fun pruneOlderThan(cutoffMillis: Long) = dao.deleteOlderThan(cutoffMillis)

    suspend fun clearHistory() = dao.deleteAll()
}
