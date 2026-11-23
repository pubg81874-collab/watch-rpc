package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.DiscordOnlineStatus
import com.example.model.MediaType
import com.example.model.RpcSettings
import com.example.model.TimelineDisplayMode
import com.example.model.WatchPresenceConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rpcDataStore: DataStore<Preferences> by preferencesDataStore(name = "watch_rpc_prefs")

class RpcPreferencesRepository(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("discord_token")
        val APP_ID = stringPreferencesKey("discord_application_id")
        val ONLINE_STATUS = stringPreferencesKey("discord_online_status")
        val PLAY_BADGE_URL = stringPreferencesKey("play_badge_url")
        val PAUSE_BADGE_URL = stringPreferencesKey("pause_badge_url")
        val KEEP_ALIVE = booleanPreferencesKey("keep_alive_notification")

        // Draft presence persistence
        val MEDIA_TYPE = stringPreferencesKey("draft_media_type")
        val MEDIA_TITLE = stringPreferencesKey("draft_media_title")
        val DETAILS_TITLE = stringPreferencesKey("draft_details_title")
        val STATE_SUBTITLE = stringPreferencesKey("draft_state_subtitle")
        val EPISODE_TAG = stringPreferencesKey("draft_episode_tag")
        val POSTER_URL = stringPreferencesKey("draft_poster_url")
        val IS_PLAYING = booleanPreferencesKey("draft_is_playing")
        val TIMELINE_MODE = stringPreferencesKey("draft_timeline_mode")
        val ELAPSED_SECONDS = longPreferencesKey("draft_elapsed_seconds")
        val TOTAL_SECONDS = longPreferencesKey("draft_total_seconds")
        val BUTTON1_LABEL = stringPreferencesKey("draft_button1_label")
        val BUTTON1_URL = stringPreferencesKey("draft_button1_url")
        val BUTTON2_ENABLED = booleanPreferencesKey("draft_button2_enabled")
        val BUTTON2_LABEL = stringPreferencesKey("draft_button2_label")
        val BUTTON2_URL = stringPreferencesKey("draft_button2_url")
    }

    val settingsFlow: Flow<RpcSettings> = context.rpcDataStore.data.map { prefs ->
        RpcSettings(
            token = prefs[Keys.TOKEN].orEmpty(),
            applicationId = prefs[Keys.APP_ID]?.takeIf { it.isNotBlank() }
                ?: RpcSettings.DEFAULT_APPLICATION_ID,
            onlineStatus = prefs[Keys.ONLINE_STATUS]
                ?.let { raw -> DiscordOnlineStatus.entries.firstOrNull { it.name == raw } }
                ?: DiscordOnlineStatus.ONLINE,
            playBadgeUrl = prefs[Keys.PLAY_BADGE_URL]?.takeIf { it.isNotBlank() }
                ?: RpcSettings.DEFAULT_PLAY_BADGE_URL,
            pauseBadgeUrl = prefs[Keys.PAUSE_BADGE_URL]?.takeIf { it.isNotBlank() }
                ?: RpcSettings.DEFAULT_PAUSE_BADGE_URL,
            keepAliveNotification = prefs[Keys.KEEP_ALIVE] ?: true,
        )
    }

    val savedDraftFlow: Flow<WatchPresenceConfig?> = context.rpcDataStore.data.map { prefs ->
        val hasTitle = prefs[Keys.MEDIA_TITLE] != null
        if (!hasTitle) return@map null
        WatchPresenceConfig(
            mediaType = prefs[Keys.MEDIA_TYPE]
                ?.let { runCatching { MediaType.valueOf(it) }.getOrNull() }
                ?: MediaType.MOVIE,
            mediaTitle = prefs[Keys.MEDIA_TITLE].orEmpty(),
            detailsTitle = prefs[Keys.DETAILS_TITLE].orEmpty(),
            stateSubtitle = prefs[Keys.STATE_SUBTITLE].orEmpty(),
            episodeTag = prefs[Keys.EPISODE_TAG].orEmpty(),
            posterUrl = prefs[Keys.POSTER_URL].orEmpty(),
            isPlaying = prefs[Keys.IS_PLAYING] ?: true,
            timelineMode = prefs[Keys.TIMELINE_MODE]
                ?.let { runCatching { TimelineDisplayMode.valueOf(it) }.getOrNull() }
                ?: TimelineDisplayMode.PROGRESS_BAR,
            elapsedSeconds = prefs[Keys.ELAPSED_SECONDS] ?: 0L,
            totalSeconds = prefs[Keys.TOTAL_SECONDS] ?: 5400L,
            button1Label = prefs[Keys.BUTTON1_LABEL] ?: "Watch Movie",
            button1Url = prefs[Keys.BUTTON1_URL].orEmpty(),
            button2Enabled = prefs[Keys.BUTTON2_ENABLED] ?: false,
            button2Label = prefs[Keys.BUTTON2_LABEL] ?: "View Series",
            button2Url = prefs[Keys.BUTTON2_URL].orEmpty(),
        )
    }

    suspend fun updateSettings(transform: (RpcSettings) -> RpcSettings) {
        context.rpcDataStore.edit { prefs ->
            val current = RpcSettings(
                token = prefs[Keys.TOKEN].orEmpty(),
                applicationId = prefs[Keys.APP_ID]?.takeIf { it.isNotBlank() }
                    ?: RpcSettings.DEFAULT_APPLICATION_ID,
                onlineStatus = prefs[Keys.ONLINE_STATUS]
                    ?.let { raw -> DiscordOnlineStatus.entries.firstOrNull { it.name == raw } }
                    ?: DiscordOnlineStatus.ONLINE,
                playBadgeUrl = prefs[Keys.PLAY_BADGE_URL]?.takeIf { it.isNotBlank() }
                    ?: RpcSettings.DEFAULT_PLAY_BADGE_URL,
                pauseBadgeUrl = prefs[Keys.PAUSE_BADGE_URL]?.takeIf { it.isNotBlank() }
                    ?: RpcSettings.DEFAULT_PAUSE_BADGE_URL,
                keepAliveNotification = prefs[Keys.KEEP_ALIVE] ?: true,
            )
            val updated = transform(current)
            prefs[Keys.TOKEN] = updated.token.trim()
            prefs[Keys.APP_ID] = updated.applicationId.trim().ifEmpty { RpcSettings.DEFAULT_APPLICATION_ID }
            prefs[Keys.ONLINE_STATUS] = updated.onlineStatus.name
            prefs[Keys.PLAY_BADGE_URL] = updated.playBadgeUrl.trim().ifEmpty { RpcSettings.DEFAULT_PLAY_BADGE_URL }
            prefs[Keys.PAUSE_BADGE_URL] = updated.pauseBadgeUrl.trim().ifEmpty { RpcSettings.DEFAULT_PAUSE_BADGE_URL }
            prefs[Keys.KEEP_ALIVE] = updated.keepAliveNotification
        }
    }

    suspend fun saveDraft(config: WatchPresenceConfig) {
        context.rpcDataStore.edit { prefs ->
            prefs[Keys.MEDIA_TYPE] = config.mediaType.name
            prefs[Keys.MEDIA_TITLE] = config.mediaTitle
            prefs[Keys.DETAILS_TITLE] = config.detailsTitle
            prefs[Keys.STATE_SUBTITLE] = config.stateSubtitle
            prefs[Keys.EPISODE_TAG] = config.episodeTag
            prefs[Keys.POSTER_URL] = config.posterUrl
            prefs[Keys.IS_PLAYING] = config.isPlaying
            prefs[Keys.TIMELINE_MODE] = config.timelineMode.name
            prefs[Keys.ELAPSED_SECONDS] = config.elapsedSeconds
            prefs[Keys.TOTAL_SECONDS] = config.totalSeconds
            prefs[Keys.BUTTON1_LABEL] = config.button1Label
            prefs[Keys.BUTTON1_URL] = config.button1Url
            prefs[Keys.BUTTON2_ENABLED] = config.button2Enabled
            prefs[Keys.BUTTON2_LABEL] = config.button2Label
            prefs[Keys.BUTTON2_URL] = config.button2Url
        }
    }
}
