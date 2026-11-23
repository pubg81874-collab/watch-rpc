package com.example.model

import java.util.Locale

enum class MediaType(
    val label: String,
    val defaultButton1: String,
    val defaultButton2: String,
    val defaultButton2Enabled: Boolean,
) {
    MOVIE(
        label = "Movie",
        defaultButton1 = "Watch Movie",
        defaultButton2 = "View Info",
        defaultButton2Enabled = false,
    ),
    SERIES(
        label = "TV Series",
        defaultButton1 = "Watch Episode",
        defaultButton2 = "View Series",
        defaultButton2Enabled = true,
    ),
    ANIME(
        label = "Anime",
        defaultButton1 = "Watch Episode",
        defaultButton2 = "View Series",
        defaultButton2Enabled = true,
    ),
}

enum class TimelineDisplayMode(val label: String) {
    PROGRESS_BAR("Progress Bar"),
    ELAPSED_WITH_EPISODE("Elapsed & Episode"),
}

enum class DiscordOnlineStatus(val apiValue: String, val label: String) {
    ONLINE("online", "Online"),
    IDLE("idle", "Idle"),
    DND("dnd", "Do Not Disturb"),
}

data class WatchPresenceConfig(
    val mediaType: MediaType = MediaType.MOVIE,
    val mediaTitle: String = "",
    val detailsTitle: String = "",
    val stateSubtitle: String = "",
    val episodeTag: String = "",
    val posterUrl: String = "",
    val isPlaying: Boolean = true,
    val timelineMode: TimelineDisplayMode = TimelineDisplayMode.PROGRESS_BAR,
    val elapsedSeconds: Long = 0L,
    val totalSeconds: Long = 5400L,
    val button1Label: String = "Watch Movie",
    val button1Url: String = "",
    val button2Enabled: Boolean = false,
    val button2Label: String = "View Series",
    val button2Url: String = "",
) {
    val headerText: String
        get() {
            val clean = mediaTitle.trim()
            return if (clean.isEmpty()) "Watching..." else "Watching $clean"
        }

    val effectiveDetails: String
        get() = detailsTitle.trim().ifEmpty { mediaTitle.trim() }

    val progressFraction: Float
        get() = if (totalSeconds > 0L) {
            (elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    /**
     * Progress bar mode (Screenshot 1): shown when timelineMode is PROGRESS_BAR and playing with total > 0.
     * Elapsed + Episode mode (Screenshots 2 & 3): shown when paused OR when user explicitly picks ELAPSED_WITH_EPISODE.
     */
    val showsProgressBar: Boolean
        get() = timelineMode == TimelineDisplayMode.PROGRESS_BAR && isPlaying && totalSeconds > 0L
}

data class DiscordUserProfile(
    val id: String,
    val username: String,
    val globalName: String?,
    val avatarHash: String?,
) {
    val displayName: String
        get() = globalName?.takeIf { it.isNotBlank() } ?: username

    val avatarUrl: String?
        get() = avatarHash?.takeIf { it.isNotBlank() }?.let {
            "https://cdn.discordapp.com/avatars/$id/$it.png?size=128"
        }
}

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val stage: String = "Connecting to Discord Gateway...") : ConnectionState
    data class Connected(
        val user: DiscordUserProfile,
        val connectedAtMs: Long,
        val lastHeartbeatAckMs: Long? = null,
    ) : ConnectionState

    data class Error(val message: String) : ConnectionState
}

data class RpcSettings(
    val token: String = "",
    val applicationId: String = DEFAULT_APPLICATION_ID,
    val onlineStatus: DiscordOnlineStatus = DiscordOnlineStatus.ONLINE,
    val playBadgeUrl: String = DEFAULT_PLAY_BADGE_URL,
    val pauseBadgeUrl: String = DEFAULT_PAUSE_BADGE_URL,
    val keepAliveNotification: Boolean = true,
) {
    companion object {
        const val DEFAULT_APPLICATION_ID = "1162753906950791290"
        const val DEFAULT_PLAY_BADGE_URL =
            "https://raw.githubusercontent.com/google/material-design-icons/master/png/av/play_circle_filled/materialicons/48dp/2x/baseline_play_circle_filled_white_48dp.png"
        const val DEFAULT_PAUSE_BADGE_URL =
            "https://raw.githubusercontent.com/google/material-design-icons/master/png/av/pause_circle_filled/materialicons/48dp/2x/baseline_pause_circle_filled_white_48dp.png"
    }
}

data class MediaSearchResult(
    val externalId: String,
    val mediaType: MediaType,
    val title: String,
    val year: String,
    val runtimeMinutes: Int?,
    val synopsis: String,
    val posterUrl: String,
    val watchUrl: String,
    val rating: String? = null,
)

data class MediaEpisode(
    val id: String,
    val season: Int,
    val episodeNumber: Int,
    val title: String,
    val synopsis: String,
    val runtimeMinutes: Int?,
    val episodeUrl: String,
    val stillUrl: String? = null,
) {
    val episodeCode: String
        get() = "S${season}E${episodeNumber}"
}

fun formatDuration(totalSeconds: Long, forceHours: Boolean = false): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val minutes = (safe % 3600L) / 60L
    val seconds = safe % 60L
    return if (forceHours || hours > 0L) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

fun parseDurationToSeconds(input: String): Long? {
    val parts = input.trim().split(":").map { it.trim() }
    return when (parts.size) {
        1 -> parts[0].toLongOrNull()?.coerceAtLeast(0L)
        2 -> {
            val m = parts[0].toLongOrNull() ?: return null
            val s = parts[1].toLongOrNull() ?: return null
            (m * 60L + s).coerceAtLeast(0L)
        }
        3 -> {
            val h = parts[0].toLongOrNull() ?: return null
            val m = parts[1].toLongOrNull() ?: return null
            val s = parts[2].toLongOrNull() ?: return null
            (h * 3600L + m * 60L + s).coerceAtLeast(0L)
        }
        else -> null
    }
}
