package com.zaffox.discordwear.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import com.zaffox.discordwear.SetupPreferences  // add this

@Composable
fun HomeScreen(
    onNavigateToDms: () -> Unit,
    onNavigateToServers: () -> Unit,
    onNavigateToWelcome: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()

    LaunchedEffect(Unit) {
        if (!SetupPreferences.isSetupComplete(context)) {
            onNavigateToWelcome()
        }
    }

    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(state = listState) {
            item { Text("Discord") }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToDms,
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) { Text("Direct Messages") }
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToServers,
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) { Text("Servers") }
            }
            item { Text("Notifications") }
            item {
                TitleCard(
                    modifier = Modifier,   // was: modifier (undefined)
                    time = { Text("11m") },
                    title = { Text("Server Admin\n Server Name") },
                    onClick = {}
                ) { Text("Announcements: Lorem ipsum dolor sit amet.") }
            }
            item {
                TitleCard(
                    modifier = Modifier,
                    time = { Text("12m") },
                    title = { Text("Name") },
                    onClick = {}
                ) { Text("Lorem ipsum dolor sit amet.") }
            }
        }
    }
}
