package info.rbuck.billiardscoreboard.obs

import android.util.Base64
import android.util.Log
import info.rbuck.billiardscoreboard.data.AppSettingsRepository
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

enum class ObsWebSocketStatus { DISCONNECTED, CONNECTING, CONNECTED, AUTH_FAILED, ERROR }

/** Idle/active/paused for both the record and stream outputs - derived from obs-websocket's
 * `outputActive` + `outputState` fields (see [ObsWebSocketClient.parseOutputState]). */
enum class ObsOutputState { IDLE, ACTIVE, PAUSED }

data class ObsOutputStatus(val state: ObsOutputState = ObsOutputState.IDLE, val timecode: String? = null)

/**
 * Connects OUT to OBS's own built-in obs-websocket server (bundled since OBS Studio 28 - Tools >
 * WebSocket Server Settings) and writes each live-score value straight into named OBS Text
 * sources via `SetInputSettings` whenever the match state changes. The app is the client here,
 * OBS is the server, so nothing is ever broadcast or listening on this device's side of the
 * connection.
 *
 * Text source names expected in the OBS scene (create these once - see docs/obs-scripts/ANLEITUNG.md):
 * name1, name2, club1, club2, score1, score2, run1, run2, highestbreak1, highestbreak2,
 * innings1, innings2, raceto, discipline, turn1, turn2 (turn1/turn2 get a filled/empty circle
 * glyph as plain text, not HTML - there's no browser to render HTML on this path).
 *
 * If several devices push into the SAME OBS instance (e.g. several tables shown in one scene), each
 * device must set a distinct "source prefix" in Settings so their SetInputSettings calls target
 * different sources - e.g. prefix "tisch1" writes to "tisch1_name1" etc. instead of plain "name1".
 * One full set of text sources (with that prefix) must exist per table in the OBS scene.
 *
 * Club crest images are NOT sent over this connection - see docs/obs-scripts/club_crest_switcher.lua,
 * which switches local crest files on the OBS side based on the club1/club2 text instead.
 *
 * Transport is plain `ws://`, not `wss://` - obs-websocket has no native TLS support (see the
 * project's own SSL-Tunneling wiki page). The configured password is still required to do
 * anything, so an unauthorized device on the same Wi-Fi can't read or change anything even though
 * it could still passively sniff the traffic - there's no client-side fix available for that since
 * it's OBS's own server we're talking to.
 *
 * Also drives OBS's Record and Stream outputs (start/stop/pause) - see [recordStatus],
 * [streamStatus], [startRecord] etc. below. Subscribes to the Outputs event category so
 * [recordStatus]/[streamStatus] reflect what OBS is actually doing (including changes made
 * directly in OBS, e.g. by a human at the PC), not just what this app last requested.
 *
 * obs-websocket's Stream output has no native pause. [pauseStream]/[resumeStream] fake one by
 * switching the OBS program scene to a configured "pause" scene and back - the stream itself
 * keeps running throughout (no YouTube reconnect), see [AppSettingsRepository.obsPauseSceneName].
 */
class ObsWebSocketClient(
    private val liveScoreRepository: LiveScoreRepository,
    private val settingsRepository: AppSettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val httpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    private var webSocket: WebSocket? = null
    private var identified = false
    private var reconnectJob: Job? = null
    private var currentTarget: ConnectionTarget? = null

    private val _status = MutableStateFlow(ObsWebSocketStatus.DISCONNECTED)
    val status: StateFlow<ObsWebSocketStatus> = _status

    /** The underlying exception message for the last ERROR, so Settings can show more than just "failed" - e.g. a
     * blocked-cleartext-traffic error looks completely different to fix than a plain connection-refused one. */
    private val _lastErrorDetail = MutableStateFlow<String?>(null)
    val lastErrorDetail: StateFlow<String?> = _lastErrorDetail

    private val _recordStatus = MutableStateFlow(ObsOutputStatus())
    val recordStatus: StateFlow<ObsOutputStatus> = _recordStatus

    private val _streamStatus = MutableStateFlow(ObsOutputStatus())
    val streamStatus: StateFlow<ObsOutputStatus> = _streamStatus

    /** True while the stream is "paused" via [pauseStream] (a scene switch - see class doc).
     * Independent of [streamStatus], which only reflects the real obs-websocket output state. */
    private val _streamPaused = MutableStateFlow(false)
    val streamPaused: StateFlow<Boolean> = _streamPaused

    /** The program scene [pauseStream] switched away from, so [resumeStream] can switch back. */
    private var sceneBeforePause: String? = null

    /** When an established (identified) connection dropped, so the next successful identify can tell
     * a real outage (PC sleep etc. - restart the camera stream, which OBS tends to leave frozen on
     * wake) from a brief blip (do nothing). Null unless currently disconnected after being up. */
    private var establishedConnectionLostAtMs: Long? = null

    /** requestId -> callback, awaiting that request's `RequestResponse` (op 7). [sendRequest] is
     * called from whatever thread the UI/caller is on (not necessarily [scope]), while responses
     * arrive on OkHttp's own WebSocket callback thread - hence a concurrent map, not a plain one. */
    private val pendingRequests = java.util.concurrent.ConcurrentHashMap<String, (JsonObject) -> Unit>()

    init {
        scope.launch {
            combine(
                settingsRepository.obsWebSocketEnabled,
                settingsRepository.obsWebSocketHost,
                settingsRepository.obsWebSocketPort,
                settingsRepository.obsWebSocketPassword,
                settingsRepository.obsWebSocketSourcePrefix,
            ) { enabled, host, port, password, sourcePrefix -> ConnectionTarget(enabled, host, port, password, sourcePrefix) }
                .distinctUntilChanged()
                .collect { target -> switchTo(target) }
        }
        scope.launch {
            liveScoreRepository.state.collect { state -> pushState(state) }
        }
        scope.launch {
            liveScoreRepository.state.map { it.toVisibility() }.distinctUntilChanged().collect { visibility -> applyAutoHide(visibility) }
        }
    }

    /** Straight Pool is the only discipline that tracks run/highest-break/innings (see
     * [LiveScoreState] and both match screens' publish calls) - so "currentRun set" doubles as a
     * cheap, already-there signal for "this is a 14.1 match" without a dedicated enum. */
    private fun LiveScoreState?.toVisibility() = AutoHideVisibility(matchActive = this != null, straightPoolActive = this?.player1?.currentRun != null)

    private data class AutoHideVisibility(val matchActive: Boolean, val straightPoolActive: Boolean)

    private data class ConnectionTarget(
        val enabled: Boolean,
        val host: String,
        val port: Int,
        val password: String,
        val sourcePrefix: String,
    )

    private fun switchTo(target: ConnectionTarget) {
        currentTarget = target
        reconnectJob?.cancel()
        webSocket?.close(1000, "Settings changed")
        webSocket = null
        identified = false
        establishedConnectionLostAtMs = null
        clearLiveOutputState()
        if (!target.enabled || target.host.isBlank()) {
            _status.value = ObsWebSocketStatus.DISCONNECTED
            return
        }
        connect(target)
    }

    private fun connect(target: ConnectionTarget) {
        _status.value = ObsWebSocketStatus.CONNECTING
        _lastErrorDetail.value = null
        val request = Request.Builder().url("ws://${target.host}:${target.port}").build()
        webSocket = httpClient.newWebSocket(request, Listener(target))
    }

    private fun scheduleReconnect(target: ConnectionTarget) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(RECONNECT_DELAY_MS)
            if (currentTarget == target) connect(target)
        }
    }

    private inner class Listener(private val target: ConnectionTarget) : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            val message = try {
                json.parseToJsonElement(text).jsonObject
            } catch (e: Exception) {
                return
            }
            val op = message["op"]?.jsonPrimitive?.content?.toIntOrNull() ?: return
            val data = message["d"]?.jsonObject ?: JsonObject(emptyMap())
            when (op) {
                OP_HELLO -> handleHello(webSocket, data)
                OP_IDENTIFIED -> {
                    identified = true
                    _status.value = ObsWebSocketStatus.CONNECTED
                    pushState(liveScoreRepository.state.value)
                    refreshOutputStatus()
                    applyAutoHide(liveScoreRepository.state.value.toVisibility())
                    maybeRestartCameraAfterOutage()
                }
                OP_EVENT -> handleEvent(data)
                OP_REQUEST_RESPONSE -> handleRequestResponse(data)
            }
        }

        private fun handleEvent(data: JsonObject) {
            val eventType = data["eventType"]?.jsonPrimitive?.content ?: return
            val eventData = data["eventData"]?.jsonObject ?: JsonObject(emptyMap())
            when (eventType) {
                "RecordStateChanged" -> _recordStatus.value = parseOutputStatus(eventData)
                "StreamStateChanged" -> {
                    val newStatus = parseOutputStatus(eventData)
                    _streamStatus.value = newStatus
                    // The real output stopped/started - our faked scene-switch "pause" no longer applies.
                    if (newStatus.state != ObsOutputState.ACTIVE) {
                        _streamPaused.value = false
                        sceneBeforePause = null
                    }
                }
            }
        }

        private fun handleRequestResponse(data: JsonObject) {
            val requestId = data["requestId"]?.jsonPrimitive?.content ?: return
            val callback = pendingRequests.remove(requestId) ?: return
            callback(data["responseData"]?.jsonObject ?: JsonObject(emptyMap()))
        }

        private fun handleHello(webSocket: WebSocket, data: JsonObject) {
            val authObj = data["authentication"]?.jsonObject
            val authString = authObj?.let {
                val challenge = it["challenge"]?.jsonPrimitive?.content
                val salt = it["salt"]?.jsonPrimitive?.content
                if (challenge != null && salt != null) computeAuthResponse(target.password, salt, challenge) else null
            }
            val identify = buildJsonObject {
                put("op", OP_IDENTIFY)
                putJsonObject("d") {
                    put("rpcVersion", 1)
                    if (authString != null) put("authentication", authString)
                    // Outputs category only - covers RecordStateChanged/StreamStateChanged, which is
                    // all this client needs events for. Everything else stays off (scoreboard pushes
                    // are one-way, fire-and-forget, and need no events at all).
                    put("eventSubscriptions", EVENT_SUB_OUTPUTS)
                }
            }
            webSocket.send(json.encodeToString(JsonObject.serializer(), identify))
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "obs-websocket connection failed", t)
            val wasIdentified = identified
            identified = false
            if (currentTarget != target) return
            _status.value = ObsWebSocketStatus.ERROR
            _lastErrorDetail.value = t.message ?: t.javaClass.simpleName
            noteConnectionLost(wasIdentified)
            clearLiveOutputState()
            scheduleReconnect(target)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            val wasIdentified = identified
            identified = false
            if (currentTarget != target) return
            noteConnectionLost(wasIdentified)
            if (code == CLOSE_CODE_AUTH_FAILED) {
                // Wrong password - retrying immediately would just fail again; wait for the user to fix it
                // in Settings, which re-triggers switchTo() on its own.
                _status.value = ObsWebSocketStatus.AUTH_FAILED
                clearLiveOutputState()
                return
            }
            _status.value = ObsWebSocketStatus.DISCONNECTED
            clearLiveOutputState()
            scheduleReconnect(target)
        }
    }

    /** Called whenever the connection is lost for any reason (OBS PC off/asleep, network drop,
     * wrong password, ...) - Regie's record/stream status must not keep showing the last known
     * value as if it were still current, since the app has no way to know what's actually
     * happening on the OBS side once the link is gone. [pendingRequests] callbacks would otherwise
     * also leak (never resolved) and never fire once the connection is re-established with new,
     * unrelated request ids. */
    private fun clearLiveOutputState() {
        pendingRequests.clear()
        _recordStatus.value = ObsOutputStatus()
        _streamStatus.value = ObsOutputStatus()
        _streamPaused.value = false
        sceneBeforePause = null
    }

    private fun computeAuthResponse(password: String, salt: String, challenge: String): String {
        val secret = sha256Base64(password + salt)
        return sha256Base64(secret + challenge)
    }

    private fun sha256Base64(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private fun pushState(state: LiveScoreState?) {
        val ws = webSocket ?: return
        if (!identified) return
        val prefix = currentTarget?.sourcePrefix.orEmpty().let { if (it.isBlank()) "" else "${it.trim()}_" }
        val fields = if (state == null) {
            FIELD_NAMES.associateWith { "" }
        } else {
            mapOf(
                "name1" to state.player1.name,
                "name2" to state.player2.name,
                "club1" to state.player1.club,
                "club2" to state.player2.club,
                "score1" to state.player1.score.toString(),
                "score2" to state.player2.score.toString(),
                "run1" to (state.player1.currentRun?.toString() ?: ""),
                "run2" to (state.player2.currentRun?.toString() ?: ""),
                "highestbreak1" to (state.player1.highestBreak?.toString() ?: ""),
                "highestbreak2" to (state.player2.highestBreak?.toString() ?: ""),
                "innings1" to (state.player1.innings?.toString() ?: ""),
                "innings2" to (state.player2.innings?.toString() ?: ""),
                "raceto" to state.raceTo.toString(),
                "discipline" to state.discipline,
                "turn1" to (if (state.player1.atTurn) "●" else "○"),
                "turn2" to (if (state.player2.atTurn) "●" else "○"),
            )
        }
        val batch = buildJsonObject {
            put("op", OP_REQUEST_BATCH)
            putJsonObject("d") {
                put("requestId", UUID.randomUUID().toString())
                put("haltOnFailure", false)
                putJsonArray("requests") {
                    fields.forEach { (sourceName, value) ->
                        addJsonObject {
                            put("requestType", "SetInputSettings")
                            putJsonObject("requestData") {
                                put("inputName", prefix + sourceName)
                                putJsonObject("inputSettings") { put("text", value) }
                            }
                        }
                    }
                }
            }
        }
        ws.send(json.encodeToString(JsonObject.serializer(), batch))
    }

    // --- Single-request plumbing (Record/Stream control) ---------------------------------------

    /** Fires one obs-websocket Request (op 6). [onResponse] - if given - is called with the
     * response's `responseData` once the matching `RequestResponse` (op 7) arrives; dropped
     * silently if the connection is lost first (no timeout - these are all quick local-LAN calls
     * and the UI reflects reality via [recordStatus]/[streamStatus] regardless of whether a given
     * response makes it back). Not for the scoreboard push, which stays a single batched request. */
    private fun sendRequest(requestType: String, requestData: JsonObject? = null, onResponse: ((JsonObject) -> Unit)? = null) {
        val ws = webSocket ?: return
        if (!identified) return
        val requestId = UUID.randomUUID().toString()
        if (onResponse != null) pendingRequests[requestId] = onResponse
        val message = buildJsonObject {
            put("op", OP_REQUEST)
            putJsonObject("d") {
                put("requestType", requestType)
                put("requestId", requestId)
                if (requestData != null) put("requestData", requestData)
            }
        }
        ws.send(json.encodeToString(JsonObject.serializer(), message))
    }

    private fun refreshOutputStatus() {
        sendRequest("GetRecordStatus") { data -> _recordStatus.value = parseOutputStatus(data) }
        sendRequest("GetStreamStatus") { data -> _streamStatus.value = parseOutputStatus(data) }
    }

    /** Both `Get*Status` responses and `*StateChanged` events use the same field names
     * (`outputActive`, `outputState`, `outputTimecode` - the latter only on `Get*Status`), so one
     * parser covers both call sites. `outputState` (e.g. `OBS_WEBSOCKET_OUTPUT_PAUSED`) is the
     * authoritative signal for pause; `Get*Status` also has a plain `outputPaused` boolean, which
     * this checks too since events don't carry it. */
    private fun parseOutputStatus(data: JsonObject): ObsOutputStatus {
        val active = data["outputActive"]?.jsonPrimitive?.booleanOrNull ?: false
        val pausedFlag = data["outputPaused"]?.jsonPrimitive?.booleanOrNull ?: false
        val stateStr = data["outputState"]?.jsonPrimitive?.content ?: ""
        val paused = pausedFlag || stateStr.contains("PAUSE")
        val state = when {
            paused -> ObsOutputState.PAUSED
            active -> ObsOutputState.ACTIVE
            else -> ObsOutputState.IDLE
        }
        val timecode = data["outputTimecode"]?.jsonPrimitive?.content
        return ObsOutputStatus(state, timecode)
    }

    fun startRecord() = sendRequest("StartRecord")
    fun stopRecord() = sendRequest("StopRecord")
    fun pauseRecord() = sendRequest("PauseRecord")
    fun resumeRecord() = sendRequest("ResumeRecord")

    fun startStream() = sendRequest("StartStream")
    fun stopStream() = sendRequest("StopStream")

    /** Fakes a stream pause by switching OBS to [pauseSceneName] (see class doc) - remembers the
     * scene that was live so [resumeStream] can switch back. No-op if already paused, if the
     * scene name is blank, or if OBS is already showing that scene. */
    fun pauseStream(pauseSceneName: String) {
        if (pauseSceneName.isBlank() || _streamPaused.value) return
        sendRequest("GetCurrentProgramScene") { data ->
            val current = data["currentProgramSceneName"]?.jsonPrimitive?.content ?: return@sendRequest
            if (current == pauseSceneName) return@sendRequest
            sceneBeforePause = current
            sendRequest("SetCurrentProgramScene", buildJsonObject { put("sceneName", pauseSceneName) })
            _streamPaused.value = true
        }
    }

    fun resumeStream() {
        val previousScene = sceneBeforePause ?: return
        sendRequest("SetCurrentProgramScene", buildJsonObject { put("sceneName", previousScene) })
        _streamPaused.value = false
        sceneBeforePause = null
    }

    // --- Camera (media source) restart --------------------------------------------------------

    /** Restarts playback of an OBS media/ffmpeg source (an RTSP camera "Media Source"). Manual
     * counterpart of [maybeRestartCameraAfterOutage]; wired to the Regie "reload camera" button. */
    fun restartCameraSource() {
        val name = settingsRepository.obsCameraSourceName.value
        if (name.isNotBlank()) restartMediaSource(name)
    }

    private fun restartMediaSource(sourceName: String) {
        sendRequest(
            "TriggerMediaInputAction",
            buildJsonObject {
                put("inputName", sourceName)
                put("mediaAction", "OBS_WEBSOCKET_MEDIA_INPUT_ACTION_RESTART")
            },
        )
    }

    private fun noteConnectionLost(wasIdentified: Boolean) {
        if (wasIdentified && establishedConnectionLostAtMs == null) {
            establishedConnectionLostAtMs = System.currentTimeMillis()
        }
    }

    /** After the app reconnects, if the connection had been down long enough to look like a real
     * outage (PC sleep/wake), restart the camera source once - OBS keeps an RTSP camera's last
     * frame frozen on wake until its stream is restarted. Short blips (tablet Wi-Fi) are ignored
     * so a hiccup mid-broadcast doesn't cause an unnecessary camera reload. */
    private fun maybeRestartCameraAfterOutage() {
        val downMs = establishedConnectionLostAtMs?.let { System.currentTimeMillis() - it }
        establishedConnectionLostAtMs = null
        if (downMs == null || downMs < CAMERA_RESTART_MIN_OUTAGE_MS) return
        val name = settingsRepository.obsCameraSourceName.value
        if (name.isBlank()) return
        scope.launch {
            delay(CAMERA_RESTART_DELAY_MS)
            if (identified) restartMediaSource(name)
        }
    }

    // --- Auto-hide idle scoreboard sources ------------------------------------------------------

    /** Shows/hides two configured groups of top-level scene items whenever the open match's state
     * changes - [AppSettingsRepository.obsAutoHideSourceNames] (any match open) and
     * [AppSettingsRepository.obsStraightPoolOnlySourceNames] (only while that match is 14.1 Straight
     * Pool, e.g. an innings/highest-break "Statistik" group that doesn't apply to 8/9/10-Ball) -
     * so an idle scene doesn't sit there with a blank-but-visible scoreboard overlay. No-op if
     * nothing is configured. Best-effort: resolved fresh against the current program scene each
     * time (not cached), so it keeps working if the operator switches scenes between matches; a
     * source that isn't found in that scene is silently skipped rather than failing the others. */
    private fun applyAutoHide(visibility: AutoHideVisibility) {
        fun parseNames(value: String) = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val generalNames = parseNames(settingsRepository.obsAutoHideSourceNames.value)
        val straightNames = parseNames(settingsRepository.obsStraightPoolOnlySourceNames.value)
        if (generalNames.isEmpty() && straightNames.isEmpty()) return
        sendRequest("GetCurrentProgramScene") { sceneData ->
            val sceneName = sceneData["currentProgramSceneName"]?.jsonPrimitive?.content ?: return@sendRequest
            sendRequest("GetSceneItemList", buildJsonObject { put("sceneName", sceneName) }) { listData ->
                val itemsByName = listData["sceneItems"]?.jsonArray
                    ?.mapNotNull { it.jsonObject }
                    ?.associateBy { it["sourceName"]?.jsonPrimitive?.content }
                    ?: return@sendRequest
                fun setEnabled(names: List<String>, enabled: Boolean) {
                    names.forEach { name ->
                        val itemId = itemsByName[name]?.get("sceneItemId")?.jsonPrimitive?.content?.toIntOrNull() ?: return@forEach
                        sendRequest(
                            "SetSceneItemEnabled",
                            buildJsonObject {
                                put("sceneName", sceneName)
                                put("sceneItemId", itemId)
                                put("sceneItemEnabled", enabled)
                            },
                        )
                    }
                }
                setEnabled(generalNames, visibility.matchActive)
                setEnabled(straightNames, visibility.straightPoolActive)
            }
        }
    }

    companion object {
        private const val TAG = "ObsWebSocketClient"
        private const val OP_HELLO = 0
        private const val OP_IDENTIFY = 1
        private const val OP_IDENTIFIED = 2
        private const val OP_EVENT = 5
        private const val OP_REQUEST = 6
        private const val OP_REQUEST_RESPONSE = 7
        private const val OP_REQUEST_BATCH = 8
        /** obs-websocket `EventSubscription::Outputs` bit - covers RecordStateChanged/StreamStateChanged. */
        private const val EVENT_SUB_OUTPUTS = 64
        private const val CLOSE_CODE_AUTH_FAILED = 4009
        private const val RECONNECT_DELAY_MS = 5000L
        /** Outage shorter than this on reconnect is treated as a blip - no camera restart. */
        private const val CAMERA_RESTART_MIN_OUTAGE_MS = 45_000L
        /** Wait after reconnect before restarting the camera, to let OBS/the PC settle post-wake. */
        private const val CAMERA_RESTART_DELAY_MS = 4000L
        private val FIELD_NAMES = listOf(
            "name1", "name2", "club1", "club2", "score1", "score2", "run1", "run2",
            "highestbreak1", "highestbreak2", "innings1", "innings2", "raceto", "discipline",
            "turn1", "turn2",
        )
    }
}
