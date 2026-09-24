package info.rbuck.billiardscoreboard.ui.navigation

object BsDestinations {
    const val START = "start"
    const val PLAYERS = "players"
    const val ARCHIVE = "archive"
    const val CLUBS = "clubs"
    const val SETTINGS = "settings"
    const val OBS_CONTROL = "obs_control"
    const val HEAD_TO_HEAD = "head_to_head?a={a}&b={b}"

    fun headToHead(aId: String? = null, bId: String? = null): String =
        if (aId != null && bId != null) "head_to_head?a=$aId&b=$bId" else "head_to_head?a=&b="

    // rematchOf is an optional query arg: when set, the New Match screen prefills players and
    // settings from that already-archived match instead of starting from blank defaults - used by
    // the "Save & Rematch" button on a finished match's scoreboard.
    const val NEW_SIMPLE_MATCH = "new_simple_match/{gameType}?rematchOf={rematchOf}"
    const val NEW_STRAIGHT_MATCH = "new_straight_match?rematchOf={rematchOf}"
    const val SIMPLE_MATCH = "simple_match/{matchId}"
    const val STRAIGHT_MATCH = "straight_match/{matchId}"
    const val ARCHIVE_DETAIL = "archive_detail/{matchId}"
    const val NEW_TRAINING = "new_training"
    const val TRAINING_SESSION = "training_session/{exercise}/{playerId}"
    const val IMPORT_PLAYERS_FILE = "import_players_file/{uri}"

    // Easter egg - see BsApplication's PlayerRepository wiring for the unlock trigger.
    const val NEW_TOURNAMENT = "new_tournament?rematchOf={rematchOf}"
    const val TOURNAMENT = "tournament/{tournamentId}"
    const val TOURNAMENT_HISTORY = "tournament_history"

    fun tournament(tournamentId: String) = "tournament/$tournamentId"

    fun newTournament(rematchOf: String? = null) =
        if (rematchOf != null) "new_tournament?rematchOf=$rematchOf" else "new_tournament"

    fun newSimpleMatch(gameType: String, rematchOf: String? = null) =
        if (rematchOf != null) "new_simple_match/$gameType?rematchOf=$rematchOf" else "new_simple_match/$gameType"
    fun newStraightMatch(rematchOf: String? = null) =
        if (rematchOf != null) "new_straight_match?rematchOf=$rematchOf" else "new_straight_match"
    fun simpleMatch(matchId: String) = "simple_match/$matchId"
    fun straightMatch(matchId: String) = "straight_match/$matchId"
    fun archiveDetail(matchId: String) = "archive_detail/$matchId"
    fun trainingSession(exercise: String, playerId: String) = "training_session/$exercise/$playerId"
    fun importPlayersFile(encodedUri: String) = "import_players_file/$encodedUri"
}
