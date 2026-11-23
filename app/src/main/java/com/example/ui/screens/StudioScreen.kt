package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BroadcastOnPersonal
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.MediaType
import com.example.model.TimelineDisplayMode
import com.example.model.WatchPresenceConfig
import com.example.model.formatDuration
import com.example.model.parseDurationToSeconds
import com.example.ui.components.DiscordPresencePreviewCard
import com.example.ui.theme.DiscordAmber
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.DiscordCardSurface
import com.example.ui.theme.DiscordCrimson
import com.example.ui.theme.DiscordGreen
import com.example.ui.theme.DiscordInputSurface
import com.example.ui.theme.DiscordObsidian
import com.example.ui.theme.DiscordOutline
import com.example.ui.theme.DiscordSurfaceElevated
import com.example.ui.theme.DiscordTextMuted
import com.example.ui.theme.DiscordTextPrimary
import com.example.ui.theme.DiscordTextSecondary
import com.example.ui.theme.JetBrainsMonoFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    config: WatchPresenceConfig,
    connectionState: ConnectionState,
    onSwitchMediaType: (MediaType) -> Unit,
    onUpdateField: ((WatchPresenceConfig) -> WatchPresenceConfig) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSetTimelineMode: (TimelineDisplayMode) -> Unit,
    onSeekTo: (Long) -> Unit,
    onGoLiveOrUpdate: () -> Unit,
    onDisconnect: () -> Unit,
    onSavePreset: (String) -> Unit,
    onBuildPayloadJson: () -> String,
    onNavigateToDiscover: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    var showPayloadSheet by remember { mutableStateOf(false) }
    var payloadPreviewJson by remember { mutableStateOf("") }

    var elapsedEditDialog by remember { mutableStateOf(false) }
    var totalEditDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF161822), DiscordObsidian, DiscordObsidian),
                ),
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Top Studio Header
            item(key = "studio_header") {
                StudioTopHeader(
                    connectionState = connectionState,
                    onOpenSearch = onNavigateToDiscover,
                    onOpenSavePreset = {
                        presetNameInput = buildString {
                            append(config.mediaTitle.ifBlank { "My Presence" })
                            if (config.episodeTag.isNotBlank()) {
                                append(" • ")
                                append(config.episodeTag)
                            }
                        }
                        showSaveDialog = true
                    },
                    onOpenJsonInspector = {
                        payloadPreviewJson = onBuildPayloadJson()
                        showPayloadSheet = true
                    },
                )
            }

            // Live Pixel-Perfect Discord RPC Preview Card
            item(key = "rpc_preview") {
                DiscordPresencePreviewCard(
                    config = config,
                    onTogglePlayPause = onTogglePlayPause,
                    onButtonClick = { label, url ->
                        val cleaned = url.trim()
                        if (cleaned.isNotEmpty()) {
                            val normalized = if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
                                cleaned
                            } else {
                                "https://$cleaned"
                            }
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
                            }.onFailure {
                                onShowMessage("Could not open link for \"$label\"")
                            }
                        } else {
                            onShowMessage("Add a URL for \"$label\" in Action Buttons below")
                        }
                    },
                )
            }

            // Live Broadcast & Playback Controls
            item(key = "broadcast_controls") {
                BroadcastControlDeck(
                    config = config,
                    connectionState = connectionState,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekTo = onSeekTo,
                    onGoLiveOrUpdate = onGoLiveOrUpdate,
                    onDisconnect = onDisconnect,
                    onEditElapsed = { elapsedEditDialog = true },
                    onEditTotal = { totalEditDialog = true },
                )
            }

            // Media Type Selector (Movie | TV Series | Anime)
            item(key = "media_type_selector") {
                MediaTypeSection(
                    selectedType = config.mediaType,
                    timelineMode = config.timelineMode,
                    onSelectType = onSwitchMediaType,
                    onSelectTimelineMode = onSetTimelineMode,
                )
            }

            // Media Metadata Editor Card
            item(key = "metadata_editor") {
                MediaDetailsEditorCard(
                    config = config,
                    onUpdateField = onUpdateField,
                )
            }

            // Action Buttons Editor Card
            item(key = "buttons_editor") {
                ActionButtonsEditorCard(
                    config = config,
                    onUpdateField = onUpdateField,
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Save Preset Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = DiscordSurfaceElevated,
            titleContentColor = DiscordTextPrimary,
            textContentColor = DiscordTextSecondary,
            title = { Text("Save RPC Preset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Save this presence locally for one-tap launching later.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        label = { Text("Preset name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_preset_name_input"),
                        colors = studioTextFieldColors(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSavePreset(presetNameInput)
                        showSaveDialog = false
                    },
                    modifier = Modifier.testTag("confirm_save_preset_button"),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Edit Elapsed / Total Time Dialogs
    if (elapsedEditDialog) {
        DurationInputDialog(
            title = "Set Elapsed Position",
            initialSeconds = config.elapsedSeconds,
            onDismiss = { elapsedEditDialog = false },
            onConfirm = { secs ->
                onSeekTo(secs)
                elapsedEditDialog = false
            },
        )
    }

    if (totalEditDialog) {
        DurationInputDialog(
            title = "Set Total Duration",
            initialSeconds = config.totalSeconds,
            onDismiss = { totalEditDialog = false },
            onConfirm = { secs ->
                onUpdateField { cur ->
                    cur.copy(
                        totalSeconds = secs,
                        elapsedSeconds = if (secs > 0L) cur.elapsedSeconds.coerceAtMost(secs) else cur.elapsedSeconds,
                    )
                }
                totalEditDialog = false
            },
        )
    }

    // Discord Gateway OP 3 Payload Inspector Bottom Sheet
    if (showPayloadSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPayloadSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DiscordCardSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Discord Gateway Payload",
                            style = MaterialTheme.typography.titleMedium,
                            color = DiscordTextPrimary,
                        )
                        Text(
                            text = "OP 3 • Type 3 (Watching)",
                            style = MaterialTheme.typography.labelSmall,
                            color = DiscordTextMuted,
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("WatchRPC Payload", payloadPreviewJson))
                            onShowMessage("Copied OP 3 JSON to clipboard")
                        },
                        modifier = Modifier.testTag("copy_payload_json_button"),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DiscordObsidian,
                    border = BorderStroke(1.dp, DiscordOutline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = payloadPreviewJson,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFFBDE3FF),
                        modifier = Modifier
                            .padding(14.dp)
                            .horizontalScroll(rememberScrollState()),
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun StudioTopHeader(
    connectionState: ConnectionState,
    onOpenSearch: () -> Unit,
    onOpenSavePreset: () -> Unit,
    onOpenJsonInspector: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "WatchRPC",
                style = MaterialTheme.typography.headlineMedium,
                color = DiscordTextPrimary,
            )
            ConnectionStatusPill(connectionState = connectionState)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalIconButton(
                onClick = onOpenSearch,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("studio_quick_search_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = DiscordSurfaceElevated,
                    contentColor = DiscordTextPrimary,
                ),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "Search movies, TV shows, or anime")
            }
            FilledTonalIconButton(
                onClick = onOpenSavePreset,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("studio_save_preset_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = DiscordSurfaceElevated,
                    contentColor = DiscordTextPrimary,
                ),
            ) {
                Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Save current presence as preset")
            }
            FilledTonalIconButton(
                onClick = onOpenJsonInspector,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("studio_inspect_json_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = DiscordSurfaceElevated,
                    contentColor = DiscordTextPrimary,
                ),
            ) {
                Icon(Icons.Rounded.Code, contentDescription = "Inspect Discord RPC JSON payload")
            }
        }
    }
}

@Composable
fun ConnectionStatusPill(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val (dotColor, label) = when (connectionState) {
        is ConnectionState.Connected -> DiscordGreen to "Live as @${connectionState.user.username}"
        is ConnectionState.Connecting -> DiscordAmber to connectionState.stage
        is ConnectionState.Error -> DiscordCrimson to "Error • Tap Connect"
        ConnectionState.Disconnected -> DiscordTextMuted to "Preview Only • Offline"
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(DiscordSurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = DiscordTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BroadcastControlDeck(
    config: WatchPresenceConfig,
    connectionState: ConnectionState,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onGoLiveOrUpdate: () -> Unit,
    onDisconnect: () -> Unit,
    onEditElapsed: () -> Unit,
    onEditTotal: () -> Unit,
) {
    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DiscordCardSurface,
        border = BorderStroke(1.dp, DiscordOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Primary Row: Play/Pause + Go Live / Push Update + Disconnect
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("studio_play_pause_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (config.isPlaying) Color(0xFF1D3828) else Color(0xFF3A311A),
                        contentColor = if (config.isPlaying) DiscordGreen else DiscordAmber,
                    ),
                ) {
                    Icon(
                        imageVector = if (config.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (config.isPlaying) "Pause" else "Play",
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (config.isPlaying) "Pause" else "Resume")
                }

                Button(
                    onClick = onGoLiveOrUpdate,
                    enabled = !isConnecting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("studio_broadcast_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) DiscordGreen else DiscordBlurple,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Rounded.Sync else Icons.Rounded.BroadcastOnPersonal,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isConnecting -> "Connecting..."
                            isConnected -> "Push Update"
                            else -> "Go Live"
                        },
                        maxLines = 1,
                    )
                }

                if (isConnected || isConnecting) {
                    FilledTonalIconButton(
                        onClick = onDisconnect,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("studio_disconnect_button"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFF3E191B),
                            contentColor = DiscordCrimson,
                        ),
                    ) {
                        Icon(Icons.Rounded.StopCircle, contentDescription = "Disconnect Discord RPC")
                    }
                }
            }

            // Timeline Scrubber & Quick Seek
            val sliderMax = config.totalSeconds.coerceAtLeast(60L).toFloat()
            val forceHours = config.totalSeconds >= 3600L || config.elapsedSeconds >= 3600L
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Slider(
                    value = config.elapsedSeconds.coerceIn(0L, sliderMax.toLong()).toFloat(),
                    onValueChange = { onSeekTo(it.toLong()) },
                    valueRange = 0f..sliderMax,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_timeline_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = DiscordBlurple,
                        inactiveTrackColor = DiscordInputSurface,
                    ),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onEditElapsed,
                        modifier = Modifier.testTag("edit_elapsed_time_button"),
                    ) {
                        Icon(
                            Icons.Rounded.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = DiscordTextMuted,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDuration(config.elapsedSeconds, forceHours = forceHours),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.5.sp,
                            color = DiscordTextPrimary,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { onSeekTo((config.elapsedSeconds - 30L).coerceAtLeast(0L)) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("seek_back_30_button"),
                        ) {
                            Icon(
                                Icons.Rounded.FastRewind,
                                contentDescription = "Rewind 30 seconds",
                                tint = DiscordTextSecondary,
                            )
                        }
                        IconButton(
                            onClick = { onSeekTo(config.elapsedSeconds + 30L) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("seek_forward_30_button"),
                        ) {
                            Icon(
                                Icons.Rounded.FastForward,
                                contentDescription = "Forward 30 seconds",
                                tint = DiscordTextSecondary,
                            )
                        }
                    }

                    TextButton(
                        onClick = onEditTotal,
                        modifier = Modifier.testTag("edit_total_time_button"),
                    ) {
                        Text(
                            text = formatDuration(config.totalSeconds, forceHours = forceHours),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.5.sp,
                            color = DiscordTextPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTypeSection(
    selectedType: MediaType,
    timelineMode: TimelineDisplayMode,
    onSelectType: (MediaType) -> Unit,
    onSelectTimelineMode: (TimelineDisplayMode) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DiscordCardSurface,
        border = BorderStroke(1.dp, DiscordOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Format & Card Style",
                style = MaterialTheme.typography.titleSmall,
                color = DiscordTextSecondary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MediaType.entries.forEach { type ->
                    val icon: ImageVector = when (type) {
                        MediaType.MOVIE -> Icons.Rounded.Movie
                        MediaType.SERIES -> Icons.Rounded.LiveTv
                        MediaType.ANIME -> Icons.Rounded.AutoAwesome
                    }
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { onSelectType(type) },
                        label = { Text(type.label, maxLines = 1) },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("media_type_chip_${type.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DiscordBlurple,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = DiscordInputSurface,
                            labelColor = DiscordTextSecondary,
                            iconColor = DiscordTextMuted,
                        ),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimelineDisplayMode.entries.forEach { mode ->
                    FilterChip(
                        selected = timelineMode == mode,
                        onClick = { onSelectTimelineMode(mode) },
                        label = { Text(mode.label, maxLines = 1) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("timeline_mode_chip_${mode.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2B3166),
                            selectedLabelColor = Color.White,
                            containerColor = DiscordInputSurface,
                            labelColor = DiscordTextSecondary,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaDetailsEditorCard(
    config: WatchPresenceConfig,
    onUpdateField: ((WatchPresenceConfig) -> WatchPresenceConfig) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DiscordCardSurface,
        border = BorderStroke(1.dp, DiscordOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Watching Details",
                style = MaterialTheme.typography.titleSmall,
                color = DiscordTextSecondary,
            )

            OutlinedTextField(
                value = config.mediaTitle,
                onValueChange = { value ->
                    onUpdateField { cur ->
                        val syncDetails = cur.mediaType == MediaType.MOVIE &&
                            (cur.detailsTitle.isBlank() || cur.detailsTitle == cur.mediaTitle)
                        cur.copy(
                            mediaTitle = value,
                            detailsTitle = if (syncDetails) value else cur.detailsTitle,
                        )
                    }
                },
                label = {
                    Text(
                        if (config.mediaType == MediaType.MOVIE) {
                            "Movie Name (Header: Watching ...)"
                        } else {
                            "Series / Anime Name (Header: Watching ...)"
                        },
                    )
                },
                placeholder = { Text("e.g. The Mentalist, Modha Rathri") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("media_title_input"),
                colors = studioTextFieldColors(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = config.detailsTitle,
                    onValueChange = { value -> onUpdateField { it.copy(detailsTitle = value) } },
                    label = {
                        Text(
                            if (config.mediaType == MediaType.MOVIE) "Primary Title" else "Episode Title",
                        )
                    },
                    placeholder = {
                        Text(
                            if (config.mediaType == MediaType.MOVIE) "Modha Rathri" else "Rose-Colored Glasses",
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("details_title_input"),
                    colors = studioTextFieldColors(),
                )

                if (config.mediaType != MediaType.MOVIE) {
                    OutlinedTextField(
                        value = config.episodeTag,
                        onValueChange = { value -> onUpdateField { it.copy(episodeTag = value) } },
                        label = { Text("Episode") },
                        placeholder = { Text("S2E11") },
                        singleLine = true,
                        modifier = Modifier
                            .width(108.dp)
                            .testTag("episode_tag_input"),
                        colors = studioTextFieldColors(),
                    )
                }
            }

            OutlinedTextField(
                value = config.stateSubtitle,
                onValueChange = { value -> onUpdateField { it.copy(stateSubtitle = value) } },
                label = {
                    Text(
                        if (config.mediaType == MediaType.MOVIE) {
                            "Year & Runtime"
                        } else {
                            "Episode Synopsis / State"
                        },
                    )
                },
                placeholder = {
                    Text(
                        if (config.mediaType == MediaType.MOVIE) {
                            "2026 • 141 minutes"
                        } else {
                            "Rigsby must go undercover as an alumnus to investigate..."
                        },
                    )
                },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("state_subtitle_input"),
                colors = studioTextFieldColors(),
            )

            OutlinedTextField(
                value = config.posterUrl,
                onValueChange = { value -> onUpdateField { it.copy(posterUrl = value) } },
                label = { Text("Poster Artwork URL") },
                placeholder = { Text("https://... (auto-filled via Discover)") },
                leadingIcon = {
                    Icon(Icons.Rounded.Image, contentDescription = null, tint = DiscordTextMuted)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("poster_url_input"),
                colors = studioTextFieldColors(),
            )
        }
    }
}

@Composable
private fun ActionButtonsEditorCard(
    config: WatchPresenceConfig,
    onUpdateField: ((WatchPresenceConfig) -> WatchPresenceConfig) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DiscordCardSurface,
        border = BorderStroke(1.dp, DiscordOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Action Buttons (Discord Links)",
                style = MaterialTheme.typography.titleSmall,
                color = DiscordTextSecondary,
            )

            OutlinedTextField(
                value = config.button1Label,
                onValueChange = { value -> onUpdateField { it.copy(button1Label = value) } },
                label = { Text("Button 1 Label") },
                placeholder = { Text("Watch Episode / Watch Movie") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button1_label_input"),
                colors = studioTextFieldColors(),
            )

            OutlinedTextField(
                value = config.button1Url,
                onValueChange = { value -> onUpdateField { it.copy(button1Url = value) } },
                label = { Text("Button 1 URL") },
                placeholder = { Text("https://...") },
                leadingIcon = {
                    Icon(Icons.Rounded.Link, contentDescription = null, tint = DiscordTextMuted)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button1_url_input"),
                colors = studioTextFieldColors(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Second Action Button",
                        style = MaterialTheme.typography.titleSmall,
                        color = DiscordTextPrimary,
                    )
                    Text(
                        text = "e.g. \"View Series\" for TV & Anime",
                        style = MaterialTheme.typography.bodySmall,
                        color = DiscordTextMuted,
                    )
                }
                Switch(
                    checked = config.button2Enabled,
                    onCheckedChange = { enabled -> onUpdateField { it.copy(button2Enabled = enabled) } },
                    modifier = Modifier.testTag("button2_toggle_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DiscordBlurple,
                    ),
                )
            }

            AnimatedVisibility(visible = config.button2Enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = config.button2Label,
                        onValueChange = { value -> onUpdateField { it.copy(button2Label = value) } },
                        label = { Text("Button 2 Label") },
                        placeholder = { Text("View Series") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button2_label_input"),
                        colors = studioTextFieldColors(),
                    )

                    OutlinedTextField(
                        value = config.button2Url,
                        onValueChange = { value -> onUpdateField { it.copy(button2Url = value) } },
                        label = { Text("Button 2 URL") },
                        placeholder = { Text("https://...") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Link, contentDescription = null, tint = DiscordTextMuted)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button2_url_input"),
                        colors = studioTextFieldColors(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationInputDialog(
    title: String,
    initialSeconds: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var text by remember(initialSeconds) {
        mutableStateOf(formatDuration(initialSeconds, forceHours = true))
    }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DiscordSurfaceElevated,
        titleContentColor = DiscordTextPrimary,
        textContentColor = DiscordTextSecondary,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter HH:MM:SS or MM:SS (e.g. 01:03:49 or 11:49)",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        isError = false
                    },
                    isError = isError,
                    singleLine = true,
                    label = { Text("Duration") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("duration_dialog_input"),
                    colors = studioTextFieldColors(),
                )
                if (isError) {
                    Text(
                        text = "Use HH:MM:SS or MM:SS format",
                        color = DiscordCrimson,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = parseDurationToSeconds(text)
                    if (parsed != null) {
                        onConfirm(parsed)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("duration_dialog_confirm"),
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun studioTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DiscordBlurple,
    unfocusedBorderColor = DiscordOutline,
    focusedContainerColor = DiscordInputSurface,
    unfocusedContainerColor = DiscordInputSurface,
    focusedTextColor = DiscordTextPrimary,
    unfocusedTextColor = DiscordTextPrimary,
    focusedLabelColor = Color(0xFF9AA4FF),
    unfocusedLabelColor = DiscordTextMuted,
    cursorColor = DiscordBlurple,
)
