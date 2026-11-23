package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.MediaType
import com.example.model.TimelineDisplayMode
import com.example.model.WatchPresenceConfig

@Entity(tableName = "rpc_presets")
data class RpcPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val mediaType: String,
    val mediaTitle: String,
    val detailsTitle: String,
    val stateSubtitle: String,
    val episodeTag: String,
    val posterUrl: String,
    val isPlaying: Boolean,
    val timelineMode: String,
    val elapsedSeconds: Long,
    val totalSeconds: Long,
    val button1Label: String,
    val button1Url: String,
    val button2Enabled: Boolean,
    val button2Label: String,
    val button2Url: String,
    val updatedAtMs: Long = System.currentTimeMillis(),
) {
    fun toConfig(): WatchPresenceConfig = WatchPresenceConfig(
        mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.MOVIE),
        mediaTitle = mediaTitle,
        detailsTitle = detailsTitle,
        stateSubtitle = stateSubtitle,
        episodeTag = episodeTag,
        posterUrl = posterUrl,
        isPlaying = isPlaying,
        timelineMode = runCatching { TimelineDisplayMode.valueOf(timelineMode) }
            .getOrDefault(TimelineDisplayMode.PROGRESS_BAR),
        elapsedSeconds = elapsedSeconds,
        totalSeconds = totalSeconds,
        button1Label = button1Label,
        button1Url = button1Url,
        button2Enabled = button2Enabled,
        button2Label = button2Label,
        button2Url = button2Url,
    )

    companion object {
        fun fromConfig(label: String, config: WatchPresenceConfig): RpcPresetEntity = RpcPresetEntity(
            label = label.trim().ifEmpty { config.mediaTitle.ifBlank { "Custom Preset" } },
            mediaType = config.mediaType.name,
            mediaTitle = config.mediaTitle,
            detailsTitle = config.detailsTitle,
            stateSubtitle = config.stateSubtitle,
            episodeTag = config.episodeTag,
            posterUrl = config.posterUrl,
            isPlaying = config.isPlaying,
            timelineMode = config.timelineMode.name,
            elapsedSeconds = config.elapsedSeconds,
            totalSeconds = config.totalSeconds,
            button1Label = config.button1Label,
            button1Url = config.button1Url,
            button2Enabled = config.button2Enabled,
            button2Label = config.button2Label,
            button2Url = config.button2Url,
            updatedAtMs = System.currentTimeMillis(),
        )
    }
}
