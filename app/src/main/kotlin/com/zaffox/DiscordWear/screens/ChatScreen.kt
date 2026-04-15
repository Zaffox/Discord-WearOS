package com.zaffox.discordwear.screens

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender

data class Message(val author: String, val content: String, val isOwn: Boolean)

private const val INPUT_KEY = "message_input"

@Composable
fun ChatScreen(onNavigateToChatScreen: () -> Unit = {}) {
    val listState = rememberScalingLazyListState()
    val messages = remember {
        mutableStateListOf(
             Message("User1", "hello!", false),
             Message("Me", "hello!", true)
        )
    }

    val inputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val bundle: Bundle? = RemoteInput.getResultsFromIntent(result.data ?: return@rememberLauncherForActivityResult)
        val text = bundle?.getCharSequence(INPUT_KEY)?.toString()
         if (!text.isNullOrBlank()) {
            messages.add(Message("Me", text.trim(), true))
        }
    }

    fun openInput() {
        val remoteInput = RemoteInput.Builder(INPUT_KEY)
            .setLabel("Message Channel Name")
            .wearableExtender {
                setEmojisAllowed(true)
                setInputActionType(android.view.inputmethod.EditorInfo.IME_ACTION_SEND)
            }
            .build()

        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
        inputLauncher.launch(intent)
    }

    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text("Server name\nChanel Name"/*Server/Channel name?*/, style = MaterialTheme.typography.titleMedium)
            }

            items(messages.size) { index ->
                val msg = messages[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                     Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = msg.author,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = msg.content,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { openInput() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp), //yeah, thats not going to cause compatibility issues with text size /s
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text("Message 'Channel Name'")
                }
            }
            item {
                Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
                Button(
                    onClick = { openInput() },
                    modifier = Modifier
                        .height(30.dp), //yeah, thats not going to cause compatibility issues with text size /s
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text("Em")
                }
                Button(
                    onClick = { openInput() },
                    modifier = Modifier
                        .height(30.dp), //yeah, thats not going to cause compatibility issues with text size /s
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text("St")
                }
                }
            }
        }
    }
}
