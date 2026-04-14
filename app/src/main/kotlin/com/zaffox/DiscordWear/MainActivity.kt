package com.zaffox.discordwear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.material3.MaterialTheme
import com.zaffox.discordwear.screens.HomeScreen
import com.zaffox.discordwear.screens.WelcomeScreen
import com.zaffox.discordwear.screens.ChatScreen
import com.zaffox.discordwear.screens.ServerChannels
import com.zaffox.discordwear.screens.DmsScreen
import com.zaffox.discordwear.screens.ServerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberSwipeDismissableNavController()
                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(
                            .onNavigateToDms = { navController.navigate("DMs") },
                            onNavigateToServers = { navController.navigate("servers") },
                            onNavigateToWelcome = { navController.navigate("Welcome") }
                        )
                    }
                    composable("Welcome") {
                        WelcomeScreen(
                            onSetupComplete = {
                                navController.navigate("home") {
                                     popUpTo("Welcome") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("chatscreen") {
                        ChatScreen()
                    }
                    composable("ServerChannels") {
                        ServerChannels(
                            onNavigateToChatScreen = { navController.navigate("chatscreen") }
                        )
                    }
                     composable("DMs") {
                        DmsScreen(onNavigateToChatScreen = { navController.navigate("chatscreen") })
                    }
                    composable("servers") {
                        ServerScreen(
                            onNavigateToChannels = { navController.navigate("ServerChannels") }
                        )
                    }
                }
             }
        }
    }
}
