package com.zaffox.discordwear.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.zaffox.discordwear.R
import com.zaffox.discordwear.api.DiscordMessage
import com.zaffox.discordwear.api.DiscordUser
import com.zaffox.discordwear.api.GuildRole
import com.zaffox.discordwear.discordApp
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.zaffox.discordwear.SetupPreferences
import com.zaffox.discordwear.api.ReactionEmoji
import java.io.File
import kotlinx.coroutines.delay

/**
 * Full-screen thread viewer.
 * Shows messages in the thread and lets the user reply to the thread.
 *
 * Called from ChatScreen when the user taps "Open Thread" in MessageOptionsDialog
 * or taps the thread count chip on a message.
 *
 * Navigation: push "thread/{threadId}/{threadName}" onto the nav stack.
 */
@Composable
fun ThreadScreen(
    threadId: String,
    threadName: String,
    guildId: String? = null,
    onNavigateToProfile: ((userId: String, user: DiscordUser?) -> Unit)? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val repo = context.discordApp.repository ?: return
    val listState = rememberScalingLazyListState()
    val scope = rememberCoroutineScope()

    val imageLoader = remember {
        ImageLoader.Builder(context).components {
            if (android.os.Build.VERSION.SDK_INT >= 28)
                add(ImageDecoderDecoder.Factory())
            else
                add(GifDecoder.Factory())
        }.build()
    }

    val allMessages by repo.messages.collectAsState()
    val threadMessages = allMessages[threadId].orEmpty()
    val currentUser by repo.currentUser.collectAsState()
    val myId = currentUser?.id ?: ""

    var loading by remember { mutableStateOf(true) }
    var sendError by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf("") }
    var pendingText by remember { mutableStateOf("") }
    var selectedMsg by remember { mutableStateOf<DiscordMessage?>(null) }
    var replyingTo by remember { mutableStateOf<DiscordMessage?>(null) }
    var reactingToMsg by remember { mutableStateOf<DiscordMessage?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    val roleColorCache = remember { mutableStateMapOf<String, GuildRole?>() }
    val channelNames = remember(threadMessages.size, loading) { repo.getChannelNames() }

    var uploadError by remember { mutableStateOf("") }
    var showPhotoPicker by remember { mutableStateOf(false) }
    val imagePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showPhotoPicker = true
        else uploadError = "Photo permission denied"
    }

    var isRecording by remember { mutableStateOf(false) }
    var recordingSecs by remember { mutableStateOf(0) }
    var voiceFile by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) uploadError = "Microphone permission denied"
    }

    fun startRecording() {
        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        if (!hasAudio) { micPermLauncher.launch(Manifest.permission.RECORD_AUDIO); return }
        try {
            val f = File(context.cacheDir, "voice_${System.currentTimeMillis()}.ogg")
            voiceFile = f
            val mr = if (android.os.Build.VERSION.SDK_INT >= 31) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.OGG)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            mr.setOutputFile(f.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            isRecording = true
            recordingSecs = 0
        } catch (e: Exception) {
            uploadError = "Mic error: ${e.message}"
            isRecording = false
        }
    }

    fun stopAndSendRecording() {
        val mr = recorder ?: return
        val f = voiceFile ?: return
        val dur = recordingSecs.toDouble()
        try {
            mr.stop()
            mr.release()
        } catch (_: Exception) {}
        recorder = null
        isRecording = false
        scope.launch {
            runCatching {
                val bytes = f.readBytes()
                f.delete()
                repo.sendVoiceMessage(threadId, bytes, dur)
                    .onFailure { uploadError = "Voice send failed: ${it.message}" }
            }.onFailure { uploadError = "Error: ${it.message}" }
        }
    }

    fun cancelRecording() {
        val mr = recorder ?: return
        try { mr.stop(); mr.release() } catch (_: Exception) {}
        recorder = null
        isRecording = false
        voiceFile?.delete()
        voiceFile = null
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1_000)
                recordingSecs++
                if (recordingSecs >= 120) { stopAndSendRecording(); break }
            }
        }
    }

    LaunchedEffect(threadId) {
        scope.launch {
            repo.loadMessages(threadId)
            loading = false
        }
    }

    LaunchedEffect(threadMessages, guildId, loading) {
        if (!loading && guildId != null) {
            threadMessages.map { it.author.id }.distinct()
                .filterNot { roleColorCache.containsKey(it) }
                .forEach { userId ->
                    roleColorCache[userId] = repo.getTopRoleForUser(guildId, userId)
                }
        }
    }

    BackHandler(enabled = isRecording) {
        cancelRecording()
    }
    BackHandler(enabled = showPhotoPicker) {
        showPhotoPicker = false
    }
    BackHandler(enabled = showPicker) {
        showPicker = false
        reactingToMsg = null
    }
    BackHandler(enabled = !isRecording && !showPhotoPicker && !showPicker) { onBack() }

    val hasNitro = currentUser?.hasNitro ?: false
    val sendAnimatedAsGif = SetupPreferences.getSendAnimatedAsGif(context)

    if (showPicker) {
        val isReactMode = reactingToMsg != null
        EmojiStickerScreen(
            tab = if (isReactMode) 0 else tab,
            guildId = guildId,
            hasNitro = hasNitro,
            sendAnimatedAsGif = sendAnimatedAsGif,
            reactMode = isReactMode,
            onEmojiPicked = { insertText ->
                showPicker = false
                val target = reactingToMsg
                if (target != null) {
                    val reactionEmoji = parseInsertTextToReactionEmoji(insertText)
                    if (reactionEmoji != null) {
                        scope.launch {
                            try {
                                repo.toggleReaction(threadId, target.id, reactionEmoji)
                            } catch (e: Exception) {
                                sendError = "Failed: ${e.message}"
                            }
                        }
                    }
                    reactingToMsg = null
                } else {
                    val isAnimated = insertText.startsWith("<a:")
                    if (isAnimated && sendAnimatedAsGif) {
                        showPicker = false
                        val link = buildEmojiLink(insertText)
                        scope.launch {
                            repo.sendMessage(threadId, link)
                                .onFailure { sendError = "Failed: ${it.message}" }
                        }
                    } else {
                        val newText = if (inputText.isBlank()) insertText else "$inputText$insertText"
                        inputText = newText
                        pendingText = newText
                    }
                }
            },
            onUnicodeEmojiPicked = { unicode ->
                showPicker = false
                val target = reactingToMsg
                if (target != null) {
                    val reactionEmoji = ReactionEmoji(id = null, name = unicode, animated = false)
                    scope.launch {
                        try { repo.toggleReaction(threadId, target.id, reactionEmoji) }
                        catch (e: Exception) { sendError = "Failed: ${e.message}" }
                    }
                    reactingToMsg = null
                } else {
                    val newText = if (inputText.isBlank()) unicode else "$inputText$unicode"
                    inputText = newText
                    pendingText = newText
                }
            },
            onStickerPicked = { stickerId ->
                showPicker = false
                reactingToMsg = null
                scope.launch {
                    try {
                        repo.sendSticker(threadId, stickerId)
                    } catch (e: Exception) {
                        sendError = "Failed: ${e.message}"
                    }
                }
            }
        )
        return
    }

    if (showPhotoPicker) {
        PhotoPickerScreen(
            imageLoader = imageLoader,
            onImageSelected = { uri, mime ->
                showPhotoPicker = false
                scope.launch {
                    runCatching {
                        val cr = context.contentResolver
                        val ext = when {
                            mime.contains("png") -> "png"
                            mime.contains("gif") -> "gif"
                            mime.contains("webp") -> "webp"
                            else -> "jpg"
                        }
                        val bytes = cr.openInputStream(uri)?.readBytes()
                            ?: throw Exception("Cannot read image")
                        repo.sendFileAttachment(threadId, bytes, "image.$ext", mime)
                            .onFailure { uploadError = "Upload failed: ${it.message}" }
                    }.onFailure { uploadError = "Error: ${it.message}" }
                }
            },
            onDismiss = { showPhotoPicker = false }
        )
        return
    }

    // Message options overlay
    val msgForOptions = selectedMsg
    if (msgForOptions != null) {
        val threadListState = rememberScalingLazyListState()
        ScreenScaffold(scrollState = threadListState) {
            ScalingLazyColumn(state = threadListState, modifier = Modifier.fillMaxSize()) {
                item {
                    Text("Message by ${msgForOptions.author.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Button(onClick = { replyingTo = msgForOptions; selectedMsg = null },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.filledTonalButtonColors()) {
                        Icon(painter = painterResource(id = R.drawable.reply),
                            contentDescription = null, tint = Color.White,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reply")
                    }
                }
                item {
                    Button(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("message", msgForOptions.content))
                        selectedMsg = null
                    }, modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.filledTonalButtonColors()) {
                        Icon(painter = painterResource(id = R.drawable.copy),
                            contentDescription = null, tint = Color.White,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy")
                    }
                }
                if (msgForOptions.author.id == myId) {
                    item {
                        Button(onClick = {
                            scope.launch {
                                repo.deleteMessage(threadId, msgForOptions.id)
                                    .onFailure { sendError = "Delete failed" }
                            }
                            selectedMsg = null
                        }, modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error)) {
                            Icon(painter = painterResource(id = R.drawable.delete),
                                contentDescription = null, tint = Color.White,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }
                item {
                    Button(onClick = { selectedMsg = null },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors()) { Text("Cancel") }
                }
            }
        }
        return
    }

    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "thread_title") {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(painter = painterResource(id = R.drawable.reply),
                        contentDescription = null, tint = Color(0xFF5865F2),
                        modifier = Modifier.size(14.dp))
                    Text("# $threadName", style = MaterialTheme.typography.titleSmall)
                }
            }

            when {
                loading -> item { CircularProgressIndicator() }
                threadMessages.isEmpty() -> item {
                    Text("No messages in thread.", style = MaterialTheme.typography.bodySmall)
                }
                else -> items(threadMessages.size, key = { threadMessages[it].id }) { index ->
                    val msg = threadMessages[index]
                    val prevMsg = if (index > 0) threadMessages[index - 1] else null
                    fun String.toEpochMs(): Long = runCatching {
                        java.time.OffsetDateTime.parse(this).toInstant().toEpochMilli()
                    }.getOrElse { 0L }
                    val gapMs = if (prevMsg != null)
                        msg.timestamp.toEpochMs() - prevMsg.timestamp.toEpochMs()
                    else Long.MAX_VALUE
                    val isContinuation = prevMsg != null &&
                        prevMsg.author.id == msg.author.id && gapMs < 10 * 60 * 1000L

                    MessageBubble(
                        msg = msg,
                        isOwn = msg.author.id == myId,
                        isContinuation = isContinuation,
                        imageLoader = imageLoader,
                        channelNames = channelNames,
                        guildId = guildId,
                        roleColorCache = roleColorCache,
                        onReact = { emoji ->
                            scope.launch { repo.toggleReaction(threadId, msg.id, emoji) }
                        },
                        onSwipeLeft = { replyingTo = msg },
                        onLongPress = { selectedMsg = msg },
                        onAvatarClick = { userId ->
                            onNavigateToProfile?.invoke(userId, msg.author.takeIf { it.id == userId })
                        }
                    )
                }
            }

            if (sendError.isNotEmpty()) {
                item(key = "error") {
                    Text(sendError, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { sendError = "" })
                }
            }

            // Reply banner
            item(key = "reply_banner") {
                if (replyingTo != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("↩ ${replyingTo!!.author.displayName}: ${replyingTo!!.content.take(30)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f))
                        Text("X", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.clickable { replyingTo = null }.padding(4.dp))
                    }
                }
            }

            // Text input
            item(key = "text_input") {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it; pendingText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Reply in thread", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    minLines = 1, maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }

            item(key = "action_buttons") {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth().padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                        FilledIconButton(
                            onClick = {
                                reactingToMsg = null
                                showPicker = true
                                tab = 0
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(painter = painterResource(id = R.drawable.emoji),
                                contentDescription = "Emoji")
                        }
                        FilledIconButton(
                            onClick = {
                                reactingToMsg = null
                                showPicker = true
                                tab = 1
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(painter = painterResource(id = R.drawable.sticker),
                                contentDescription = "Stickers")
                        }
                        FilledIconButton(
                            onClick = {
                                val text = inputText.trim()
                                if (text.isBlank()) return@FilledIconButton
                                inputText = ""
                                pendingText = ""
                                scope.launch {
                                    val replyTarget = replyingTo
                                    if (replyTarget != null) {
                                        repo.sendReply(threadId, text, replyTarget.id)
                                            .onFailure { sendError = "Failed: ${it.message}" }
                                        replyingTo = null
                                    } else {
                                        repo.sendMessage(threadId, text)
                                            .onFailure { sendError = "Failed: ${it.message}" }
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(painter = painterResource(id = R.drawable.send),
                                contentDescription = "Send")
                        }
                    }

                    if (!isRecording) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(space = 16.dp, alignment = Alignment.CenterHorizontally)
                        ) {
                            FilledIconButton(
                                onClick = {
                                    val perm = if (android.os.Build.VERSION.SDK_INT >= 33)
                                        Manifest.permission.READ_MEDIA_IMAGES
                                    else
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    val hasPerm = ContextCompat.checkSelfPermission(context, perm) ==
                                            PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        showPhotoPicker = true
                                    } else {
                                        imagePermLauncher.launch(perm)
                                    }
                                },
                                modifier = Modifier.height(40.dp).width(40.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.image),
                                    contentDescription = "Upload Photo"
                                )
                            }
                            FilledIconButton(
                                onClick = { startRecording() },
                                modifier = Modifier.height(40.dp).width(40.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.mic),
                                    contentDescription = "Voice Message"
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val mins = recordingSecs / 60
                            val secs = recordingSecs % 60
                            Text(
                                text = "%02d:%02d".format(mins, secs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { cancelRecording() },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    colors = ButtonDefaults.outlinedButtonColors()
                                ) { Text("Cancel", fontSize = 12.sp) }
                                Spacer(Modifier.width(6.dp))
                                Button(
                                    onClick = { stopAndSendRecording() },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors()
                                ) { Text("Send", fontSize = 11.sp) }
                            }
                        }
                    }
                    if (uploadError.isNotEmpty()) {
                        Text(
                            uploadError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.clickable { uploadError = "" }
                        )
                    }
                }
            }
        }
    }
}

private fun buildEmojiLink(insertText: String): String {
    val animated = Regex("""<a:(\w+):(\d+)>""").find(insertText) ?: return insertText
    val id = animated.groupValues[2]
    return "https://cdn.discordapp.com/emojis/$id.webp?size=80"
}


