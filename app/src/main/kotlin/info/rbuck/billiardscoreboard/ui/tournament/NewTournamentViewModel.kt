package info.rbuck.billiardscoreboard.ui.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.rbuck.billiardscoreboard.data.Club
import info.rbuck.billiardscoreboard.data.ClubRepository
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.data.PlayerRepository
import info.rbuck.billiardscoreboard.data.TournamentRepository
import info.rbuck.billiardscoreboard.domain.tournament.TournamentMode
import info.rbuck.billiardscoreboard.domain.tournament.TournamentSettings
import info.rbuck.billiardscoreboard.domain.tournament.TournamentState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class NewTournamentViewModel(
    private val playerRepository: PlayerRepository,
    private val tournamentRepository: TournamentRepository,
    private val clubRepository: ClubRepository,
) : ViewModel() {

    val players: StateFlow<List<Player>> = playerRepository.observeActivePlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clubs: StateFlow<List<Club>> = clubRepository.observeClubs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlayer(name: String, onCreated: (Player) -> Unit) {
        viewModelScope.launch { onCreated(playerRepository.createOrUpdate(name = name)) }
    }

    /** Loads a past tournament's mode/target-wins/roster for "Save & Rematch" prefill. */
    fun loadForRematch(tournamentId: String, onLoaded: (TournamentState) -> Unit) {
        viewModelScope.launch { tournamentRepository.loadState(tournamentId)?.let(onLoaded) }
    }

    /**
     * Creates and saves the tournament. [randomOrder] draws the starting order (who's first at the
     * table, and the order the rest join the queue): true = shuffle once here, false = keep the
     * roster exactly as the user listed it on the left.
     */
    fun createTournament(
        mode: TournamentMode,
        targetWins: Int?,
        gamesPerEncounter: Int,
        playerIds: List<String>,
        randomOrder: Boolean,
        onCreated: (String) -> Unit,
    ) {
        val state = TournamentState(
            id = UUID.randomUUID().toString(),
            settings = TournamentSettings(mode, targetWins, gamesPerEncounter),
            playerIds = if (randomOrder) playerIds.shuffled() else playerIds,
        )
        viewModelScope.launch {
            tournamentRepository.save(state)
            onCreated(state.id)
        }
    }
}
