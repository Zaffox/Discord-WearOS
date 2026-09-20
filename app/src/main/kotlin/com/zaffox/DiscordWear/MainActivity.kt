package com.zaffox.discordwear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.zaffox.discordwear.screens.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    var activeChannelId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = SetupPreferences.getToken(this)
        if (token != null) discordApp.initRepository(token)

        UpdateChecker.checkNow(this)

        setContent {
            MaterialTheme {
                val updateState by UpdateChecker.state.collectAsState()
                var dismissUpdate by remember { mutableStateOf(false) }

                AppScaffold(timeText = { TimeText() }) {
                    if (updateState is UpdateChecker.UpdateState.UpdateAvailable && !dismissUpdate) {
                        val release = (updateState as UpdateChecker.UpdateState.UpdateAvailable).release
                        val dialogListState = rememberScalingLazyListState()
                        var downloading by remember { mutableStateOf(false) }
                        var downloadProgress by remember { mutableStateOf(0f) }
                        var downloadError by remember { mutableStateOf("") }
                        val scope = rememberCoroutineScope()

                        androidx.wear.compose.material3.ScreenScaffold(scrollState = dialogListState) {
                            ScalingLazyColumn(state = dialogListState, modifier = Modifier.fillMaxSize()) {
                                item {
                                    androidx.wear.compose.material3.Text(
                                        "Update Available",
                                        style = MaterialTheme.typography.titleSmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                item {
                                    androidx.wear.compose.material3.Text(
                                        "Version ${release.tagName} is available.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                    )
                                }
                                if (release.apkUrl != null) {
                                    item {
                                        androidx.wear.compose.material3.Button(
                                            onClick = {
                                                if (!downloading) {
                                                    downloading = true
                                                    downloadProgress = 0f
                                                    downloadError = ""
                                                    scope.launch {
                                                        ApkInstaller.downloadAndInstall(
                                                            context = this@MainActivity,
                                                            url = release.apkUrl,
                                                            onProgress = { p -> downloadProgress = p }
                                                        ).onFailure { downloadError = it.message ?: "Download failed" }
                                                        downloading = false
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            enabled = !downloading,
                                            colors = androidx.wear.compose.material3.ButtonDefaults.buttonColors()
                                        ) {
                                            androidx.wear.compose.material3.Text(
                                                if (downloading) "Downloading…" else "Download & Install",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    if (downloading) {
                                        item {
                                            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                                LinearProgressIndicator(
                                                    progress = { downloadProgress },
                                                    modifier = Modifier.fillMaxWidth().height(4.dp)
                                                )
                                                androidx.wear.compose.material3.Text(
                                                    text = "${(downloadProgress * 100).toInt()}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                    if (downloadError.isNotEmpty()) {
                                        item {
                                            androidx.wear.compose.material3.Text(
                                                downloadError,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                item {
                                    androidx.wear.compose.material3.Button(
                                        onClick = { ApkInstaller.openInPhoneBrowser(this@MainActivity, release.htmlUrl) },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = androidx.wear.compose.material3.ButtonDefaults.filledTonalButtonColors()
                                    ) {
                                        androidx.wear.compose.material3.Text("Open on phone", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                item {
                                    androidx.wear.compose.material3.Button(
                                        onClick = { dismissUpdate = true },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = androidx.wear.compose.material3.ButtonDefaults.outlinedButtonColors()
                                    ) {
                                        androidx.wear.compose.material3.Text("Remind me later", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    val navController = rememberSwipeDismissableNavController()
                    SwipeDismissableNavHost(
                        navController = navController,
                        startDestination = if (token != null) "home" else "Welcome"
                    ) {

                        composable("home") {
                            HomeScreen(
                                onNavigateToDms = { navController.navigate("DMs") },
                                onNavigateToServers = { navController.navigate("servers") },
                                onNavigateToWelcome = { navController.navigate("Welcome") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToChat = { chId, chName, guildId ->
                                    val guildSeg = guildId ?: "dm"
                                    navController.navigate("chatscreen/$chId/$chName/$guildSeg")
                                }
                            )
                        }

                        composable("Welcome") {
                            WelcomeScreen(onSetupComplete = {
                                navController.navigate("home") {
                                    popUpTo("Welcome") { inclusive = true }
                                }
                            }, onNavigateToQrLogin = {
                                navController.navigate("qrlogin")
                            })
                        }

                        composable("qrlogin") {
                            QrLoginScreen(
                                onSetupComplete = {
                                    navController.navigate("home") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onLogOut = {
                                    navController.navigate("Welcome") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("chatscreen/{channelId}/{channelName}/{guildId}") { back ->
                            val channelId = back.arguments?.getString("channelId")   ?: return@composable
                            val channelName = back.arguments?.getString("channelName") ?: channelId
                            val guildIdArg = back.arguments?.getString("guildId")
                            val guildId = if (guildIdArg == "dm") null else guildIdArg
                            activeChannelId = channelId

                            ChatScreen(
                                channelId = channelId,
                                channelName = channelName,
                                guildId = guildId,
                                onNavigateToProfile = { userId, user ->
                                    val encodedName = java.net.URLEncoder.encode(user?.displayName ?: userId, "UTF-8")
                                    navController.navigate("userprofile/$userId/$encodedName")
                                },
                                onNavigateToThread = { threadId, threadName ->
                                    val encodedName = java.net.URLEncoder.encode(threadName, "UTF-8")
                                    navController.navigate("thread/$threadId/$encodedName/${guildIdArg ?: "dm"}")
                                }
                            )
                        }

                        composable("thread/{threadId}/{threadName}/{guildId}") { back ->
                            val threadId = back.arguments?.getString("threadId") ?: return@composable
                            val threadName = back.arguments?.getString("threadName")
                                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: threadId
                            val guildIdArg = back.arguments?.getString("guildId")
                            val guildId = if (guildIdArg == "dm") null else guildIdArg
                            activeChannelId = threadId
                            ThreadScreen(
                                threadId = threadId,
                                threadName = threadName,
                                guildId = guildId,
                                onNavigateToProfile = { userId, user ->
                                    val encodedName = java.net.URLEncoder.encode(user?.displayName ?: userId, "UTF-8")
                                    navController.navigate("userprofile/$userId/$encodedName")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("userprofile/{userId}/{displayName}") { back ->
                            val userId = back.arguments?.getString("userId") ?: return@composable
                            val displayName = back.arguments?.getString("displayName")
                                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: userId
                            UserProfileScreen(
                                userId = userId,
                                onNavigateToChat = { chId, chName ->
                                    navController.navigate("chatscreen/$chId/$chName/dm") {
                                        popUpTo("userprofile/$userId/$displayName") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("ServerChannels/{guildId}/{guildName}") { back ->
                            val guildId = back.arguments?.getString("guildId")   ?: return@composable
                            val guildName = back.arguments?.getString("guildName") ?: guildId
                            activeChannelId = null
                            ServerChannels(
                                guildId = guildId,
                                guildName = guildName,
                                onNavigateToChatScreen = { chId, chName ->
                                    navController.navigate("chatscreen/$chId/$chName/$guildId")
                                }
                            )
                        }

                        composable("DMs") {
                            activeChannelId = null
                            DmsScreen(onNavigateToChatScreen = { chId, chName ->
                                navController.navigate("chatscreen/$chId/$chName/dm")
                            })
                        }

                        composable("servers") {
                            activeChannelId = null
                            ServerScreen(onNavigateToChannels = { gId, gName ->
                                navController.navigate("ServerChannels/$gId/$gName")
                            })
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        discordApp.repository?.refreshOnResume(activeChannelId)
    }
}
