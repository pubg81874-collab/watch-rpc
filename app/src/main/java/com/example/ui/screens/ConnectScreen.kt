package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BroadcastOnPersonal
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.SubcomposeAsyncImage
import com.example.model.ConnectionState
import com.example.model.DiscordOnlineStatus
import com.example.model.RpcSettings
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

@Composable
fun ConnectScreen(
    settings: RpcSettings,
    connectionState: ConnectionState,
    gatewayLogs: List<String>,
    onTokenChange: (String) -> Unit,
    onOnlineStatusChange: (DiscordOnlineStatus) -> Unit,
    onAppIdChange: (String) -> Unit,
    onPlayBadgeUrlChange: (String) -> Unit,
    onPauseBadgeUrlChange: (String) -> Unit,
    onKeepAliveChange: (Boolean) -> Unit,
    onResetAdvanced: () -> Unit,
    onConnectNow: () -> Unit,
    onDisconnect: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showToken by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            },
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasNotifPermission = granted
            onShowMessage(
                if (granted) "Background RPC notifications enabled"
                else "Notification permission denied — RPC still works while app is open",
            )
        },
    )

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "connect_header") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Discord Gateway",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DiscordTextPrimary,
                    )
                    Text(
                        text = "Real-time Gateway v10 WebSocket Rich Presence controller",
                        style = MaterialTheme.typography.bodySmall,
                        color = DiscordTextMuted,
                    )
                }
            }

            // Live Account Connection Card
            item(key = "connection_banner") {
                AccountConnectionCard(
                    connectionState = connectionState,
                    hasToken = settings.token.isNotBlank(),
                    onConnect = onConnectNow,
                    onDisconnect = onDisconnect,
                )
            }

            // Token & Local Privacy Card
            item(key = "token_card") {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Rounded.Key, contentDescription = null, tint = DiscordBlurple)
                            Text(
                                text = "Discord Account Token",
                                style = MaterialTheme.typography.titleMedium,
                                color = DiscordTextPrimary,
                            )
                        }

                        OutlinedTextField(
                            value = settings.token,
                            onValueChange = onTokenChange,
                            label = { Text("User Token") },
                            placeholder = { Text("Paste your Discord authorization token") },
                            singleLine = true,
                            visualTransformation = if (showToken) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showToken = !showToken },
                                    modifier = Modifier.testTag("toggle_token_visibility_button"),
                                ) {
                                    Icon(
                                        imageVector = if (showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (showToken) "Hide token" else "Show token",
                                        tint = DiscordTextMuted,
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("discord_token_input"),
                            colors = studioTextFieldColors(),
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DiscordInputSurface,
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = DiscordGreen,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Stored strictly on-device in local DataStore and sent only directly to wss://gateway.discord.gg and Discord's external-assets proxy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DiscordTextSecondary,
                                )
                            }
                        }

                        // Expandable How-To Guide
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showGuide = !showGuide }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                                .testTag("toggle_gateway_guide_row"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "How WatchRPC broadcasts to Discord",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF9AA4FF),
                            )
                            Icon(
                                imageVector = if (showGuide) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF9AA4FF),
                            )
                        }

                        AnimatedVisibility(visible = showGuide) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DiscordInputSurface)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "1. Opens a real Discord Gateway v10 WebSocket session with automatic heartbeats & resume.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DiscordTextSecondary,
                                )
                                Text(
                                    text = "2. Proxies your movie/show poster & Play/Pause badge URLs through Discord's /external-assets endpoint into mp:external/... assets.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DiscordTextSecondary,
                                )
                                Text(
                                    text = "3. Sends OP 3 Watching (type=3) with start+end timestamps for the blurple progress bar when Playing, or start-only + S#E# metadata when Paused.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DiscordTextSecondary,
                                )
                            }
                        }
                    }
                }
            }

            // Presence Status & Background Service
            item(key = "status_service_card") {
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
                            text = "Discord Presence Status",
                            style = MaterialTheme.typography.titleSmall,
                            color = DiscordTextSecondary,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DiscordOnlineStatus.entries.forEach { status ->
                                val dotColor = when (status) {
                                    DiscordOnlineStatus.ONLINE -> DiscordGreen
                                    DiscordOnlineStatus.IDLE -> DiscordAmber
                                    DiscordOnlineStatus.DND -> DiscordCrimson
                                }
                                FilterChip(
                                    selected = settings.onlineStatus == status,
                                    onClick = { onOnlineStatusChange(status) },
                                    label = { Text(status.label, maxLines = 1) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(dotColor),
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("online_status_chip_${status.apiValue}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DiscordSurfaceElevated,
                                        selectedLabelColor = DiscordTextPrimary,
                                        containerColor = DiscordInputSurface,
                                        labelColor = DiscordTextMuted,
                                    ),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Keep Alive in Background",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DiscordTextPrimary,
                                )
                                Text(
                                    text = "Shows a persistent notification with Play/Pause & Disconnect actions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DiscordTextMuted,
                                )
                            }
                            Switch(
                                checked = settings.keepAliveNotification,
                                onCheckedChange = { enabled ->
                                    onKeepAliveChange(enabled)
                                    if (enabled && !hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                modifier = Modifier.testTag("keep_alive_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DiscordBlurple,
                                ),
                            )
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                            OutlinedButton(
                                onClick = {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("request_notification_permission_button"),
                            ) {
                                Icon(
                                    Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Allow Notification Controls")
                            }
                        }
                    }
                }
            }

            // Advanced App ID & Badge Icons
            item(key = "advanced_card") {
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showAdvanced = !showAdvanced }
                                .padding(vertical = 4.dp)
                                .testTag("toggle_advanced_settings_row"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Advanced Asset & App ID",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DiscordTextPrimary,
                                )
                                Text(
                                    text = "App ID ${settings.applicationId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DiscordTextMuted,
                                )
                            }
                            Icon(
                                imageVector = if (showAdvanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = DiscordTextSecondary,
                            )
                        }

                        AnimatedVisibility(visible = showAdvanced) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = settings.applicationId,
                                    onValueChange = onAppIdChange,
                                    label = { Text("Discord Application ID") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("discord_app_id_input"),
                                    colors = studioTextFieldColors(),
                                )
                                OutlinedTextField(
                                    value = settings.playBadgeUrl,
                                    onValueChange = onPlayBadgeUrlChange,
                                    label = { Text("Playing Small Badge URL") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("play_badge_url_input"),
                                    colors = studioTextFieldColors(),
                                )
                                OutlinedTextField(
                                    value = settings.pauseBadgeUrl,
                                    onValueChange = onPauseBadgeUrlChange,
                                    label = { Text("Paused Small Badge URL") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pause_badge_url_input"),
                                    colors = studioTextFieldColors(),
                                )
                                OutlinedButton(
                                    onClick = onResetAdvanced,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("reset_advanced_defaults_button"),
                                ) {
                                    Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore Default Assets")
                                }
                            }
                        }
                    }
                }
            }

            // Live Gateway Log Terminal
            item(key = "gateway_log_card") {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Terminal,
                                contentDescription = null,
                                tint = DiscordTextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Gateway Diagnostics",
                                style = MaterialTheme.typography.titleSmall,
                                color = DiscordTextPrimary,
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DiscordObsidian,
                            border = BorderStroke(1.dp, DiscordOutline),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (gatewayLogs.isEmpty()) {
                                    Text(
                                        text = "No gateway frames yet. Connect to inspect HELLO, IDENTIFY, READY, and OP 3 updates.",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 11.5.sp,
                                        color = DiscordTextMuted,
                                    )
                                } else {
                                    gatewayLogs.takeLast(10).forEach { line ->
                                        Text(
                                            text = line,
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = Color(0xFFB5E8C3),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "connect_bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AccountConnectionCard(
    connectionState: ConnectionState,
    hasToken: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val borderColor = when (connectionState) {
        is ConnectionState.Connected -> DiscordGreen.copy(alpha = 0.65f)
        is ConnectionState.Connecting -> DiscordAmber.copy(alpha = 0.65f)
        is ConnectionState.Error -> DiscordCrimson.copy(alpha = 0.7f)
        ConnectionState.Disconnected -> DiscordOutline
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DiscordCardSurface,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(DiscordSurfaceElevated),
                    contentAlignment = Alignment.Center,
                ) {
                    val avatarUrl = (connectionState as? ConnectionState.Connected)?.user?.avatarUrl
                    if (!avatarUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = avatarUrl,
                            contentDescription = "Discord user avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            tint = DiscordBlurple,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    when (connectionState) {
                        is ConnectionState.Connected -> {
                            Text(
                                text = connectionState.user.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = DiscordTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "@${connectionState.user.username} • Connected",
                                style = MaterialTheme.typography.bodySmall,
                                color = DiscordGreen,
                            )
                        }

                        is ConnectionState.Connecting -> {
                            Text(
                                text = "Connecting...",
                                style = MaterialTheme.typography.titleMedium,
                                color = DiscordAmber,
                            )
                            Text(
                                text = connectionState.stage,
                                style = MaterialTheme.typography.bodySmall,
                                color = DiscordTextSecondary,
                            )
                        }

                        is ConnectionState.Error -> {
                            Text(
                                text = "Connection Failed",
                                style = MaterialTheme.typography.titleMedium,
                                color = DiscordCrimson,
                            )
                            Text(
                                text = connectionState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFDADA),
                            )
                        }

                        ConnectionState.Disconnected -> {
                            Text(
                                text = if (hasToken) "Ready to Connect" else "Not Connected",
                                style = MaterialTheme.typography.titleMedium,
                                color = DiscordTextPrimary,
                            )
                            Text(
                                text = if (hasToken) {
                                    "Token saved locally. Tap Connect to start broadcasting."
                                } else {
                                    "Enter your Discord token below to broadcast live RPC."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = DiscordTextMuted,
                            )
                        }
                    }
                }
            }

            if (connectionState is ConnectionState.Connected || connectionState is ConnectionState.Connecting) {
                Button(
                    onClick = onDisconnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("connect_screen_disconnect_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiscordCrimson,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Rounded.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect Gateway")
                }
            } else {
                Button(
                    onClick = onConnect,
                    enabled = hasToken,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("connect_screen_connect_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiscordBlurple,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Rounded.BroadcastOnPersonal, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect & Broadcast Presence")
                }
            }
        }
    }
}
