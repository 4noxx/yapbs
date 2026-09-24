package info.rbuck.billiardscoreboard.ui.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.rbuck.billiardscoreboard.data.PlayerRepository
import info.rbuck.billiardscoreboard.data.TournamentRecordEntity
import info.rbuck.billiardscoreboard.data.TournamentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TournamentHistoryViewModel(
    private val tournamentRepository: TournamentRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    val entries: StateFlow<List<TournamentRecordEntity>> = tournamentRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(entry: TournamentRecordEntity) {
        viewModelScope.launch { tournamentRepository.delete(entry) }
    }

    private val _playerNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val playerNames: StateFlow<Map<String, String>> = _playerNames.asStateFlow()

    init {
        viewModelScope.launch {
            entries.collect { list ->
                val ids = list.flatMap { it.playerIds.split(",") }.filter { it.isNotBlank() }.distinct()
                val missing = ids.filter { it !in _playerNames.value }
                if (missing.isNotEmpty()) {
                    val resolved = missing.mapNotNull { id -> playerRepository.getById(id)?.let { id to it.name } }
                    _playerNames.value = _playerNames.value + resolved
                }
            }
        }
    }
}
