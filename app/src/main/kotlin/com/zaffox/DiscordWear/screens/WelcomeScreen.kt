package com.zaffox.discordwear.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import com.zaffox.discordwear.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.zaffox.discordwear.SetupPreferences

@Composable
fun WelcomeScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(state = listState) {
            item { Text("Discord") }
            item { Text("Welcome to DiscordWear!") }
            item {
                Button(onClick = {
                    SetupPreferences.saveToken(context, "TOKEN")
                    
                    onSetupComplete()
                }) {
                     Text("Get Started")
                }
            }
        }
    }
}
