package com.zaffox.discordwear.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.zaffox.discordwear.RemoteAuthClient
import com.zaffox.discordwear.RemoteAuthState
import com.zaffox.discordwear.RemoteAuthStatus
import com.zaffox.discordwear.SetupPreferences
import com.zaffox.discordwear.discordApp

@Composable
fun QrLoginScreen(onSetupComplete: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()

    var state  by remember { mutableStateOf<RemoteAuthState>(RemoteAuthState.Connecting) }
    var status by remember { mutableStateOf(RemoteAuthStatus()) }
    var client by remember { mutableStateOf<RemoteAuthClient?>(null) }

    LaunchedEffect(Unit) {
        val c = RemoteAuthClient(
            onStateChange  = { newState  -> state  = newState  },
            onStatusUpdate = { newStatus -> status = newStatus },
            onTokenReceived = { token ->
                SetupPreferences.saveToken(context, token)
                context.discordApp.initRepository(token)
                onSetupComplete()
            }
        )
        client = c
        c.connect()
    }

    DisposableEffect(Unit) {
        onDispose { client?.disconnect() }
    }

    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            when (val s = state) {

                is RemoteAuthState.Connecting -> {
                    item {
                        Text(
                            "Connecting…",
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                    item { CircularProgressIndicator(modifier = Modifier.size(28.dp)) }
                    // Verbose log lines
                    items(status.lines.size) { i ->
                        Text(
                            status.lines[i],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 1.dp)
                        )
                    }
                    item {
                        Button(
                            onClick = { client?.disconnect(); onBack() },
                            modifier = Modifier.fillMaxWidth(0.7f).height(32.dp),
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) { Text("Cancel") }
                    }
                }

                is RemoteAuthState.WaitingForScan -> {
                    item {
                        Text(
                            "Scan with Discord",
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                    item {
                        QrCodeImage(
                            content = "https://discord.com/ra/${s.fingerprint}",
                            modifier = Modifier
                                .size(130.dp)
                                .background(Color.White)
                                .padding(4.dp)
                        )
                    }
                    item {
                        Text(
                            "Profile → Scan QR Code in the Discord app",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    }
                    item {
                        Button(
                            onClick = { client?.disconnect(); onBack() },
                            modifier = Modifier.fillMaxWidth(0.7f).height(32.dp),
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) { Text("Cancel") }
                    }
                }

                is RemoteAuthState.UserScanned -> {
                    item {
                        Text(
                            "Confirm on your phone",
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                    item {
                        val avatarUrl = if (s.avatarHash != "0")
                            "https://cdn.discordapp.com/avatars/${s.userId}/${s.avatarHash}.png?size=80"
                        else null
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(48.dp).clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    s.username.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    item {
                        Text(
                            s.username,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        Text(
                            "Tap 'Log In' in the Discord app",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    }
                    item { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                }

                is RemoteAuthState.Canceled -> {
                    item {
                        Text("Canceled", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                    }
                    item {
                        Text("Login was canceled on your phone.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                    item {
                        Button(onClick = { onBack() }, modifier = Modifier.fillMaxWidth(0.7f).height(32.dp)) { Text("Go Back") }
                    }
                }

                is RemoteAuthState.Error -> {
                    item {
                        Text("Error", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                    item {
                        Text(s.message, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
                    }
                    // Show log even on error so we can debug
                    items(status.lines.size) { i ->
                        Text(
                            status.lines[i],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 1.dp)
                        )
                    }
                    item {
                        Button(onClick = { onBack() }, modifier = Modifier.fillMaxWidth(0.7f).height(32.dp)) { Text("Go Back") }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCodeImage(content: String, modifier: Modifier = Modifier) {
    val bitmap = remember(content) { generateQrBitmap(content, 256) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? = runCatching {
    val hints = mapOf(EncodeHintType.MARGIN to 0)
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size)
        bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    bmp
}.getOrNull()
