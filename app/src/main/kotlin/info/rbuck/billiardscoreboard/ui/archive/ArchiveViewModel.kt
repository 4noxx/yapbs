package info.rbuck.billiardscoreboard.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.rbuck.billiardscoreboard.data.MatchRecordEntity
import info.rbuck.billiardscoreboard.data.MatchRepository
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.data.PlayerRepository
import info.rbuck.billiardscoreboard.data.TrainingRecordEntity
import info.rbuck.billiardscoreboard.data.TrainingRecordRepository
import info.rbuck.billiardscoreboard.domain.MatchStatePayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed class RematchResult {
    data class Simple(val matchId: String) : RematchResult()
    data class Straight(val matchId: String) : RematchResult()
}

class ArchiveViewModel(
    private val matchRepository: MatchRepository,
    private val playerRepository: PlayerRepository,
    private val trainingRecordRepository: TrainingRecordRepository,
) : ViewModel() {

    val matches: StateFlow<List<MatchRecordEntity>> = matchRepository.observeArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trainingSessions: StateFlow<List<TrainingRecordEntity>> = trainingRecordRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _players = MutableStateFlow<Map<String, Player>>(emptyMap())
    val players: StateFlow<Map<String, Player>> = _players.asStateFlow()

    init {
        viewModelScope.launch {
            combine(matches, trainingSessions) { matchList, trainingList ->
                (matchList.map { it.player1Id } + matchList.map { it.player2Id } + trainingList.map { it.playerId }).distinct()
            }.collect { ids ->
                _players.value = ids.mapNotNull { playerRepository.getById(it) }.associateBy { it.id }
            }
        }
    }

    fun inningScores(entity: TrainingRecordEntity): List<Int> = trainingRecordRepository.inningScores(entity)

    fun delete(entity: MatchRecordEntity) {
        viewModelScope.launch { matchRepository.delete(entity) }
    }

    fun deleteTraining(entity: TrainingRecordEntity) {
        viewModelScope.launch { trainingRecordRepository.delete(entity) }
    }

    fun rematch(entity: MatchRecordEntity, onResult: (RematchResult) -> Unit) {
        viewModelScope.launch {
            when (val payload = matchRepository.loadPayload(entity.id)) {
                is MatchStatePayload.Simple -> {
                    val old = payload.state
                    val swapped = old.copy(
                        id = UUID.randomUUID().toString(),
                        playerIds = old.playerIds.reversed(),
                        actions = emptyList(),
                        createdAt = System.currentTimeMillis(),
                    )
                    matchRepository.save(swapped, archived = false)
                    onResult(RematchResult.Simple(swapped.id))
                }

                is MatchStatePayload.Straight -> {
                    val old = payload.state
                    val swapped = old.copy(
                        id = UUID.randomUUID().toString(),
                        playerIds = old.playerIds.reversed(),
                        actions = emptyList(),
                        createdAt = System.currentTimeMillis(),
                    )
                    matchRepository.save(swapped, archived = false)
                    onResult(RematchResult.Straight(swapped.id))
                }

                null -> Unit
            }
        }
    }
}
