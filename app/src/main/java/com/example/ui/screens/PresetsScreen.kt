package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.BroadcastOnPersonal
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.local.RpcPresetEntity
import com.example.model.MediaType
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.DiscordCardSurface
import com.example.ui.theme.DiscordCrimson
import com.example.ui.theme.DiscordInputSurface
import com.example.ui.theme.DiscordObsidian
import com.example.ui.theme.DiscordOutline
import com.example.ui.theme.DiscordSurfaceElevated
import com.example.ui.theme.DiscordTextMuted
import com.example.ui.theme.DiscordTextPrimary
import com.example.ui.theme.DiscordTextSecondary
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun PresetsScreen(
    presets: List<RpcPresetEntity>,
    onLoadPreset: (RpcPresetEntity, Boolean) -> Unit,
    onDeletePreset: (RpcPresetEntity) -> Unit,
    onGoToDiscover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF161822), DiscordObsidian, DiscordObsidian),
                ),
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Saved Presets",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DiscordTextPrimary,
                )
                Text(
                    text = "Stored locally on this device for instant Discord broadcasting",
                    style = MaterialTheme.typography.bodySmall,
                    color = DiscordTextMuted,
                )
            }

            if (presets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 24.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DiscordSurfaceElevated,
                            modifier = Modifier.size(68.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Bookmarks,
                                    contentDescription = null,
                                    tint = DiscordBlurple,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Text(
                            text = "No saved presets yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = DiscordTextPrimary,
                        )
                        Text(
                            text = "Configure a movie, TV series, or anime in Studio and tap the bookmark icon to save it here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DiscordTextMuted,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onGoToDiscover,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("presets_empty_discover_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Discover Media")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(presets, key = { it.id }) { preset ->
                        PresetCard(
                            preset = preset,
                            onEditInStudio = { onLoadPreset(preset, false) },
                            onBroadcastNow = { onLoadPreset(preset, true) },
                            onDelete = { onDeletePreset(preset) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: RpcPresetEntity,
    onEditInStudio: () -> Unit,
    onBroadcastNow: () -> Unit,
    onDelete: () -> Unit,
) {
    val type = runCatching { MediaType.valueOf(preset.mediaType) }.getOrDefault(MediaType.MOVIE)
    val icon = when (type) {
        MediaType.MOVIE -> Icons.Rounded.Movie
        MediaType.SERIES -> Icons.Rounded.LiveTv
        MediaType.ANIME -> Icons.Rounded.AutoAwesome
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DiscordCardSurface,
        border = BorderStroke(1.dp, DiscordOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("preset_card_${preset.id}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DiscordInputSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (preset.posterUrl.isNotBlank()) {
                        SubcomposeAsyncImage(
                            model = preset.posterUrl,
                            contentDescription = "${preset.mediaTitle} poster",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(icon, contentDescription = null, tint = DiscordBlurple)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF252A54),
                        ) {
                            Text(
                                text = type.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDDE1FF),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        if (preset.episodeTag.isNotBlank()) {
                            Text(
                                text = preset.episodeTag,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                color = DiscordTextSecondary,
                            )
                        }
                    }

                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = DiscordTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = preset.stateSubtitle.ifBlank { preset.detailsTitle },
                        style = MaterialTheme.typography.bodySmall,
                        color = DiscordTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_preset_${preset.id}"),
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete preset",
                        tint = DiscordCrimson,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onEditInStudio,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("load_preset_${preset.id}"),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }

                FilledTonalButton(
                    onClick = onBroadcastNow,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("broadcast_preset_${preset.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DiscordBlurple,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        Icons.Rounded.BroadcastOnPersonal,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Go Live")
                }
            }
        }
    }
}
