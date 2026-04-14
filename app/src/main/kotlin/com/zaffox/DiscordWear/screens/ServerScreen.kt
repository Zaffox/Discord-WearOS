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

@Composable
fun ServerScreen(
onNavigateToChannels: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(state = listState) {
            item { Text("Direct Messages") }
            val servers = List(10) {"server"}
            items(servers.size) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors   = ButtonDefaults.filledTonalButtonColors(),
                    onClick = {onNavigateToChannels()}
                ) {
                   Text("Sever Name")
                }
             }
        }
    }
}
