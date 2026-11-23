package com.example.data.discord

import com.example.model.ConnectionState
import com.example.model.DiscordUserProfile
import com.example.model.RpcSettings
import com.example.model.WatchPresenceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiscordGatewayClient(
    private val httpClient: OkHttpClient,
    private val assetResolver: DiscordExternalAssetResolver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _lastSentPayloadJson = MutableStateFlow("")
    val lastSentPayloadJson: StateFlow<String> = _lastSentPayloadJson.asStateFlow()

    private val _gatewayLogs = MutableStateFlow<List<String>>(emptyList())
    val gatewayLogs: StateFlow<List<String>> = _gatewayLogs.asStateFlow()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    @Volatile private var sequenceNumber: Int? = null
    @Volatile private var sessionId: String? = null
    @Volatile private var resumeGatewayUrl: String? = null
    @Volatile private var shouldStayConnected: Boolean = false

    private var activeSettings: RpcSettings = RpcSettings()
    private var activeConfig: WatchPresenceConfig = WatchPresenceConfig()

    fun connect(settings: RpcSettings, config: WatchPresenceConfig) {
        val token = settings.token.trim()
        if (token.isEmpty()) {
            _connectionState.value = ConnectionState.Error(
                "Add your Discord token in the Connect tab before going live."
            )
            appendLog("Connection blocked: token is empty")
            return
        }
        activeSettings = settings
        activeConfig = config
        shouldStayConnected = true
        reconnectJob?.cancel()
        openSocket(GATEWAY_URL, isResume = false)
    }

    fun updatePresence(settings: RpcSettings, config: WatchPresenceConfig) {
        activeSettings = settings
        activeConfig = config
        if (_connectionState.value !is ConnectionState.Connected) return

        scope.launch {
            sendPresencePayload(settings, config)
        }
    }

    fun disconnect(clearStatus: Boolean = true) {
        shouldStayConnected = false
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        heartbeatJob = null
        val ws = webSocket
        webSocket = null
        if (ws != null) {
            if (clearStatus) {
                runCatching { ws.send(buildClearPresencePayload(activeSettings).toString()) }
            }
            runCatching { ws.close(1000, "User disconnected") }
        }
        sessionId = null
        sequenceNumber = null
        resumeGatewayUrl = null
        _connectionState.value = ConnectionState.Disconnected
        appendLog("Disconnected from Discord Gateway")
    }

    fun buildPreviewPayloadJson(settings: RpcSettings, config: WatchPresenceConfig): String {
        val payload = buildPresenceUpdateJson(
            settings = settings,
            config = config,
            resolvedLargeImage = config.posterUrl.trim().takeIf { it.isNotEmpty() },
            resolvedSmallImage = if (config.isPlaying) {
                settings.playBadgeUrl.trim().takeIf { it.isNotEmpty() }
            } else {
                settings.pauseBadgeUrl.trim().takeIf { it.isNotEmpty() }
            },
        )
        return payload.toString(2)
    }

    private fun openSocket(url: String, isResume: Boolean) {
        heartbeatJob?.cancel()
        webSocket?.cancel()
        _connectionState.value = ConnectionState.Connecting(
            if (isResume) "Resuming Discord session..." else "Connecting to Discord Gateway..."
        )
        appendLog(if (isResume) "Opening Gateway resume socket..." else "Connecting to Discord Gateway v10...")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            .build()

        webSocket = httpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    appendLog("WebSocket opened (HTTP ${response.code})")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleGatewayFrame(text, isResume)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    appendLog("Gateway closing: $code $reason")
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    heartbeatJob?.cancel()
                    handleSocketClosure(code, reason)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    heartbeatJob?.cancel()
                    val msg = t.message ?: "Network error"
                    appendLog("Gateway failure: $msg")
                    if (shouldStayConnected) {
                        scheduleReconnect(isResume = sessionId != null)
                    } else {
                        _connectionState.value = ConnectionState.Error(msg)
                    }
                }
            },
        )
    }

    private fun handleGatewayFrame(text: String, expectResume: Boolean) {
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return
        val op = json.optInt("op", -1)
        if (!json.isNull("s")) {
            sequenceNumber = json.optInt("s")
        }
        val eventName = if (json.isNull("t")) null else json.optString("t")

        when (op) {
            10 -> { // HELLO
                val intervalMs = json.optJSONObject("d")?.optLong("heartbeat_interval", 41250L) ?: 41250L
                appendLog("Received HELLO (heartbeat ${intervalMs}ms)")
                startHeartbeatLoop(intervalMs)
                if (expectResume && sessionId != null) {
                    sendResume()
                } else {
                    sendIdentify()
                }
            }

            1 -> { // Server requested immediate heartbeat
                sendHeartbeat()
            }

            11 -> { // HEARTBEAT_ACK
                _connectionState.update { state ->
                    if (state is ConnectionState.Connected) {
                        state.copy(lastHeartbeatAckMs = System.currentTimeMillis())
                    } else {
                        state
                    }
                }
            }

            7 -> { // RECONNECT
                appendLog("Gateway requested RECONNECT (OP 7)")
                scheduleReconnect(isResume = true)
            }

            9 -> { // INVALID_SESSION
                val resumable = json.optBoolean("d", false)
                appendLog("Invalid session (OP 9, resumable=$resumable)")
                if (!resumable) {
                    sessionId = null
                    sequenceNumber = null
                }
                scheduleReconnect(isResume = resumable)
            }

            0 -> { // DISPATCH
                when (eventName) {
                    "READY" -> {
                        val d = json.optJSONObject("d")
                        sessionId = d?.optString("session_id")?.takeIf { it.isNotBlank() }
                        resumeGatewayUrl = d?.optString("resume_gateway_url")?.takeIf { it.isNotBlank() }
                        val userObj = d?.optJSONObject("user")
                        val profile = DiscordUserProfile(
                            id = userObj?.optString("id").orEmpty(),
                            username = userObj?.optString("username").orEmpty().ifBlank { "Discord User" },
                            globalName = userObj?.optString("global_name")?.takeIf { it != "null" && it.isNotBlank() },
                            avatarHash = userObj?.optString("avatar")?.takeIf { it != "null" && it.isNotBlank() },
                        )
                        _connectionState.value = ConnectionState.Connected(
                            user = profile,
                            connectedAtMs = System.currentTimeMillis(),
                            lastHeartbeatAckMs = System.currentTimeMillis(),
                        )
                        appendLog("Authenticated as @${profile.username}")
                        scope.launch {
                            sendPresencePayload(activeSettings, activeConfig)
                        }
                    }

                    "RESUMED" -> {
                        appendLog("Session resumed cleanly")
                        scope.launch {
                            sendPresencePayload(activeSettings, activeConfig)
                        }
                    }
                }
            }
        }
    }

    private fun startHeartbeatLoop(intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            delay((intervalMs * 0.35).toLong().coerceAtLeast(1500L))
            while (isActive && shouldStayConnected) {
                sendHeartbeat()
                delay(intervalMs)
            }
        }
    }

    private fun sendHeartbeat() {
        val frame = JSONObject().apply {
            put("op", 1)
            put("d", sequenceNumber ?: JSONObject.NULL)
        }
        webSocket?.send(frame.toString())
    }

    private fun sendIdentify() {
        _connectionState.value = ConnectionState.Connecting("Authenticating with Discord...")
        val properties = JSONObject().apply {
            put("os", "Android")
            put("browser", "Discord Android")
            put("device", "Android")
        }
        val d = JSONObject().apply {
            put("token", activeSettings.token.trim())
            put("capabilities", 65)
            put("properties", properties)
            put("compress", false)
            put("presence", buildPresenceDataObject(activeSettings, activeConfig, null, null))
        }
        val payload = JSONObject().apply {
            put("op", 2)
            put("d", d)
        }
        appendLog("Sent IDENTIFY (OP 2)")
        webSocket?.send(payload.toString())
    }

    private fun sendResume() {
        val sid = sessionId ?: return sendIdentify()
        val payload = JSONObject().apply {
            put("op", 6)
            put(
                "d",
                JSONObject().apply {
                    put("token", activeSettings.token.trim())
                    put("session_id", sid)
                    put("seq", sequenceNumber ?: 0)
                },
            )
        }
        appendLog("Sent RESUME (OP 6)")
        webSocket?.send(payload.toString())
    }

    private suspend fun sendPresencePayload(settings: RpcSettings, config: WatchPresenceConfig) {
        val appId = settings.applicationId.trim().ifEmpty { RpcSettings.DEFAULT_APPLICATION_ID }
        val resolvedPoster = assetResolver.resolveAsset(settings.token, appId, config.posterUrl)
        val rawSmallBadge = if (config.isPlaying) settings.playBadgeUrl else settings.pauseBadgeUrl
        val resolvedSmall = assetResolver.resolveAsset(settings.token, appId, rawSmallBadge)

        val payload = buildPresenceUpdateJson(
            settings = settings,
            config = config,
            resolvedLargeImage = resolvedPoster,
            resolvedSmallImage = resolvedSmall,
        )
        val serialized = payload.toString()
        val sent = webSocket?.send(serialized) == true
        _lastSentPayloadJson.value = payload.toString(2)
        if (sent) {
            val modeDesc = if (config.showsProgressBar) "Progress Bar" else "Paused/Elapsed"
            appendLog("Sent OP 3 Watching '${config.mediaTitle.ifBlank { "Media" }}' ($modeDesc)")
        } else {
            appendLog("Failed to send OP 3 presence frame (socket not open)")
        }
    }

    private fun handleSocketClosure(code: Int, reason: String) {
        if (!shouldStayConnected) {
            _connectionState.value = ConnectionState.Disconnected
            return
        }
        when (code) {
            4004 -> {
                shouldStayConnected = false
                _connectionState.value = ConnectionState.Error(
                    "Authentication failed (4004). Verify your Discord token in Connect."
                )
                appendLog("Auth rejected (4004): invalid token")
            }

            4010, 4011, 4012, 4013, 4014 -> {
                shouldStayConnected = false
                _connectionState.value = ConnectionState.Error("Discord rejected connection ($code: $reason)")
            }

            else -> {
                scheduleReconnect(isResume = sessionId != null)
            }
        }
    }

    private fun scheduleReconnect(isResume: Boolean) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            _connectionState.value = ConnectionState.Connecting("Reconnecting in 3s...")
            delay(3000L)
            if (!shouldStayConnected) return@launch
            val url = if (isResume && !resumeGatewayUrl.isNullOrBlank()) {
                "${resumeGatewayUrl!!.trimEnd('/')}/?v=10&encoding=json"
            } else {
                GATEWAY_URL
            }
            openSocket(url, isResume = isResume)
        }
    }

    private fun buildPresenceUpdateJson(
        settings: RpcSettings,
        config: WatchPresenceConfig,
        resolvedLargeImage: String?,
        resolvedSmallImage: String?,
    ): JSONObject = JSONObject().apply {
        put("op", 3)
        put(
            "d",
            buildPresenceDataObject(
                settings = settings,
                config = config,
                resolvedLargeImage = resolvedLargeImage,
                resolvedSmallImage = resolvedSmallImage,
            ),
        )
    }

    private fun buildPresenceDataObject(
        settings: RpcSettings,
        config: WatchPresenceConfig,
        resolvedLargeImage: String?,
        resolvedSmallImage: String?,
    ): JSONObject {
        val nowMs = System.currentTimeMillis()
        val appId = settings.applicationId.trim().ifEmpty { RpcSettings.DEFAULT_APPLICATION_ID }
        val activityName = config.mediaTitle.trim().ifEmpty { "WatchRPC" }

        val activity = JSONObject().apply {
            put("name", activityName)
            put("type", 3) // WATCHING
            put("application_id", appId)

            val details = config.effectiveDetails
            if (details.isNotBlank()) {
                put("details", details.take(128))
            }
            val state = config.stateSubtitle.trim()
            if (state.isNotBlank()) {
                put("state", state.take(128))
            }

            val timestamps = JSONObject()
            if (config.showsProgressBar) {
                val startMs = (nowMs - config.elapsedSeconds * 1000L).coerceAtLeast(0L)
                val remainingMs = (config.totalSeconds - config.elapsedSeconds).coerceAtLeast(1L) * 1000L
                val endMs = nowMs + remainingMs
                timestamps.put("start", startMs)
                timestamps.put("end", endMs)
            } else {
                val startMs = (nowMs - config.elapsedSeconds * 1000L).coerceAtLeast(0L)
                timestamps.put("start", startMs)
            }
            put("timestamps", timestamps)

            val assets = JSONObject()
            if (!resolvedLargeImage.isNullOrBlank()) {
                assets.put("large_image", resolvedLargeImage)
            }
            val largeText = config.episodeTag.trim().ifEmpty { activityName }
            if (largeText.isNotBlank()) {
                assets.put("large_text", largeText.take(128))
            }
            if (!resolvedSmallImage.isNullOrBlank()) {
                assets.put("small_image", resolvedSmallImage)
            }
            assets.put("small_text", if (config.isPlaying) "Playing" else "Paused")
            put("assets", assets)

            val buttonLabels = JSONArray()
            val buttonUrls = JSONArray()

            val b1Label = config.button1Label.trim()
            val b1Url = normalizeUrl(config.button1Url)
            if (b1Label.isNotBlank() && b1Url != null) {
                buttonLabels.put(b1Label.take(32))
                buttonUrls.put(b1Url)
            }
            if (config.button2Enabled) {
                val b2Label = config.button2Label.trim()
                val b2Url = normalizeUrl(config.button2Url)
                if (b2Label.isNotBlank() && b2Url != null) {
                    buttonLabels.put(b2Label.take(32))
                    buttonUrls.put(b2Url)
                }
            }
            if (buttonLabels.length() > 0) {
                put("buttons", buttonLabels)
                put("metadata", JSONObject().put("button_urls", buttonUrls))
            }
        }

        return JSONObject().apply {
            put("since", 0)
            put("activities", JSONArray().put(activity))
            put("status", settings.onlineStatus.apiValue)
            put("afk", false)
        }
    }

    private fun buildClearPresencePayload(settings: RpcSettings): JSONObject = JSONObject().apply {
        put("op", 3)
        put(
            "d",
            JSONObject().apply {
                put("since", 0)
                put("activities", JSONArray())
                put("status", settings.onlineStatus.apiValue)
                put("afk", false)
            },
        )
    }

    private fun normalizeUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun appendLog(message: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val line = "[$stamp] $message"
        _gatewayLogs.update { existing -> (existing + line).takeLast(40) }
    }

    companion object {
        private const val GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"
    }
}
