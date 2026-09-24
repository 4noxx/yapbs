package info.rbuck.billiardscoreboard.ui.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.data.PlayerRepository
import info.rbuck.billiardscoreboard.data.TournamentRepository
import info.rbuck.billiardscoreboard.domain.tournament.TournamentEngine
import info.rbuck.billiardscoreboard.domain.tournament.TournamentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TournamentViewModel(
    private val tournamentId: String,
    private val tournamentRepository: TournamentRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val _tournament = MutableStateFlow<TournamentState?>(null)
    val tournament: StateFlow<TournamentState?> = _tournament.asStateFlow()

    private val _players = MutableStateFlow<Map<String, Player>>(emptyMap())
    val players: StateFlow<Map<String, Player>> = _players.asStateFlow()

    init {
        viewModelScope.launch {
            val state = tournamentRepository.loadState(tournamentId) ?: return@launch
            _tournament.value = state
            _players.value = state.playerIds
                .mapNotNull { playerRepository.getById(it) }
                .associateBy { it.id }
        }
    }

    private fun mutate(transform: (TournamentState) -> TournamentState) {
        val current = _tournament.value ?: return
        val updated = transform(current)
        _tournament.value = updated
        viewModelScope.launch { tournamentRepository.save(updated) }
    }

    /** The one action every tap of an encounter-side's "+" button goes through now - see
     * [TournamentEngine.recordEncounterGame]. */
    fun recordEncounterGame(winnerId: String, loserId: String) = mutate { TournamentEngine.recordEncounterGame(it, winnerId, loserId) }

    fun removeLastEncounterGameFor(side: Int) = mutate { TournamentEngine.removeLastEncounterGameFor(it, side) }

    fun undo() = mutate { TournamentEngine.undo(it) }
}
