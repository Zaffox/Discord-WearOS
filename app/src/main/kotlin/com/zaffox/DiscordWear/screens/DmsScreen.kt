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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun DmsScreen(
    onNavigateToChatScreen: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(state = listState) {
            item { Text("Direct Messages") }
            val dms = List(10) {"dms"}
            items(dms.size) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors   = ButtonDefaults.filledTonalButtonColors(),
                    onClick = onNavigateToChatScreen
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text  = "Name123",
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            )
                            Text(
                                text  = "hello, message",
                                style = TextStyle(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                     }
                }
             }
        }
    }
}
