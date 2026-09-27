package com.zaffox.discordwear.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.zaffox.discordwear.R
import com.zaffox.discordwear.SetupPreferences
import com.zaffox.discordwear.api.Channel
import com.zaffox.discordwear.api.ChannelType
import com.zaffox.discordwear.discordApp
import kotlinx.coroutines.launch

@Composable
private fun ChannelIcon(ch: Channel, allChannels: List<Channel>) {
    when {
        !ch.hasAccess ->
            Icon(
                painter = painterResource(id = R.drawable.lock),
                contentDescription = "Locked",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )

        ch.type == ChannelType.GUILD_NEWS ->
            Icon(
                painter = painterResource(id = R.drawable.announce),
                contentDescription = "Announcement",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )

        ch.name.contains("rule", ignoreCase = true) &&
                allChannels.firstOrNull {
                    it.name.contains(
                        "rule",
                        ignoreCase = true
                    )
                }?.id == ch.id ->
            Icon(
                painter = painterResource(id = R.drawable.rules),
                contentDescription = "Rules",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )

        else ->
            Text("#", fontSize = 16.sp)
    }
}

@Composable
fun ServerChannels(
    guildId: String,
    guildName: String,
    onNavigateToChatScreen: (channelId: String, channelName: String) -> Unit
) {
    val context = LocalContext.current
    val repo = context.discordApp.repository
    val listState = rememberScalingLazyListState()
    val scope = rememberCoroutineScope()
    var hideInaccessible by remember {
        mutableStateOf(
            SetupPreferences.getHideInaccessibleChannels(
                context
            )
        )
    }
    val showMentionBadges = remember { SetupPreferences.getShowMentionBadges(context) }
    val readState by (repo?.readState ?: return).collectAsState()
    val guildChannelsMap by repo.guildChannelsMap.collectAsState()

    val groups = guildChannelsMap[guildId] ?: remember {
        repo.getCachedChannels(
            guildId,
            filterInaccessible = hideInaccessible
        )
    } ?: emptyList()
    var loading by remember { mutableStateOf(groups.isEmpty()) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(guildId) {
        hideInaccessible = SetupPreferences.getHideInaccessibleChannels(context)
        scope.launch {
            if (groups.isEmpty()) {
                val cached = repo?.getCachedChannels(guildId, filterInaccessible = hideInaccessible)
                if (!cached.isNullOrEmpty()) {
                    repo?.cacheChannelNames(cached)
                }
            }
            repo?.refreshGuildChannels(guildId, filterInaccessible = hideInaccessible)
                ?.onSuccess {
                    loading = false
                }
                ?.onFailure {
                    if (groups.isEmpty()) {
                        error = it.message ?: "Error"
                        loading = false
                    }
                }
                ?: run {
                    if (groups.isEmpty()) {
                        error = "Not connected"
                        loading = false
                    }
                }
            loading = false
        }
    }

    val allChannels = remember(groups) { groups.flatMap { it.channels } }

    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(state = listState) {
            item {
                Text(guildName, style = MaterialTheme.typography.titleMedium)
            }

            when {
                loading -> item { CircularProgressIndicator() }
                error.isNotEmpty() -> item {
                    Text(
                        error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                groups.isEmpty() -> item {
                    Text("No text channels.", style = MaterialTheme.typography.bodySmall)
                }

                else -> {
                    for (group in groups) {
                        if (group.category != null) {
                            item {
                                Text(
                                    text = "▸ ${group.category.name.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                        }

                        items(group.channels.size) { idx ->
                            val ch = group.channels[idx]
                            if (!ch.hasAccess && hideInaccessible) return@items
                            val rs = if (showMentionBadges) readState[ch.id] else null
                            val mentionCount = rs?.mentionCount ?: 0
                            val hasUnread = rs != null && ch.lastMessageId != null &&
                                    ch.lastMessageId > rs.lastMessageId
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                onClick = { onNavigateToChatScreen(ch.id, ch.name) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ChannelIcon(ch, allChannels)
                                        Text(
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            text = ch.name,
                                            color = if (ch.hasAccess) Color.White else Color.White.copy(
                                                alpha = 0.5f
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (mentionCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                                .background(Color(0xFFF23F43), CircleShape)
                                                .padding(horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (mentionCount > 99) "99+" else mentionCount.toString(),
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        }
                                    } else if (hasUnread) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(
                                                    Color.White.copy(alpha = 0.8f),
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
