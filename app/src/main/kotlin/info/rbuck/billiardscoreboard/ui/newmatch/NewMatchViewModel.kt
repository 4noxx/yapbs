package info.rbuck.billiardscoreboard.ui.newmatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.rbuck.billiardscoreboard.data.Club
import info.rbuck.billiardscoreboard.data.ClubRepository
import info.rbuck.billiardscoreboard.data.MatchRepository
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.data.PlayerRepository
import info.rbuck.billiardscoreboard.domain.GameType
import info.rbuck.billiardscoreboard.domain.Handicap
import info.rbuck.billiardscoreboard.domain.simple.BreakMode
import info.rbuck.billiardscoreboard.domain.simple.SimpleMatchState
import info.rbuck.billiardscoreboard.domain.simple.SimpleSettings
import info.rbuck.billiardscoreboard.domain.straight.StraightMatchState
import info.rbuck.billiardscoreboard.domain.straight.StraightSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class NewMatchViewModel(
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository,
    private val clubRepository: ClubRepository,
) : ViewModel() {

    val players: StateFlow<List<Player>> = playerRepository.observeActivePlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clubs: StateFlow<List<Club>> = clubRepository.observeClubs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlayer(name: String, onCreated: (Player) -> Unit) {
        viewModelScope.launch { onCreated(playerRepository.createOrUpdate(name = name)) }
    }

    fun createSimpleMatch(
        gameType: GameType,
        player1: Player,
        player2: Player,
        raceTo: Int,
        breakMode: BreakMode,
        firstBreakPlayer: Int,
        handicap: Handicap?,
        onCreated: (String) -> Unit,
    ) {
        val state = SimpleMatchState(
            id = UUID.randomUUID().toString(),
            settings = SimpleSettings(gameType, raceTo, breakMode, firstBreakPlayer, handicap),
            playerIds = listOf(player1.id, player2.id),
        )
        viewModelScope.launch {
            matchRepository.save(state, archived = false)
            onCreated(state.id)
        }
    }

    fun createStraightMatch(
        player1: Player,
        player2: Player,
        raceTo: Int,
        maxInnings: Int?,
        firstPlayerIndex: Int,
        handicap: Handicap?,
        onCreated: (String) -> Unit,
    ) {
        val state = StraightMatchState(
            id = UUID.randomUUID().toString(),
            settings = StraightSettings(raceTo, maxInnings, handicap, firstPlayerIndex),
            playerIds = listOf(player1.id, player2.id),
        )
        viewModelScope.launch {
            matchRepository.save(state, archived = false)
            onCreated(state.id)
        }
    }
}
