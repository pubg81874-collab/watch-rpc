package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.model.MediaType
import com.example.model.WatchPresenceConfig
import com.example.model.formatDuration
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.DiscordBlurpleBright
import com.example.ui.theme.DiscordButtonGray
import com.example.ui.theme.DiscordCardSurface
import com.example.ui.theme.DiscordInputSurface
import com.example.ui.theme.DiscordObsidian
import com.example.ui.theme.DiscordOutlineSubtle
import com.example.ui.theme.DiscordTextMuted
import com.example.ui.theme.DiscordTextPrimary
import com.example.ui.theme.DiscordTextSecondary
import com.example.ui.theme.DiscordTrackGray
import com.example.ui.theme.DiscordWatchTimerGreen
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaSansFamily

/**
 * Pixel-faithful Discord Mobile "Watching" Rich Presence card preview matching:
 * 1. Playing Movie style: poster + play triangle badge + title + "Year • Runtime minutes" +
 *    blurple progress bar with HH:MM:SS timestamps + "Watch Movie" button.
 * 2. Paused Series/Anime style: poster + pause bars badge + episode title + synopsis +
 *    green monitor icon & elapsed timer + episode icon & SxEx tag + "Watch Episode" & "View Series" buttons.
 */
@Composable
fun DiscordPresencePreviewCard(
    config: WatchPresenceConfig,
    onTogglePlayPause: () -> Unit,
    onButtonClick: (label: String, url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = config.progressFraction,
        animationSpec = spring(stiffness = 300f),
        label = "rpc_progress",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("discord_rpc_preview_card"),
        shape = RoundedCornerShape(18.dp),
        color = DiscordCardSurface,
        border = BorderStroke(1.dp, DiscordOutlineSubtle),
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header: "Watching <Media Title>"
            Text(
                text = config.headerText,
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFFE3E5E8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Main artwork + details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PosterWithPlayPauseBadge(
                    posterUrl = config.posterUrl,
                    mediaType = config.mediaType,
                    isPlaying = config.isPlaying,
                    onTogglePlayPause = onTogglePlayPause,
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = config.effectiveDetails.ifBlank { "Episode or Movie Title" },
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        color = if (config.effectiveDetails.isBlank()) DiscordTextMuted else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val subtitle = config.stateSubtitle.trim()
                    Text(
                        text = subtitle.ifBlank {
                            when (config.mediaType) {
                                MediaType.MOVIE -> "2026 • 120 minutes"
                                MediaType.SERIES, MediaType.ANIME -> "Episode overview or synopsis..."
                            }
                        },
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                        color = if (subtitle.isBlank()) DiscordTextMuted else DiscordTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Paused / Elapsed + Episode metadata row (Screenshots 2 & 3)
                    if (!config.showsProgressBar) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tv,
                                    contentDescription = "Watching elapsed timer",
                                    tint = DiscordWatchTimerGreen,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = formatDuration(
                                        config.elapsedSeconds,
                                        forceHours = config.elapsedSeconds >= 3600L,
                                    ),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = DiscordWatchTimerGreen,
                                )
                            }

                            if (config.episodeTag.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                        contentDescription = "Episode number",
                                        tint = DiscordTextMuted,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        text = config.episodeTag.trim(),
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                        color = DiscordTextSecondary,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    } else if (config.episodeTag.isNotBlank()) {
                        // Subtle episode tag when playing a series/anime with progress bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = "Episode number",
                                tint = DiscordTextMuted,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = config.episodeTag.trim(),
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.5.sp,
                                color = DiscordTextSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            // Progress Bar Section (Screenshot 1)
            if (config.showsProgressBar) {
                Spacer(modifier = Modifier.height(14.dp))
                val forceHours = config.totalSeconds >= 3600L || config.elapsedSeconds >= 3600L

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(DiscordTrackGray),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.coerceAtLeast(0.02f))
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(DiscordBlurple, DiscordBlurpleBright),
                                ),
                            ),
                    )
                }

                Spacer(modifier = Modifier.height(7.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatDuration(config.elapsedSeconds, forceHours = forceHours),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        color = DiscordTextSecondary,
                    )
                    Text(
                        text = formatDuration(config.totalSeconds, forceHours = forceHours),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        color = DiscordTextSecondary,
                    )
                }
            }

            // Action Buttons (Screenshots 1, 2, 3)
            val showBtn1 = config.button1Label.isNotBlank()
            val showBtn2 = config.button2Enabled && config.button2Label.isNotBlank()
            if (showBtn1 || showBtn2) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (showBtn1) {
                        DiscordRpcActionButton(
                            label = config.button1Label.trim(),
                            testTag = "rpc_preview_button_1",
                            onClick = { onButtonClick(config.button1Label.trim(), config.button1Url) },
                        )
                    }
                    if (showBtn2) {
                        DiscordRpcActionButton(
                            label = config.button2Label.trim(),
                            testTag = "rpc_preview_button_2",
                            onClick = { onButtonClick(config.button2Label.trim(), config.button2Url) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterWithPlayPauseBadge(
    posterUrl: String,
    mediaType: MediaType,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
) {
    val badgeBg by animateColorAsState(
        targetValue = if (isPlaying) Color(0xFF16171A) else Color(0xFF111214),
        label = "badge_bg",
    )

    Box(
        modifier = Modifier
            .size(78.dp)
            .testTag("rpc_preview_poster_box"),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF2B2F48), DiscordInputSurface),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (posterUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = posterUrl,
                    contentDescription = "Media poster artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = { PosterFallbackGlyph(mediaType) },
                    loading = { PosterFallbackGlyph(mediaType) },
                )
            } else {
                PosterFallbackGlyph(mediaType)
            }
        }

        // Overlapping circular Play / Pause badge (bottom-right, identical to screenshots)
        Box(
            modifier = Modifier
                .size(30.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(badgeBg)
                .border(2.5.dp, DiscordCardSurface, CircleShape)
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (isPlaying) "Pause playback" else "Resume playback",
                    onClick = onTogglePlayPause,
                )
                .testTag("rpc_preview_play_pause_badge"),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "play_pause_icon",
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = if (playing) "Playing" else "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun PosterFallbackGlyph(mediaType: MediaType) {
    val icon = when (mediaType) {
        MediaType.MOVIE -> Icons.Rounded.Movie
        MediaType.SERIES -> Icons.Rounded.LiveTv
        MediaType.ANIME -> Icons.Rounded.AutoAwesome
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2B2F52), DiscordObsidian),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF8E99F3),
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun DiscordRpcActionButton(
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = DiscordButtonGray,
        contentColor = Color.White,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = DiscordTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
