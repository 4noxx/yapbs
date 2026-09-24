package info.rbuck.billiardscoreboard.ui.simplematch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.rbuck.billiardscoreboard.data.MatchRepository
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.data.PlayerRepository
import info.rbuck.billiardscoreboard.domain.MatchStatePayload
import info.rbuck.billiardscoreboard.domain.simple.SimpleMatchEngine
import info.rbuck.billiardscoreboard.domain.simple.SimpleMatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SimpleMatchViewModel(
    private val matchId: String,
    private val matchRepository: MatchRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val _match = MutableStateFlow<SimpleMatchState?>(null)
    val match: StateFlow<SimpleMatchState?> = _match.asStateFlow()

    private val _players = MutableStateFlow<Map<String, Player>>(emptyMap())
    val players: StateFlow<Map<String, Player>> = _players.asStateFlow()

    // Seeded from the stored record so reopening an archived match keeps it archived - otherwise the
    // first mutate() (even an Undo) would rewrite it with archived = false and drop it out of History.
    private var archived = false

    private val _isArchived = MutableStateFlow(false)
    val isArchived: StateFlow<Boolean> = _isArchived.asStateFlow()

    init {
        viewModelScope.launch {
            archived = matchRepository.isArchived(matchId)
            _isArchived.value = archived
            val payload = matchRepository.loadPayload(matchId)
            val state = (payload as? MatchStatePayload.Simple)?.state ?: return@launch
            _match.value = state
            _players.value = state.playerIds
                .mapNotNull { playerRepository.getById(it) }
                .associateBy { it.id }
        }
    }

    private fun mutate(transform: (SimpleMatchState) -> SimpleMatchState) {
        val current = _match.value ?: return
        val updated = transform(current)
        _match.value = updated
        viewModelScope.launch { matchRepository.save(updated, archived) }
    }

    fun addWin(playerIndex: Int, isRunout: Boolean = false) = mutate { SimpleMatchEngine.addRackWin(it, playerIndex, isRunout) }

    fun removeLastFor(playerIndex: Int) = mutate { SimpleMatchEngine.removeLastRackFor(it, playerIndex) }

    fun undo() = mutate { SimpleMatchEngine.undo(it) }

    fun saveToArchive() {
        archived = true
        val current = _match.value ?: return
        viewModelScope.launch { matchRepository.save(current, archived = true) }
    }
}
