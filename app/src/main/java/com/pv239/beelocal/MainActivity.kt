package com.pv239.beelocal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pv239.beelocal.navigation.AppNavGraph
import com.pv239.beelocal.navigation.BingoRoute
import com.pv239.beelocal.navigation.DailyChallengeRoute
import com.pv239.beelocal.navigation.HomeRoute
import com.pv239.beelocal.navigation.ProfileRoute
import com.pv239.beelocal.navigation.RoutesRoute
import com.pv239.beelocal.navigation.SocialRoute
import com.pv239.beelocal.navigation.TopLevelRoute
import com.pv239.beelocal.ui.components.Header
import com.pv239.beelocal.ui.components.NavigationBar
import com.pv239.beelocal.ui.components.NavigationItem
import com.pv239.beelocal.ui.screens.dailychallenge.DailyChallengeScreen
import com.pv239.beelocal.ui.screens.HomeScreen
import com.pv239.beelocal.ui.theme.BeelocalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var navigationTarget by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createDailyChallengeNotificationChannel()

        navigationTarget = intent.getStringExtra("destination")
        enableEdgeToEdge()
        setContent {
            BeelocalTheme {
                AppNavGraph(initialNavigationTarget = navigationTarget)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navigationTarget = intent.getStringExtra("destination")
    }

    private fun createDailyChallengeNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "daily_challenge_channel"
            val name = "Daily Challenge Notifications"
            val descriptionText = "This channel is used for daily challenge notifications."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}

@PreviewScreenSizes
@Composable
fun BeelocalApp(initialTarget: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(initialTarget) {
        if (initialTarget == "dailyChallenge") {
            navController.navigate(DailyChallengeRoute) {
                // Ensure we don't build up a large stack
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val topLevelRoutes = listOf(
        TopLevelRoute("Home", HomeRoute, R.drawable.baseline_home_24, R.drawable.outline_home_24),
        TopLevelRoute("Routes", RoutesRoute, R.drawable.baseline_map_24, R.drawable.outline_map_24),
        TopLevelRoute(
            "Spot",
            DailyChallengeRoute,
            R.drawable.baseline_photo_camera_24,
            R.drawable.outline_photo_camera_24
        ),
        TopLevelRoute(
            "Bingo", BingoRoute, R.drawable.baseline_grid_on_24, R.drawable.outline_grid_on_24
        ),
        TopLevelRoute(
            "Social", SocialRoute, R.drawable.baseline_group_24, R.drawable.outline_group_24
        ),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(), topBar = {
            // TODO: replace hardcoded values
            Header(
                streakCount = 7,
                honeyCount = 67,
                onProfileClick = {
                    navController.navigate(ProfileRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        innerPadding = innerPadding, onDailyChallengeClick = {
                            navController.navigate(DailyChallengeRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        })
                }
                composable<RoutesRoute> { Greeting("Routes Screen") }
                composable<DailyChallengeRoute> {
                    DailyChallengeScreen(
                        innerPadding = innerPadding,
                        // TODO: pass ViewModel state (distanceMeters, isCompleted, …)
                    )
                }
                composable<BingoRoute> { Greeting("Bingo Screen") }
                composable<SocialRoute> { Greeting("Social Screen") }
                composable<ProfileRoute> { Greeting("Profile Screen") }
            }

            NavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 16.dp)
            ) {
                topLevelRoutes.forEach { topLevelRoute ->
                    val isSelected =
                        currentDestination?.hasRoute(topLevelRoute.route::class) == true

                    NavigationItem(
                        label = topLevelRoute.name,
                        icon = if (isSelected) topLevelRoute.iconSelected else topLevelRoute.iconUnselected,
                        isSelected = isSelected,
                        onClick = {
                            navController.navigate(topLevelRoute.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BeelocalTheme {
        Greeting("Android")
    }
}
