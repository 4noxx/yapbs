package info.rbuck.billiardscoreboard.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.rbuck.billiardscoreboard.data.Club
import info.rbuck.billiardscoreboard.data.ClubRepository
import info.rbuck.billiardscoreboard.data.MatchRepository
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.data.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PlayerSort { NAME, FREQUENCY }

class PlayersViewModel(
    private val playerRepository: PlayerRepository,
    matchRepository: MatchRepository,
    clubRepository: ClubRepository,
) : ViewModel() {

    val clubs: StateFlow<List<Club>> = clubRepository.observeClubs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val matchCounts: kotlinx.coroutines.flow.Flow<Map<String, Int>> =
        matchRepository.observeArchived().map { matches ->
            val counts = mutableMapOf<String, Int>()
            for (m in matches) {
                counts[m.player1Id] = (counts[m.player1Id] ?: 0) + 1
                counts[m.player2Id] = (counts[m.player2Id] ?: 0) + 1
            }
            counts
        }

    private val sortMode = MutableStateFlow(PlayerSort.NAME)

    fun setSort(sort: PlayerSort) {
        sortMode.value = sort
    }

    val players: StateFlow<List<Player>> = combine(
        playerRepository.observeActivePlayers(),
        matchCounts,
        sortMode,
    ) { players, counts, mode ->
        when (mode) {
            PlayerSort.NAME -> players.sortedBy { it.name.lowercase() }
            PlayerSort.FREQUENCY -> players.sortedByDescending { counts[it.id] ?: 0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createOrUpdate(id: String? = null, name: String, clubId: String?) {
        viewModelScope.launch { playerRepository.createOrUpdate(id, name, clubId) }
    }

    fun delete(player: Player) {
        viewModelScope.launch { playerRepository.delete(player) }
    }
}
