package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.WatchRpcApp
import com.example.data.local.RpcPresetEntity
import com.example.model.ConnectionState
import com.example.model.DiscordOnlineStatus
import com.example.model.MediaEpisode
import com.example.model.MediaSearchResult
import com.example.model.MediaType
import com.example.model.RpcSettings
import com.example.model.TimelineDisplayMode
import com.example.model.WatchPresenceConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainDestination(val route: String) {
    STUDIO("studio"),
    DISCOVER("discover"),
    PRESETS("presets"),
    CONNECT("connect"),
}

data class DiscoverUiState(
    val query: String = "",
    val selectedType: MediaType = MediaType.SERIES,
    val isLoading: Boolean = false,
    val results: List<MediaSearchResult> = emptyList(),
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
    val selectedShowForEpisodes: MediaSearchResult? = null,
    val isLoadingEpisodes: Boolean = false,
    val episodes: List<MediaEpisode> = emptyList(),
    val episodesError: String? = null,
    val selectedSeason: Int = 1,
)

class WatchRpcViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as WatchRpcApp
    private val rpcManager = app.rpcManager
    private val presetRepository = app.presetRepository
    private val preferencesRepository = app.preferencesRepository
    private val mediaCatalogRepository = app.mediaCatalogRepository

    val presenceConfig: StateFlow<WatchPresenceConfig> = rpcManager.config
    val connectionState: StateFlow<ConnectionState> = rpcManager.connectionState
    val gatewayLogs: StateFlow<List<String>> = rpcManager.gatewayLogs
    val lastSentPayloadJson: StateFlow<String> = rpcManager.lastSentPayloadJson

    val settings: StateFlow<RpcSettings> = preferencesRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = RpcSettings(),
    )

    val presets: StateFlow<List<RpcPresetEntity>> = presetRepository.presets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
    )

    private val _currentDestination = MutableStateFlow(MainDestination.STUDIO)
    val currentDestination: StateFlow<MainDestination> = _currentDestination.asStateFlow()

    private val _discoverState = MutableStateFlow(DiscoverUiState())
    val discoverState: StateFlow<DiscoverUiState> = _discoverState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    fun selectDestination(destination: MainDestination) {
        _currentDestination.value = destination
    }

    // --- Studio Editor Actions ---

    fun switchMediaType(type: MediaType) {
        rpcManager.updateConfig(pushIfConnected = false) { current ->
            val keepCustomBtn1 = current.button1Label.isNotBlank() &&
                current.button1Label != current.mediaType.defaultButton1
            val keepCustomBtn2 = current.button2Label.isNotBlank() &&
                current.button2Label != current.mediaType.defaultButton2
            current.copy(
                mediaType = type,
                button1Label = if (keepCustomBtn1) current.button1Label else type.defaultButton1,
                button2Label = if (keepCustomBtn2) current.button2Label else type.defaultButton2,
                button2Enabled = type.defaultButton2Enabled,
                episodeTag = if (type == MediaType.MOVIE) "" else current.episodeTag.ifBlank { "S1E1" },
            )
        }
    }

    fun updateField(transform: (WatchPresenceConfig) -> WatchPresenceConfig) {
        rpcManager.updateConfig(pushIfConnected = false, transform = transform)
    }

    fun togglePlayPause() {
        rpcManager.togglePlayPause()
    }

    fun setTimelineMode(mode: TimelineDisplayMode) {
        rpcManager.updateConfig(pushIfConnected = true) { current ->
            val playing = if (mode == TimelineDisplayMode.PROGRESS_BAR) true else current.isPlaying
            current.copy(timelineMode = mode, isPlaying = playing)
        }
    }

    fun seekTo(seconds: Long) {
        rpcManager.seekTo(seconds)
    }

    fun connectOrPushPresence() {
        if (settings.value.token.isBlank()) {
            _snackbarEvents.tryEmit("Enter your Discord token in Connect to go live.")
            _currentDestination.value = MainDestination.CONNECT
            return
        }
        if (presenceConfig.value.mediaTitle.isBlank()) {
            _snackbarEvents.tryEmit("Enter a movie, series, or anime title first.")
            return
        }
        rpcManager.pushUpdateNow()
        val label = if (connectionState.value is ConnectionState.Connected) {
            "Updated live Discord Rich Presence"
        } else {
            "Connecting to Discord Gateway..."
        }
        _snackbarEvents.tryEmit(label)
    }

    fun disconnectPresence() {
        rpcManager.disconnect()
        _snackbarEvents.tryEmit("Disconnected from Discord")
    }

    fun buildPreviewJson(): String = rpcManager.buildPreviewJson()

    // --- Preset Actions ---

    fun saveCurrentAsPreset(customName: String) {
        val cfg = presenceConfig.value
        val presetName = customName.trim().ifEmpty {
            buildString {
                append(cfg.mediaTitle.ifBlank { "Custom Presence" })
                if (cfg.episodeTag.isNotBlank()) {
                    append(" • ")
                    append(cfg.episodeTag)
                }
            }
        }
        viewModelScope.launch {
            presetRepository.savePreset(presetName, cfg)
            _snackbarEvents.tryEmit("Saved preset \"$presetName\"")
        }
    }

    fun loadPreset(preset: RpcPresetEntity, goLiveImmediately: Boolean) {
        val config = preset.toConfig()
        rpcManager.setConfig(config, pushIfConnected = true)
        _currentDestination.value = MainDestination.STUDIO
        if (goLiveImmediately && connectionState.value !is ConnectionState.Connected) {
            if (settings.value.token.isBlank()) {
                _snackbarEvents.tryEmit("Loaded \"${preset.label}\". Add token in Connect to broadcast.")
            } else {
                rpcManager.connectAndBroadcast()
                _snackbarEvents.tryEmit("Broadcasting \"${preset.label}\"")
            }
        } else {
            _snackbarEvents.tryEmit("Loaded \"${preset.label}\" into Studio")
        }
    }

    fun deletePreset(preset: RpcPresetEntity) {
        viewModelScope.launch {
            presetRepository.deletePreset(preset.id)
            _snackbarEvents.tryEmit("Removed preset \"${preset.label}\"")
        }
    }

    // --- Discover / Media Search Actions ---

    fun updateSearchQuery(query: String) {
        _discoverState.update { it.copy(query = query) }
    }

    fun selectSearchMediaType(type: MediaType) {
        val prev = _discoverState.value
        _discoverState.update {
            it.copy(
                selectedType = type,
                results = emptyList(),
                errorMessage = null,
                hasSearched = false,
            )
        }
        if (prev.query.isNotBlank()) {
            performSearch()
        }
    }

    fun performSearch() {
        val current = _discoverState.value
        val query = current.query.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            _discoverState.update {
                it.copy(isLoading = true, errorMessage = null, hasSearched = true)
            }
            val result = mediaCatalogRepository.search(query, current.selectedType)
            result.fold(
                onSuccess = { items ->
                    _discoverState.update {
                        it.copy(isLoading = false, results = items, errorMessage = null)
                    }
                },
                onFailure = { err ->
                    _discoverState.update {
                        it.copy(
                            isLoading = false,
                            results = emptyList(),
                            errorMessage = err.message ?: "Unable to fetch media catalog.",
                        )
                    }
                },
            )
        }
    }

    fun onMediaResultSelected(item: MediaSearchResult) {
        if (item.mediaType == MediaType.MOVIE) {
            val runtimeMins = item.runtimeMinutes ?: 120
            val subtitle = buildString {
                if (item.year.isNotBlank()) {
                    append(item.year)
                    append(" • ")
                }
                append("$runtimeMins minutes")
            }
            val config = WatchPresenceConfig(
                mediaType = MediaType.MOVIE,
                mediaTitle = item.title,
                detailsTitle = item.title,
                stateSubtitle = subtitle,
                episodeTag = "",
                posterUrl = item.posterUrl,
                isPlaying = true,
                timelineMode = TimelineDisplayMode.PROGRESS_BAR,
                elapsedSeconds = 0L,
                totalSeconds = runtimeMins * 60L,
                button1Label = "Watch Movie",
                button1Url = item.watchUrl,
                button2Enabled = false,
                button2Label = "View Info",
                button2Url = "",
            )
            rpcManager.setConfig(config, pushIfConnected = true)
            _currentDestination.value = MainDestination.STUDIO
            _snackbarEvents.tryEmit("Loaded movie \"${item.title}\"")
        } else {
            openEpisodesPicker(item)
        }
    }

    private fun openEpisodesPicker(show: MediaSearchResult) {
        _discoverState.update {
            it.copy(
                selectedShowForEpisodes = show,
                isLoadingEpisodes = true,
                episodes = emptyList(),
                episodesError = null,
                selectedSeason = 1,
            )
        }
        viewModelScope.launch {
            val result = mediaCatalogRepository.fetchEpisodes(show)
            result.fold(
                onSuccess = { episodes ->
                    val firstSeason = episodes.minOfOrNull { it.season } ?: 1
                    _discoverState.update {
                        it.copy(
                            isLoadingEpisodes = false,
                            episodes = episodes,
                            selectedSeason = firstSeason,
                            episodesError = if (episodes.isEmpty()) "No episode list available for this title." else null,
                        )
                    }
                },
                onFailure = { err ->
                    _discoverState.update {
                        it.copy(
                            isLoadingEpisodes = false,
                            episodesError = err.message ?: "Could not load episodes.",
                        )
                    }
                },
            )
        }
    }

    fun selectEpisodeSeason(season: Int) {
        _discoverState.update { it.copy(selectedSeason = season) }
    }

    fun dismissEpisodesPicker() {
        _discoverState.update {
            it.copy(
                selectedShowForEpisodes = null,
                episodes = emptyList(),
                episodesError = null,
                isLoadingEpisodes = false,
            )
        }
    }

    fun applyEpisodeSelection(show: MediaSearchResult, episode: MediaEpisode) {
        val runtimeMins = episode.runtimeMinutes ?: show.runtimeMinutes ?: 42
        val synopsis = episode.synopsis.ifBlank { show.synopsis }
        val config = WatchPresenceConfig(
            mediaType = show.mediaType,
            mediaTitle = show.title,
            detailsTitle = episode.title,
            stateSubtitle = synopsis,
            episodeTag = episode.episodeCode,
            posterUrl = show.posterUrl,
            isPlaying = false,
            timelineMode = TimelineDisplayMode.ELAPSED_WITH_EPISODE,
            elapsedSeconds = 0L,
            totalSeconds = runtimeMins * 60L,
            button1Label = "Watch Episode",
            button1Url = episode.episodeUrl.ifBlank { show.watchUrl },
            button2Enabled = true,
            button2Label = "View Series",
            button2Url = show.watchUrl,
        )
        rpcManager.setConfig(config, pushIfConnected = true)
        dismissEpisodesPicker()
        _currentDestination.value = MainDestination.STUDIO
        _snackbarEvents.tryEmit("Loaded ${show.title} ${episode.episodeCode}")
    }

    fun applyShowDirectly(show: MediaSearchResult) {
        val runtimeMins = show.runtimeMinutes ?: 42
        val config = WatchPresenceConfig(
            mediaType = show.mediaType,
            mediaTitle = show.title,
            detailsTitle = show.title,
            stateSubtitle = show.synopsis,
            episodeTag = "S1E1",
            posterUrl = show.posterUrl,
            isPlaying = true,
            timelineMode = TimelineDisplayMode.PROGRESS_BAR,
            elapsedSeconds = 0L,
            totalSeconds = runtimeMins * 60L,
            button1Label = "Watch Episode",
            button1Url = show.watchUrl,
            button2Enabled = true,
            button2Label = "View Series",
            button2Url = show.watchUrl,
        )
        rpcManager.setConfig(config, pushIfConnected = true)
        dismissEpisodesPicker()
        _currentDestination.value = MainDestination.STUDIO
        _snackbarEvents.tryEmit("Loaded ${show.title}")
    }

    // --- Connect & Settings Actions ---

    fun updateToken(token: String) {
        viewModelScope.launch {
            preferencesRepository.updateSettings { it.copy(token = token) }
        }
    }

    fun updateApplicationId(appId: String) {
        viewModelScope.launch {
            preferencesRepository.updateSettings { it.copy(applicationId = appId) }
        }
    }

    fun updateOnlineStatus(status: DiscordOnlineStatus) {
        viewModelScope.launch {
            preferencesRepository.updateSettings { it.copy(onlineStatus = status) }
            if (connectionState.value is ConnectionState.Connected) {
                rpcManager.pushUpdateNow()
            }
        }
    }

    fun updatePlayBadgeUrl(url: String) {
        viewModelScope.launch {
            preferencesRepository.updateSettings { it.copy(playBadgeUrl = url) }
        }
    }

    fun updatePauseBadgeUrl(url: String) {
        viewModelScope.launch {
            preferencesRepository.updateSettings { it.copy(pauseBadgeUrl = url) }
        }
    }

    fun updateKeepAliveNotification(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateSettings { it.copy(keepAliveNotification = enabled) }
        }
    }

    fun resetAdvancedDefaults() {
        viewModelScope.launch {
            preferencesRepository.updateSettings {
                it.copy(
                    applicationId = RpcSettings.DEFAULT_APPLICATION_ID,
                    playBadgeUrl = RpcSettings.DEFAULT_PLAY_BADGE_URL,
                    pauseBadgeUrl = RpcSettings.DEFAULT_PAUSE_BADGE_URL,
                )
            }
            _snackbarEvents.tryEmit("Restored default App ID and Play/Pause badges")
        }
    }
}
