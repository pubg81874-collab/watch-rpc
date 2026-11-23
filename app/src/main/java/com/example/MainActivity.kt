package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ConnectionState
import com.example.ui.MainDestination
import com.example.ui.WatchRpcViewModel
import com.example.ui.screens.ConnectScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.DiscordBlurpleContainer
import com.example.ui.theme.DiscordCharcoal
import com.example.ui.theme.DiscordGreen
import com.example.ui.theme.DiscordObsidian
import com.example.ui.theme.DiscordTextMuted
import com.example.ui.theme.DiscordTextPrimary
import com.example.ui.theme.WatchRpcTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WatchRpcTheme(darkTheme = true) {
                WatchRpcRootApp()
            }
        }
    }
}

private data class NavTabSpec(
    val destination: MainDestination,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val navTabs = listOf(
    NavTabSpec(MainDestination.STUDIO, R.string.nav_studio, Icons.Rounded.Tv, Icons.Outlined.Tv),
    NavTabSpec(MainDestination.DISCOVER, R.string.nav_search, Icons.Rounded.Explore, Icons.Outlined.Explore),
    NavTabSpec(MainDestination.PRESETS, R.string.nav_presets, Icons.Rounded.Bookmarks, Icons.Outlined.Bookmarks),
    NavTabSpec(MainDestination.CONNECT, R.string.nav_connect, Icons.Rounded.Link, Icons.Outlined.Link),
)

@Composable
fun WatchRpcRootApp(
    viewModel: WatchRpcViewModel = viewModel(),
) {
    val destination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val presenceConfig by viewModel.presenceConfig.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val discoverState by viewModel.discoverState.collectAsStateWithLifecycle()
    val gatewayLogs by viewModel.gatewayLogs.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Return to Studio tab when pressing system back on secondary tabs
    BackHandler(enabled = destination != MainDestination.STUDIO) {
        viewModel.selectDestination(MainDestination.STUDIO)
    }

    val isLive = connectionState is ConnectionState.Connected

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        if (useNavigationRail) {
            Scaffold(
                containerColor = DiscordObsidian,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    NavigationRail(
                        containerColor = DiscordCharcoal,
                        contentColor = DiscordTextPrimary,
                        modifier = Modifier.statusBarsPadding(),
                    ) {
                        navTabs.forEach { tab ->
                            val selected = destination == tab.destination
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectDestination(tab.destination) },
                                modifier = Modifier.testTag("nav_tab_${tab.destination.route}"),
                                icon = {
                                    NavTabIcon(
                                        tab = tab,
                                        selected = selected,
                                        showLiveDot = tab.destination == MainDestination.CONNECT && isLive,
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = DiscordTextPrimary,
                                    indicatorColor = DiscordBlurpleContainer,
                                    unselectedIconColor = DiscordTextMuted,
                                    unselectedTextColor = DiscordTextMuted,
                                ),
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        ActiveTabContent(
                            destination = destination,
                            viewModel = viewModel,
                            presenceConfig = presenceConfig,
                            connectionState = connectionState,
                            settings = settings,
                            presets = presets,
                            discoverState = discoverState,
                            gatewayLogs = gatewayLogs,
                            onShowMessage = { msg ->
                                coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                        )
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = DiscordObsidian,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(
                        containerColor = DiscordCharcoal,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets.navigationBars,
                    ) {
                        navTabs.forEach { tab ->
                            val selected = destination == tab.destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectDestination(tab.destination) },
                                modifier = Modifier.testTag("nav_tab_${tab.destination.route}"),
                                icon = {
                                    NavTabIcon(
                                        tab = tab,
                                        selected = selected,
                                        showLiveDot = tab.destination == MainDestination.CONNECT && isLive,
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = DiscordTextPrimary,
                                    indicatorColor = DiscordBlurple,
                                    unselectedIconColor = DiscordTextMuted,
                                    unselectedTextColor = DiscordTextMuted,
                                ),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    ActiveTabContent(
                        destination = destination,
                        viewModel = viewModel,
                        presenceConfig = presenceConfig,
                        connectionState = connectionState,
                        settings = settings,
                        presets = presets,
                        discoverState = discoverState,
                        gatewayLogs = gatewayLogs,
                        onShowMessage = { msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTabIcon(
    tab: NavTabSpec,
    selected: Boolean,
    showLiveDot: Boolean,
) {
    val label = stringResource(tab.labelRes)
    if (showLiveDot) {
        BadgedBox(
            badge = {
                Badge(containerColor = DiscordGreen)
            },
        ) {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = label,
            )
        }
    } else {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = label,
        )
    }
}

@Composable
private fun ActiveTabContent(
    destination: MainDestination,
    viewModel: WatchRpcViewModel,
    presenceConfig: com.example.model.WatchPresenceConfig,
    connectionState: ConnectionState,
    settings: com.example.model.RpcSettings,
    presets: List<com.example.data.local.RpcPresetEntity>,
    discoverState: com.example.ui.DiscoverUiState,
    gatewayLogs: List<String>,
    onShowMessage: (String) -> Unit,
) {
    when (destination) {
        MainDestination.STUDIO -> StudioScreen(
            config = presenceConfig,
            connectionState = connectionState,
            onSwitchMediaType = viewModel::switchMediaType,
            onUpdateField = viewModel::updateField,
            onTogglePlayPause = viewModel::togglePlayPause,
            onSetTimelineMode = viewModel::setTimelineMode,
            onSeekTo = viewModel::seekTo,
            onGoLiveOrUpdate = viewModel::connectOrPushPresence,
            onDisconnect = viewModel::disconnectPresence,
            onSavePreset = viewModel::saveCurrentAsPreset,
            onBuildPayloadJson = viewModel::buildPreviewJson,
            onNavigateToDiscover = { viewModel.selectDestination(MainDestination.DISCOVER) },
            onShowMessage = onShowMessage,
        )

        MainDestination.DISCOVER -> DiscoverScreen(
            state = discoverState,
            onQueryChange = viewModel::updateSearchQuery,
            onSelectType = viewModel::selectSearchMediaType,
            onSearch = viewModel::performSearch,
            onSelectResult = viewModel::onMediaResultSelected,
            onSelectSeason = viewModel::selectEpisodeSeason,
            onSelectEpisode = viewModel::applyEpisodeSelection,
            onUseShowWithoutEpisode = viewModel::applyShowDirectly,
            onDismissEpisodes = viewModel::dismissEpisodesPicker,
        )

        MainDestination.PRESETS -> PresetsScreen(
            presets = presets,
            onLoadPreset = viewModel::loadPreset,
            onDeletePreset = viewModel::deletePreset,
            onGoToDiscover = { viewModel.selectDestination(MainDestination.DISCOVER) },
        )

        MainDestination.CONNECT -> ConnectScreen(
            settings = settings,
            connectionState = connectionState,
            gatewayLogs = gatewayLogs,
            onTokenChange = viewModel::updateToken,
            onOnlineStatusChange = viewModel::updateOnlineStatus,
            onAppIdChange = viewModel::updateApplicationId,
            onPlayBadgeUrlChange = viewModel::updatePlayBadgeUrl,
            onPauseBadgeUrlChange = viewModel::updatePauseBadgeUrl,
            onKeepAliveChange = viewModel::updateKeepAliveNotification,
            onResetAdvanced = viewModel::resetAdvancedDefaults,
            onConnectNow = viewModel::connectOrPushPresence,
            onDisconnect = viewModel::disconnectPresence,
            onShowMessage = onShowMessage,
        )
    }
}
