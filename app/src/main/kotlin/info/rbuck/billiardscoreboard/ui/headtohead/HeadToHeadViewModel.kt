package info.rbuck.billiardscoreboard.ui.headtohead

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.rbuck.billiardscoreboard.data.MatchRepository
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.data.PlayerRepository
import info.rbuck.billiardscoreboard.domain.MatchStatePayload
import info.rbuck.billiardscoreboard.domain.straight.StraightMatchEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HeadToHeadViewModel(
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository,
) : ViewModel() {

    val players: StateFlow<List<Player>> = playerRepository.observeActivePlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _playerAId = MutableStateFlow<String?>(null)
    val playerAId: StateFlow<String?> = _playerAId

    private val _playerBId = MutableStateFlow<String?>(null)
    val playerBId: StateFlow<String?> = _playerBId

    /** All finished games between the two picked players (A-perspective), or null until both are
     * picked. The screen filters/aggregates this itself so discipline/timeframe filters are cheap. */
    val games: StateFlow<List<H2HGame>?> =
        combine(_playerAId, _playerBId) { a, b -> a to b }
            .flatMapLatest { (a, b) ->
                if (a != null && b != null && a != b) {
                    matchRepository.observeFinishedBetween(a, b).map { matchRowsToGames(it, a, b) }
                } else {
                    flowOf(null)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _straightDetail = MutableStateFlow<StraightH2HDetail?>(null)
    val straightDetail: StateFlow<StraightH2HDetail?> = _straightDetail
    private var straightJob: Job? = null

    fun selectA(id: String) {
        _playerAId.value = id
        if (_playerBId.value == id) _playerBId.value = null
        _straightDetail.value = null
    }

    fun selectB(id: String) {
        _playerBId.value = id
        if (_playerAId.value == id) _playerAId.value = null
        _straightDetail.value = null
    }

    /** Decodes the given 14.1 matches' payloads and aggregates the per-player detail stats. The
     * screen calls this with whatever set of 14.1 match ids the current filter leaves visible. */
    fun loadStraightDetail(matchIds: List<String>, playerAId: String, playerBId: String) {
        straightJob?.cancel()
        if (matchIds.isEmpty()) {
            _straightDetail.value = null
            return
        }
        straightJob = viewModelScope.launch {
            var tpA = 0; var inA = 0; var hbA = 0; var flA = 0
            var tpB = 0; var inB = 0; var hbB = 0; var flB = 0
            var counted = 0
            for (id in matchIds) {
                val state = (matchRepository.loadPayload(id) as? MatchStatePayload.Straight)?.state ?: continue
                val idxA = state.playerIds.indexOf(playerAId)
                val idxB = state.playerIds.indexOf(playerBId)
                if (idxA < 0 || idxB < 0) continue
                val sa = StraightMatchEngine.stats(state, idxA)
                val sb = StraightMatchEngine.stats(state, idxB)
                tpA += sa.totalPoints; inA += sa.innings; hbA = maxOf(hbA, sa.highestBreak); flA += sa.fouls
                tpB += sb.totalPoints; inB += sb.innings; hbB = maxOf(hbB, sb.highestBreak); flB += sb.fouls
                counted++
            }
            _straightDetail.value = if (counted == 0) {
                null
            } else {
                StraightH2HDetail(
                    matchCount = counted,
                    avgA = if (inA > 0) tpA.toFloat() / inA else 0f,
                    avgB = if (inB > 0) tpB.toFloat() / inB else 0f,
                    highestBreakA = hbA,
                    highestBreakB = hbB,
                    foulsA = flA,
                    foulsB = flB,
                )
            }
        }
    }
}
