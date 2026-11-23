package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.model.MediaEpisode
import com.example.model.MediaSearchResult
import com.example.model.MediaType
import com.example.ui.DiscoverUiState
import com.example.ui.theme.DiscordAmber
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    onQueryChange: (String) -> Unit,
    onSelectType: (MediaType) -> Unit,
    onSearch: () -> Unit,
    onSelectResult: (MediaSearchResult) -> Unit,
    onSelectSeason: (Int) -> Unit,
    onSelectEpisode: (MediaSearchResult, MediaEpisode) -> Unit,
    onUseShowWithoutEpisode: (MediaSearchResult) -> Unit,
    onDismissEpisodes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current

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
                    text = "Discover Media",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DiscordTextPrimary,
                )
                Text(
                    text = "Auto-fill posters, episode synopses, runtimes & watch links",
                    style = MaterialTheme.typography.bodySmall,
                    color = DiscordTextMuted,
                )
            }

            // Category Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(MediaType.SERIES, MediaType.ANIME, MediaType.MOVIE).forEach { type ->
                    val icon = when (type) {
                        MediaType.SERIES -> Icons.Rounded.LiveTv
                        MediaType.ANIME -> Icons.Rounded.AutoAwesome
                        MediaType.MOVIE -> Icons.Rounded.Movie
                    }
                    FilterChip(
                        selected = state.selectedType == type,
                        onClick = { onSelectType(type) },
                        label = { Text(type.label) },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("discover_filter_${type.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DiscordBlurple,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = DiscordCardSurface,
                            labelColor = DiscordTextSecondary,
                            iconColor = DiscordTextMuted,
                        ),
                    )
                }
            }

            // Search Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            when (state.selectedType) {
                                MediaType.SERIES -> "Search TV shows (e.g. The Mentalist)"
                                MediaType.ANIME -> "Search anime (e.g. Apothecary Diaries)"
                                MediaType.MOVIE -> "Search movies (e.g. Inception)"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = DiscordTextMuted)
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.testTag("discover_clear_search_button"),
                            ) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search", tint = DiscordTextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboard?.hide()
                            onSearch()
                        },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("discover_search_input"),
                    colors = studioTextFieldColors(),
                )

                Button(
                    onClick = {
                        keyboard?.hide()
                        onSearch()
                    },
                    enabled = state.query.isNotBlank() && !state.isLoading,
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("discover_search_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiscordBlurple,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Find")
                }
            }

            // Results Area
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(color = DiscordBlurple)
                            Text(
                                text = "Searching ${state.selectedType.label} catalog...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DiscordTextSecondary,
                            )
                        }
                    }
                }

                state.errorMessage != null -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF31191C),
                        border = BorderStroke(1.dp, DiscordCrimson.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = state.errorMessage,
                                color = Color(0xFFFFDADA),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedButton(
                                onClick = onSearch,
                                modifier = Modifier.testTag("discover_retry_button"),
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }

                state.results.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 24.dp),
                        ) {
                            val icon = when (state.selectedType) {
                                MediaType.SERIES -> Icons.Rounded.LiveTv
                                MediaType.ANIME -> Icons.Rounded.AutoAwesome
                                MediaType.MOVIE -> Icons.Rounded.Movie
                            }
                            Surface(
                                shape = CircleShape,
                                color = DiscordSurfaceElevated,
                                modifier = Modifier.size(64.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = DiscordBlurple,
                                        modifier = Modifier.size(30.dp),
                                    )
                                }
                            }
                            Text(
                                text = if (state.hasSearched) {
                                    "No ${state.selectedType.label.lowercase()} matches found"
                                } else {
                                    "Find any ${state.selectedType.label}"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = DiscordTextPrimary,
                            )
                            Text(
                                text = when (state.selectedType) {
                                    MediaType.SERIES -> "Powered by TVMaze. Pick a series to browse every season & episode (S#E#)."
                                    MediaType.ANIME -> "Powered by Jikan / MyAnimeList. Pick an anime to choose an exact episode."
                                    MediaType.MOVIE -> "Powered by iTunes Movie Catalog. Auto-fills poster, release year & exact runtime."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = DiscordTextMuted,
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items(state.results, key = { "${it.mediaType}_${it.externalId}" }) { item ->
                            MediaResultCard(
                                item = item,
                                onClick = { onSelectResult(item) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Episode Picker Bottom Sheet (for TV Series & Anime)
    val activeShow = state.selectedShowForEpisodes
    if (activeShow != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissEpisodes,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DiscordCardSurface,
        ) {
            EpisodePickerSheetContent(
                show = activeShow,
                isLoading = state.isLoadingEpisodes,
                error = state.episodesError,
                episodes = state.episodes,
                selectedSeason = state.selectedSeason,
                onSelectSeason = onSelectSeason,
                onSelectEpisode = { ep -> onSelectEpisode(activeShow, ep) },
                onUseShowDirectly = { onUseShowWithoutEpisode(activeShow) },
            )
        }
    }
}

@Composable
private fun MediaResultCard(
    item: MediaSearchResult,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = DiscordCardSurface,
        border = BorderStroke(1.dp, DiscordOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discover_result_${item.externalId}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 62.dp, height = 84.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DiscordInputSurface),
                contentAlignment = Alignment.Center,
            ) {
                if (item.posterUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = item.posterUrl,
                        contentDescription = "${item.title} poster",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Rounded.Movie, contentDescription = null, tint = DiscordTextMuted)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = DiscordTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (item.year.isNotBlank()) {
                        Text(
                            text = item.year,
                            style = MaterialTheme.typography.labelSmall,
                            color = DiscordTextSecondary,
                        )
                    }
                    if (item.runtimeMinutes != null) {
                        Text(
                            text = "${item.runtimeMinutes} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = DiscordTextSecondary,
                        )
                    }
                    if (!item.rating.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Star,
                                contentDescription = null,
                                tint = DiscordAmber,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = item.rating,
                                style = MaterialTheme.typography.labelSmall,
                                color = DiscordAmber,
                            )
                        }
                    }
                }

                if (item.synopsis.isNotBlank()) {
                    Text(
                        text = item.synopsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = DiscordTextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Select",
                tint = DiscordTextMuted,
            )
        }
    }
}

@Composable
private fun EpisodePickerSheetContent(
    show: MediaSearchResult,
    isLoading: Boolean,
    error: String?,
    episodes: List<MediaEpisode>,
    selectedSeason: Int,
    onSelectSeason: (Int) -> Unit,
    onSelectEpisode: (MediaEpisode) -> Unit,
    onUseShowDirectly: () -> Unit,
) {
    val seasons = remember(episodes) {
        episodes.map { it.season }.distinct().sorted()
    }
    val seasonEpisodes = remember(episodes, selectedSeason) {
        episodes.filter { it.season == selectedSeason }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = show.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = DiscordTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Pick an episode to populate WatchRPC",
                    style = MaterialTheme.typography.bodySmall,
                    color = DiscordTextMuted,
                )
            }
            OutlinedButton(
                onClick = onUseShowDirectly,
                modifier = Modifier.testTag("use_show_directly_button"),
            ) {
                Text("Use Series")
            }
        }

        if (seasons.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(seasons) { season ->
                    FilterChip(
                        selected = season == selectedSeason,
                        onClick = { onSelectSeason(season) },
                        label = { Text("Season $season") },
                        modifier = Modifier.testTag("season_chip_$season"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DiscordBlurple,
                            selectedLabelColor = Color.White,
                            containerColor = DiscordInputSurface,
                            labelColor = DiscordTextSecondary,
                        ),
                    )
                }
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = DiscordBlurple)
                }
            }

            error != null -> {
                Text(
                    text = error,
                    color = DiscordTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(seasonEpisodes, key = { it.id }) { ep ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DiscordInputSurface,
                            border = BorderStroke(1.dp, DiscordOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectEpisode(ep) }
                                .testTag("episode_item_${ep.episodeCode}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF252A54),
                                ) {
                                    Text(
                                        text = ep.episodeCode,
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFDDE1FF),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = ep.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DiscordTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (ep.synopsis.isNotBlank()) {
                                        Text(
                                            text = ep.synopsis,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DiscordTextMuted,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = "Load episode",
                                    tint = DiscordTextMuted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
