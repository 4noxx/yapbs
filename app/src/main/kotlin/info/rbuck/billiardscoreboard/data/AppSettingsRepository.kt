package info.rbuck.billiardscoreboard.data

import android.content.Context
import android.content.res.Configuration
import info.rbuck.billiardscoreboard.i18n.AppLanguage
import info.rbuck.billiardscoreboard.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Small persisted app-wide preferences (dark mode, keep screen on) backed by SharedPreferences. */
class AppSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Light / Dark / Vintage - see Settings > Display. Migrates the old dark-mode boolean the first
     * time this is read on an existing install, then stores under its own key from then on. */
    private val _appTheme = MutableStateFlow(loadAppTheme(prefs, context))
    val appTheme: StateFlow<AppTheme> = _appTheme

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEY_KEEP_SCREEN_ON, false))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn

    /** The app connects out to OBS's built-in obs-websocket server and pushes live-score values into named text sources. */
    private val _obsWebSocketEnabled = MutableStateFlow(prefs.getBoolean(KEY_OBS_WS_ENABLED, false))
    val obsWebSocketEnabled: StateFlow<Boolean> = _obsWebSocketEnabled

    private val _obsWebSocketHost = MutableStateFlow(prefs.getString(KEY_OBS_WS_HOST, "") ?: "")
    val obsWebSocketHost: StateFlow<String> = _obsWebSocketHost

    private val _obsWebSocketPort = MutableStateFlow(prefs.getInt(KEY_OBS_WS_PORT, DEFAULT_OBS_WS_PORT))
    val obsWebSocketPort: StateFlow<Int> = _obsWebSocketPort

    private val _obsWebSocketPassword = MutableStateFlow(prefs.getString(KEY_OBS_WS_PASSWORD, "") ?: "")
    val obsWebSocketPassword: StateFlow<String> = _obsWebSocketPassword

    /** Prefixes every OBS text-source name (e.g. "tisch1" -> "tisch1_name1") so several tablets can push
     * into the same OBS instance without overwriting each other's sources. Empty = unprefixed (legacy names). */
    private val _obsWebSocketSourcePrefix = MutableStateFlow(prefs.getString(KEY_OBS_WS_SOURCE_PREFIX, "") ?: "")
    val obsWebSocketSourcePrefix: StateFlow<String> = _obsWebSocketSourcePrefix

    /** Scene [ObsWebSocketClient.pauseStream] switches OBS to for a "stream pause" (obs-websocket's
     * Stream output has no native pause). Empty = the Pause button in the Regie screen is hidden. */
    private val _obsPauseSceneName = MutableStateFlow(prefs.getString(KEY_OBS_PAUSE_SCENE, "") ?: "")
    val obsPauseSceneName: StateFlow<String> = _obsPauseSceneName

    /** Comma-separated names of top-level scene items (typically OBS Groups, e.g. "Player1,Player2,
     * Scoreboard") to show only while a match is open and hide the rest of the time - so crests/
     * score bars don't sit visible-but-blank on an idle scene between matches. Empty = off. */
    private val _obsAutoHideSourceNames = MutableStateFlow(prefs.getString(KEY_OBS_AUTO_HIDE_SOURCES, "") ?: "")
    val obsAutoHideSourceNames: StateFlow<String> = _obsAutoHideSourceNames

    /** Same idea as [obsAutoHideSourceNames], but shown only while the open match is specifically
     * 14.1 Straight Pool (e.g. "Statistik" - innings/highest-break aren't tracked for 8/9/10-Ball). */
    private val _obsStraightPoolOnlySourceNames = MutableStateFlow(prefs.getString(KEY_OBS_STRAIGHT_ONLY_SOURCES, "") ?: "")
    val obsStraightPoolOnlySourceNames: StateFlow<String> = _obsStraightPoolOnlySourceNames

    /** On by default whenever OBS WebSocket itself is on - lets a club that only uses the scoreboard
     * push (not the Regie record/stream controls) hide that tile from Start again. */
    private val _obsRegieTileEnabled = MutableStateFlow(prefs.getBoolean(KEY_OBS_REGIE_TILE_ENABLED, true))
    val obsRegieTileEnabled: StateFlow<Boolean> = _obsRegieTileEnabled

    /** MAC address of the OBS PC's network adapter, for the "Wake PC" button in Regie
     * (see [info.rbuck.billiardscoreboard.obs.WakeOnLan]). Empty hides that button. */
    private val _obsWakeOnLanMac = MutableStateFlow(prefs.getString(KEY_OBS_WAKE_ON_LAN_MAC, "") ?: "")
    val obsWakeOnLanMac: StateFlow<String> = _obsWakeOnLanMac

    /** Name of the OBS media/camera source (e.g. a "Kamera" RTSP input). When set, Regie gets a
     * "reload camera" button, and the source is auto-restarted after the app reconnects following a
     * longer outage - an RTSP camera keeps running while the PC sleeps, but OBS often holds the
     * last frame frozen on wake until its stream is restarted. Empty = neither. */
    private val _obsCameraSourceName = MutableStateFlow(prefs.getString(KEY_OBS_CAMERA_SOURCE, "") ?: "")
    val obsCameraSourceName: StateFlow<String> = _obsCameraSourceName

    private val _defaultRace8Ball = MutableStateFlow(prefs.getInt(KEY_RACE_8BALL, DEFAULT_RACE_8BALL))
    val defaultRace8Ball: StateFlow<Int> = _defaultRace8Ball

    private val _defaultRace9Ball = MutableStateFlow(prefs.getInt(KEY_RACE_9BALL, DEFAULT_RACE_9BALL))
    val defaultRace9Ball: StateFlow<Int> = _defaultRace9Ball

    private val _defaultRace10Ball = MutableStateFlow(prefs.getInt(KEY_RACE_10BALL, DEFAULT_RACE_10BALL))
    val defaultRace10Ball: StateFlow<Int> = _defaultRace10Ball

    private val _defaultRaceStraightPool = MutableStateFlow(prefs.getInt(KEY_RACE_STRAIGHT_POOL, DEFAULT_RACE_STRAIGHT_POOL))
    val defaultRaceStraightPool: StateFlow<Int> = _defaultRaceStraightPool

    /** Pre-selected as the club filter whenever a player picker dialog opens. Null = "All clubs". */
    private val _defaultClubId = MutableStateFlow(prefs.getString(KEY_DEFAULT_CLUB_ID, null))
    val defaultClubId: StateFlow<String?> = _defaultClubId

    /** Only affects the Training rules dialog - see [info.rbuck.billiardscoreboard.i18n.Translations]. */
    private val _language = MutableStateFlow(AppLanguage.fromCode(prefs.getString(KEY_LANGUAGE, AppLanguage.EN.code)))
    val language: StateFlow<AppLanguage> = _language

    /** Off by default - Settings > Tournament. Reveals the Tournament tile on Start. Reuses the
     * old easter-egg preference key so anyone who'd already unlocked it that way keeps it enabled. */
    private val _tournamentEnabled = MutableStateFlow(prefs.getBoolean(KEY_TOURNAMENT_UNLOCKED, false))
    val tournamentEnabled: StateFlow<Boolean> = _tournamentEnabled

    /** Off by default - Settings > Training. When on, a fully played-through solo training session
     * (all innings finished, or "Finish" on the open-ended HighRun) is written to History. */
    private val _saveTrainingToHistory = MutableStateFlow(prefs.getBoolean(KEY_SAVE_TRAINING_HISTORY, false))
    val saveTrainingToHistory: StateFlow<Boolean> = _saveTrainingToHistory

    /** Finished matches, tournaments and training sessions older than this many days are pruned
     * from History on app start. 0 = keep forever. Default 365 - see Settings > Data & privacy. */
    private val _historyRetentionDays = MutableStateFlow(prefs.getInt(KEY_HISTORY_RETENTION_DAYS, DEFAULT_HISTORY_RETENTION_DAYS))
    val historyRetentionDays: StateFlow<Int> = _historyRetentionDays

    fun setAppTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_APP_THEME, theme.name).apply()
        _appTheme.value = theme
    }

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
        _keepScreenOn.value = enabled
    }

    fun setObsWebSocketEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OBS_WS_ENABLED, enabled).apply()
        _obsWebSocketEnabled.value = enabled
    }

    fun setObsWebSocketHost(host: String) {
        prefs.edit().putString(KEY_OBS_WS_HOST, host).apply()
        _obsWebSocketHost.value = host
    }

    fun setObsWebSocketPort(port: Int) {
        prefs.edit().putInt(KEY_OBS_WS_PORT, port).apply()
        _obsWebSocketPort.value = port
    }

    fun setObsWebSocketPassword(password: String) {
        prefs.edit().putString(KEY_OBS_WS_PASSWORD, password).apply()
        _obsWebSocketPassword.value = password
    }

    fun setObsWebSocketSourcePrefix(prefix: String) {
        prefs.edit().putString(KEY_OBS_WS_SOURCE_PREFIX, prefix).apply()
        _obsWebSocketSourcePrefix.value = prefix
    }

    fun setObsPauseSceneName(name: String) {
        prefs.edit().putString(KEY_OBS_PAUSE_SCENE, name).apply()
        _obsPauseSceneName.value = name
    }

    fun setObsAutoHideSourceNames(names: String) {
        prefs.edit().putString(KEY_OBS_AUTO_HIDE_SOURCES, names).apply()
        _obsAutoHideSourceNames.value = names
    }

    fun setObsStraightPoolOnlySourceNames(names: String) {
        prefs.edit().putString(KEY_OBS_STRAIGHT_ONLY_SOURCES, names).apply()
        _obsStraightPoolOnlySourceNames.value = names
    }

    fun setObsRegieTileEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OBS_REGIE_TILE_ENABLED, enabled).apply()
        _obsRegieTileEnabled.value = enabled
    }

    fun setObsWakeOnLanMac(mac: String) {
        prefs.edit().putString(KEY_OBS_WAKE_ON_LAN_MAC, mac).apply()
        _obsWakeOnLanMac.value = mac
    }

    fun setObsCameraSourceName(name: String) {
        prefs.edit().putString(KEY_OBS_CAMERA_SOURCE, name).apply()
        _obsCameraSourceName.value = name
    }

    fun setDefaultRace8Ball(raceTo: Int) {
        prefs.edit().putInt(KEY_RACE_8BALL, raceTo).apply()
        _defaultRace8Ball.value = raceTo
    }

    fun setDefaultRace9Ball(raceTo: Int) {
        prefs.edit().putInt(KEY_RACE_9BALL, raceTo).apply()
        _defaultRace9Ball.value = raceTo
    }

    fun setDefaultRace10Ball(raceTo: Int) {
        prefs.edit().putInt(KEY_RACE_10BALL, raceTo).apply()
        _defaultRace10Ball.value = raceTo
    }

    fun setDefaultRaceStraightPool(raceTo: Int) {
        prefs.edit().putInt(KEY_RACE_STRAIGHT_POOL, raceTo).apply()
        _defaultRaceStraightPool.value = raceTo
    }

    fun setDefaultClubId(clubId: String?) {
        prefs.edit().putString(KEY_DEFAULT_CLUB_ID, clubId).apply()
        _defaultClubId.value = clubId
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        _language.value = language
    }

    fun setTournamentEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TOURNAMENT_UNLOCKED, enabled).apply()
        _tournamentEnabled.value = enabled
    }

    fun setSaveTrainingToHistory(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_TRAINING_HISTORY, enabled).apply()
        _saveTrainingToHistory.value = enabled
    }

    fun setHistoryRetentionDays(days: Int) {
        val clamped = days.coerceIn(0, MAX_HISTORY_RETENTION_DAYS)
        prefs.edit().putInt(KEY_HISTORY_RETENTION_DAYS, clamped).apply()
        _historyRetentionDays.value = clamped
    }

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_OBS_WS_ENABLED = "obs_websocket_enabled"
        private const val KEY_OBS_WS_HOST = "obs_websocket_host"
        private const val KEY_OBS_WS_PORT = "obs_websocket_port"
        private const val KEY_OBS_WS_PASSWORD = "obs_websocket_password"
        private const val KEY_OBS_WS_SOURCE_PREFIX = "obs_websocket_source_prefix"
        private const val KEY_OBS_PAUSE_SCENE = "obs_pause_scene_name"
        private const val KEY_OBS_AUTO_HIDE_SOURCES = "obs_auto_hide_source_names"
        private const val KEY_OBS_STRAIGHT_ONLY_SOURCES = "obs_straight_only_source_names"
        private const val KEY_OBS_REGIE_TILE_ENABLED = "obs_regie_tile_enabled"
        private const val KEY_OBS_WAKE_ON_LAN_MAC = "obs_wake_on_lan_mac"
        private const val KEY_OBS_CAMERA_SOURCE = "obs_camera_source_name"
        private const val KEY_RACE_8BALL = "default_race_8ball"
        private const val KEY_RACE_9BALL = "default_race_9ball"
        private const val KEY_RACE_10BALL = "default_race_10ball"
        private const val KEY_RACE_STRAIGHT_POOL = "default_race_straight_pool"
        private const val KEY_DEFAULT_CLUB_ID = "default_club_id"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_TOURNAMENT_UNLOCKED = "tournament_mode_unlocked"
        private const val KEY_SAVE_TRAINING_HISTORY = "save_training_to_history"
        private const val KEY_HISTORY_RETENTION_DAYS = "history_retention_days"
        const val DEFAULT_HISTORY_RETENTION_DAYS = 365
        const val MAX_HISTORY_RETENTION_DAYS = 3650
        const val DEFAULT_OBS_WS_PORT = 4455
        const val DEFAULT_RACE_8BALL = 5
        const val DEFAULT_RACE_9BALL = 7
        const val DEFAULT_RACE_10BALL = 6
        const val DEFAULT_RACE_STRAIGHT_POOL = 100

        private fun systemDarkDefault(context: Context): Boolean {
            val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return nightMode == Configuration.UI_MODE_NIGHT_YES
        }

        /** New installs / already-migrated installs read KEY_APP_THEME directly. An existing
         * install that only ever set the old boolean dark-mode toggle falls back to it once,
         * without needing a Room-style migration since this is just SharedPreferences. */
        private fun loadAppTheme(prefs: android.content.SharedPreferences, context: Context): AppTheme {
            prefs.getString(KEY_APP_THEME, null)?.let { name ->
                return runCatching { AppTheme.valueOf(name) }.getOrDefault(AppTheme.LIGHT)
            }
            if (prefs.contains(KEY_DARK_MODE)) {
                return if (prefs.getBoolean(KEY_DARK_MODE, false)) AppTheme.DARK else AppTheme.LIGHT
            }
            return if (systemDarkDefault(context)) AppTheme.DARK else AppTheme.LIGHT
        }
    }
}
