package info.rbuck.billiardscoreboard.ui.onepocketmatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.rbuck.billiardscoreboard.data.MatchRepository
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.data.PlayerRepository
import info.rbuck.billiardscoreboard.domain.MatchStatePayload
import info.rbuck.billiardscoreboard.domain.onepocket.OnePocketMatchEngine
import info.rbuck.billiardscoreboard.domain.onepocket.OnePocketMatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnePocketMatchViewModel(
    private val matchId: String,
    private val matchRepository: MatchRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val _match = MutableStateFlow<OnePocketMatchState?>(null)
    val match: StateFlow<OnePocketMatchState?> = _match.asStateFlow()

    private val _players = MutableStateFlow<Map<String, Player>>(emptyMap())
    val players: StateFlow<Map<String, Player>> = _players.asStateFlow()

    // Seeded from the stored record so reopening an archived match keeps it archived - see
    // StraightMatchViewModel for why this must not just default to false.
    private var archived = false

    private val _isArchived = MutableStateFlow(false)
    val isArchived: StateFlow<Boolean> = _isArchived.asStateFlow()

    init {
        viewModelScope.launch {
            archived = matchRepository.isArchived(matchId)
            _isArchived.value = archived
            val payload = matchRepository.loadPayload(matchId)
            val state = (payload as? MatchStatePayload.OnePocket)?.state ?: return@launch
            _match.value = state
            _players.value = state.playerIds
                .mapNotNull { playerRepository.getById(it) }
                .associateBy { it.id }
        }
    }

    private fun mutate(transform: (OnePocketMatchState) -> OnePocketMatchState) {
        val current = _match.value ?: return
        val updated = transform(current)
        _match.value = updated
        viewModelScope.launch { matchRepository.save(updated, archived) }
    }

    fun addPocket(count: Int = 1) = mutate { OnePocketMatchEngine.addPocket(it, count) }

    fun foul() = mutate { OnePocketMatchEngine.addFoul(it) }

    fun endTurn() = mutate { OnePocketMatchEngine.endTurn(it) }

    fun undo() = mutate { OnePocketMatchEngine.undo(it) }

    fun saveToArchive() {
        archived = true
        val current = _match.value ?: return
        viewModelScope.launch { matchRepository.save(current, archived = true) }
    }
}
