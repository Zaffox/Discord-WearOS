package com.zaffox.discordwear.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.zaffox.discordwear.api.*
import com.zaffox.discordwear.discordApp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileScreen(
    userId: String,
    guildId: String? = null,
    initialUser: DiscordUser? = null,
    onNavigateToChat: (channelId: String, channelName: String) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repo = context.discordApp.repository
    val listState = rememberScalingLazyListState()
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    var user by remember { mutableStateOf(initialUser) }
    var loading by remember { mutableStateOf(initialUser == null) }
    var error by remember { mutableStateOf("") }
    var roles by remember { mutableStateOf<List<GuildRole>>(emptyList()) }
    var guildMember by remember { mutableStateOf<GuildMember?>(null) }
    val presences by (repo?.presences ?: return).collectAsState()
    val presence = presences[userId]

    BackHandler(onBack = onBack)

    LaunchedEffect(userId, guildId) {
        if (repo == null) return@LaunchedEffect
        loading = true
        repo.fetchUserProfile(userId)
            .onSuccess { user = it; loading = false }
            .onFailure {
                loading = false
                if (initialUser == null) error = "Could not load profile"
            }
        if (guildId != null) {
            guildMember = repo.getGuildMember(guildId, userId)
            roles = repo.getRolesForUser(guildId, userId)
        }
    }

    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                val u = user
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                    ) {
                        val bannerUrl = u?.bannerUrl(480)
                        if (bannerUrl != null) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(bannerUrl).crossfade(true).build(),
                                imageLoader = imageLoader,
                                contentDescription = "Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF5865F2), Color(0xFF3B429C))
                                        )
                                    )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(
                            user = u,
                            imageLoader = imageLoader,
                            size = 44,
                            presence = presence,
                            guildId = guildId,
                            guildMember = guildMember
                        )
                    }
                }
            }

            item {
                val u = user
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else if (u != null) {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = u.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            val clan = u.clan
                            if (clan != null) {
                                Row(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainer,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 3.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val badgeUrl = clan.badgeUrl(16)
                                    if (badgeUrl != null) {
                                        SubcomposeAsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(badgeUrl)
                                                .crossfade(true)
                                                .build(),
                                            imageLoader = imageLoader,
                                            contentDescription = null,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                    Text(
                                        text = clan.tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (u.username.isNotEmpty()) {
                            Text(
                                text = "@${u.username}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        val pronouns = u.pronouns
                        if (!pronouns.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = pronouns,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            if (presence != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusDot(status = presence.status, size = 8)
                        val statusLabel = when (presence.status) {
                            OnlineStatus.ONLINE -> "Online"
                            OnlineStatus.IDLE -> "Idle"
                            OnlineStatus.DND -> "Do Not Disturb"
                            OnlineStatus.INVISIBLE, OnlineStatus.OFFLINE -> "Offline"
                        }
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val customText = presence.customStatusText
                val customEmoji = presence.customStatusEmoji
                if (!customText.isNullOrBlank() || customEmoji != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (customEmoji != null) {
                                if (customEmoji.startsWith("http")) {
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(context).data(customEmoji)
                                            .crossfade(true).build(),
                                        imageLoader = imageLoader,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Text(customEmoji, fontSize = 11.sp, lineHeight = 13.sp)
                                }
                            }
                            if (!customText.isNullOrBlank()) {
                                Text(
                                    text = customText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            val badges = user?.badges ?: emptyList()
            if (badges.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        badges.forEach { badgeUrl ->
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(badgeUrl)
                                    .crossfade(true)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = "Badge",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (roles.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ROLES",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            roles.forEach { role ->
                                val roleColor =
                                    if (role.color != 0) Color(0xFF000000.toInt() or role.color) else MaterialTheme.colorScheme.onSurface
                                Box(
                                    modifier = Modifier
                                        .background(
                                            roleColor.copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = role.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = roleColor,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val bio = user?.bio
            if (!bio.isNullOrBlank()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "ABOUT ME",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(3.dp))
                        BioContent(
                            content = bio,
                            imageLoader = imageLoader,
                            context = context
                        )
                    }
                }
            }

            if (error.isNotEmpty()) {
                item {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BioContent(
    content: String,
    imageLoader: ImageLoader,
    context: android.content.Context
) {
    val parts = remember(content) {
        ContentParser.parse(content, emptyMap(), emptyMap(), emptyMap())
    }

    FlowRow(modifier = Modifier.fillMaxWidth()) {
        parts.forEach { part ->
            when (part) {
                is ContentParser.Part.PlainText -> {
                    val spans = ContentParser.parseMarkdown(part.text)
                    spans.forEach { span ->
                        if (span.spoiler) {
                            Text(
                                text = "████",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val annotated = buildAnnotatedString {
                                val style = SpanStyle(
                                    fontWeight = if (span.bold) androidx.compose.ui.text.font.FontWeight.Bold else null,
                                    fontStyle = if (span.italic) androidx.compose.ui.text.font.FontStyle.Italic else null,
                                    textDecoration = when {
                                        span.strikethrough -> androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        else -> null
                                    }
                                )
                                withStyle(style) { append(span.text) }
                            }
                            Text(
                                text = annotated,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                is ContentParser.Part.CustomEmoji -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(part.url).crossfade(true).build(),
                        imageLoader = imageLoader,
                        contentDescription = part.name,
                        modifier = Modifier.size(16.dp)
                    )
                }

                is ContentParser.Part.UserMention -> {
                    Text(
                        text = "@${part.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is ContentParser.Part.RoleMention -> {
                    Text(
                        text = "@${part.roleName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                is ContentParser.Part.ChannelMention -> {
                    Text(
                        text = "#${part.channelName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                is ContentParser.Part.Link -> {
                    Text(
                        text = part.url,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    user: DiscordUser?,
    imageLoader: ImageLoader,
    size: Int,
    presence: UserPresence?,
    guildId: String? = null,
    guildMember: GuildMember? = null
) {
    val sizeDp = size.dp
    val blurple = Color(0xFF5865F2)
    val initial = user?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val avatarUrl = guildMember?.avatarUrl(guildId ?: "", 128) ?: user?.avatarUrl(128)
    val decorUrl = guildMember?.avatarDecorationUrl() ?: user?.avatarDecorationUrl()

    Box(contentAlignment = Alignment.Center) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(sizeDp + 4.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl).crossfade(true).build(),
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(sizeDp)
                            .clip(CircleShape),
                        error = {
                            Box(
                                modifier = Modifier
                                    .size(sizeDp)
                                    .background(blurple, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(initial, color = Color.White, fontSize = (size * 0.4f).sp)
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(sizeDp)
                            .background(blurple, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initial, color = Color.White, fontSize = (size * 0.4f).sp)
                    }
                }
            }

            if (presence != null) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.background, CircleShape)
                        .padding(2.dp)
                ) {
                    StatusDot(status = presence.status, size = 8)
                }
            }
        }
        if (decorUrl != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(decorUrl).crossfade(false).build(),
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(sizeDp * 1.4f)
            )
        }
    }
}

@Composable
private fun StatusDot(status: OnlineStatus, size: Int) {
    val color = when (status) {
        OnlineStatus.ONLINE -> Color(0xFF23A559)
        OnlineStatus.IDLE -> Color(0xFFF0B232)
        OnlineStatus.DND -> Color(0xFFF23F43)
        OnlineStatus.INVISIBLE, OnlineStatus.OFFLINE -> Color(0xFF80848E)
    }
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(color, CircleShape)
    )
}
