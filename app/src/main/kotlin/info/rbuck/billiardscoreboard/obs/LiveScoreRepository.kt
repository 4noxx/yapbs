package info.rbuck.billiardscoreboard.obs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Holds whatever match screen is currently open, for [ObsWebSocketClient] to push to OBS. Null when no match is open. */
class LiveScoreRepository {
    private val _state = MutableStateFlow<LiveScoreState?>(null)
    val state: StateFlow<LiveScoreState?> = _state

    fun publish(state: LiveScoreState) {
        _state.value = state
    }

    fun clear() {
        _state.value = null
    }
}
