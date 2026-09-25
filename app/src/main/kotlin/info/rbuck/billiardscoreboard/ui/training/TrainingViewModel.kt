package info.rbuck.billiardscoreboard.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.data.PlayerRepository
import info.rbuck.billiardscoreboard.data.TrainingRecordRepository
import info.rbuck.billiardscoreboard.domain.training.TrainingExercise
import info.rbuck.billiardscoreboard.domain.training.TrainingMatchEngine
import info.rbuck.billiardscoreboard.domain.training.TrainingMatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** Training sessions are ephemeral - kept only in this ViewModel (survives rotation, lost on process death). */
class TrainingViewModel(
    exercise: TrainingExercise,
    playerId: String,
    private val playerRepository: PlayerRepository,
    private val trainingRecordRepository: TrainingRecordRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        TrainingMatchState(id = UUID.randomUUID().toString(), exercise = exercise, playerId = playerId),
    )
    val state: StateFlow<TrainingMatchState> = _state.asStateFlow()

    private val _player = MutableStateFlow<Player?>(null)
    val player: StateFlow<Player?> = _player.asStateFlow()

    init {
        viewModelScope.launch { _player.value = playerRepository.getById(playerId) }
    }

    private fun mutate(transform: (TrainingMatchState) -> TrainingMatchState) {
        _state.value = transform(_state.value)
    }

    fun addBalls(diff: Int) = mutate { TrainingMatchEngine.addBalls(it, diff) }
    fun setBallsOnTable(newCount: Int) = mutate { TrainingMatchEngine.setBallsOnTable(it, newCount) }
    fun forceBreakballRack() = mutate { TrainingMatchEngine.forceBreakballRack(it) }
    fun recordError() = mutate { TrainingMatchEngine.recordError(it) }
    fun endAttempt() = mutate { TrainingMatchEngine.endAttempt(it) }
    fun undo() = mutate { TrainingMatchEngine.undo(it) }

    /** Persist the current session to History. Idempotent (keyed on the session id), so it's safe
     * to call more than once. Caller decides *when* - only for a played-through session, and only
     * when the "save training to history" setting is on. */
    fun persistToHistory() {
        viewModelScope.launch { trainingRecordRepository.save(_state.value) }
    }
}
