package com.zaffox.discordwear.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import com.zaffox.discordwear.SetupPreferences
import com.zaffox.discordwear.api.GatewayEvent
import com.zaffox.discordwear.discordApp

@Composable
fun HomeScreen(
    onNavigateToDms: () -> Unit,
    onNavigateToServers: () -> Unit,
    onNavigateToWelcome: () -> Unit
) {
    val context   = LocalContext.current
    val listState = rememberScalingLazyListState()

    LaunchedEffect(Unit) {
        if (!SetupPreferences.isSetupComplete(context)) {
            onNavigateToWelcome()
        }
    }

    val repo        = context.discordApp.repository
    val currentUser by (repo?.currentUser ?: return).collectAsState()

    // Collect a few recent notifications from the gateway
    val recentMessages = remember { mutableStateListOf<String>() }
    LaunchedEffect(repo) {
        repo.gatewayEvents.collect { event ->
            if (event is GatewayEvent.MessageCreate && recentMessages.size < 5) {
                recentMessages.add(0, "${event.message.author.displayName}: ${event.message.content.take(40)}")
                if (recentMessages.size > 5) recentMessages.removeLastOrNull()
            }
        }
    }

    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(state = listState) {
            item {
                Text(
                    text  = if (currentUser != null) "Hi, ${currentUser!!.displayName}" else "Discord",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick  = onNavigateToDms,
                    colors   = ButtonDefaults.filledTonalButtonColors()
                ) { Text("Direct Messages") }
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick  = onNavigateToServers,
                    colors   = ButtonDefaults.filledTonalButtonColors()
                ) { Text("Servers") }
            }

            if (recentMessages.isNotEmpty()) {
                item { Text("Recent", style = MaterialTheme.typography.labelMedium) }
                items(recentMessages.size) { index ->
                    TitleCard(
                        modifier = Modifier,
                        time     = { },
                        title    = { },
                        onClick  = {}
                    ) { Text(recentMessages[index], style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
