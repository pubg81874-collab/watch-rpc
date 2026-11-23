package com.example.data.discord

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.local.RpcPreferencesRepository
import com.example.model.ConnectionState
import com.example.model.RpcSettings
import com.example.model.TimelineDisplayMode
import com.example.model.WatchPresenceConfig
import com.example.service.DiscordRpcService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DiscordRpcManager(
    private val appContext: Context,
    private val gatewayClient: DiscordGatewayClient,
    private val preferencesRepository: RpcPreferencesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _config = MutableStateFlow(WatchPresenceConfig())
    val config: StateFlow<WatchPresenceConfig> = _config.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = gatewayClient.connectionState
    val lastSentPayloadJson: StateFlow<String> = gatewayClient.lastSentPayloadJson
    val gatewayLogs: StateFlow<List<String>> = gatewayClient.gatewayLogs

    private var currentSettings: RpcSettings = RpcSettings()
    private var tickerJob: Job? = null
    private var saveDraftJob: Job? = null

    init {
        scope.launch {
            preferencesRepository.savedDraftFlow.first()?.let { saved ->
                _config.value = saved
            }
        }
        scope.launch {
            preferencesRepository.settingsFlow.collect { settings ->
                currentSettings = settings
            }
        }
        startPlaybackTicker()
    }

    fun updateConfig(
        pushIfConnected: Boolean = false,
        transform: (WatchPresenceConfig) -> WatchPresenceConfig,
    ) {
        val updated = transform(_config.value)
        _config.value = updated
        persistDraftDebounced(updated)
        if (pushIfConnected && connectionState.value is ConnectionState.Connected) {
            gatewayClient.updatePresence(currentSettings, updated)
            refreshServiceNotification()
        }
    }

    fun setConfig(newConfig: WatchPresenceConfig, pushIfConnected: Boolean = true) {
        _config.value = newConfig
        persistDraftDebounced(newConfig)
        if (pushIfConnected && connectionState.value is ConnectionState.Connected) {
            gatewayClient.updatePresence(currentSettings, newConfig)
            refreshServiceNotification()
        }
    }

    fun togglePlayPause() {
        val current = _config.value
        val nextPlaying = !current.isPlaying
        val nextMode = if (nextPlaying && current.totalSeconds > 0L) {
            TimelineDisplayMode.PROGRESS_BAR
        } else {
            TimelineDisplayMode.ELAPSED_WITH_EPISODE
        }
        updateConfig(pushIfConnected = true) {
            it.copy(isPlaying = nextPlaying, timelineMode = nextMode)
        }
    }

    fun seekTo(seconds: Long) {
        val clamped = seconds.coerceAtLeast(0L)
        updateConfig(pushIfConnected = true) { cfg ->
            val max = if (cfg.totalSeconds > 0L) cfg.totalSeconds else clamped
            cfg.copy(elapsedSeconds = clamped.coerceAtMost(max))
        }
    }

    fun connectAndBroadcast() {
        scope.launch {
            val latestSettings = preferencesRepository.settingsFlow.first()
            currentSettings = latestSettings
            gatewayClient.connect(latestSettings, _config.value)
            if (latestSettings.keepAliveNotification && latestSettings.token.isNotBlank()) {
                startForegroundKeepAlive()
            }
        }
    }

    fun pushUpdateNow() {
        scope.launch {
            val latestSettings = preferencesRepository.settingsFlow.first()
            currentSettings = latestSettings
            if (connectionState.value is ConnectionState.Connected) {
                gatewayClient.updatePresence(latestSettings, _config.value)
                refreshServiceNotification()
            } else {
                connectAndBroadcast()
            }
        }
    }

    fun disconnect() {
        gatewayClient.disconnect(clearStatus = true)
        stopForegroundKeepAlive()
    }

    fun buildPreviewJson(): String =
        gatewayClient.buildPreviewPayloadJson(currentSettings, _config.value)

    private fun startPlaybackTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val cfg = _config.value
                if (cfg.isPlaying) {
                    val next = cfg.elapsedSeconds + 1L
                    if (cfg.totalSeconds > 0L && next >= cfg.totalSeconds) {
                        _config.update {
                            it.copy(
                                elapsedSeconds = cfg.totalSeconds,
                                isPlaying = false,
                                timelineMode = TimelineDisplayMode.ELAPSED_WITH_EPISODE,
                            )
                        }
                        if (connectionState.value is ConnectionState.Connected) {
                            gatewayClient.updatePresence(currentSettings, _config.value)
                        }
                    } else {
                        _config.update { it.copy(elapsedSeconds = next) }
                    }
                }
            }
        }
    }

    private fun persistDraftDebounced(config: WatchPresenceConfig) {
        saveDraftJob?.cancel()
        saveDraftJob = scope.launch(Dispatchers.IO) {
            delay(350L)
            preferencesRepository.saveDraft(config)
        }
    }

    private fun startForegroundKeepAlive() {
        runCatching {
            val intent = Intent(appContext, DiscordRpcService::class.java).apply {
                action = DiscordRpcService.ACTION_START
            }
            ContextCompat.startForegroundService(appContext, intent)
        }
    }

    private fun refreshServiceNotification() {
        if (!currentSettings.keepAliveNotification) return
        runCatching {
            val intent = Intent(appContext, DiscordRpcService::class.java).apply {
                action = DiscordRpcService.ACTION_REFRESH
            }
            appContext.startService(intent)
        }
    }

    private fun stopForegroundKeepAlive() {
        runCatching {
            appContext.stopService(Intent(appContext, DiscordRpcService::class.java))
        }
    }
}
