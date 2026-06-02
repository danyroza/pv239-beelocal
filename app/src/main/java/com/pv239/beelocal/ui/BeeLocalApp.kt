package com.pv239.beelocal.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pv239.beelocal.R
import com.pv239.beelocal.navigation.ActiveJourneyRoute
import com.pv239.beelocal.navigation.BingoRoute
import com.pv239.beelocal.navigation.DailyChallengeRoute
import com.pv239.beelocal.navigation.HomeRoute
import com.pv239.beelocal.navigation.ProfileRoute
import com.pv239.beelocal.navigation.RouteCompletionRoute
import com.pv239.beelocal.navigation.RouteDetailRoute
import com.pv239.beelocal.navigation.RoutesRoute
import com.pv239.beelocal.navigation.SocialRoute
import com.pv239.beelocal.navigation.TopLevelRoute
import com.pv239.beelocal.navigation.UserProfileRoute
import com.pv239.beelocal.ui.components.Header
import com.pv239.beelocal.ui.components.NavigationBar
import com.pv239.beelocal.ui.components.NavigationItem
import com.pv239.beelocal.ui.screens.bingo.BingoScreen
import com.pv239.beelocal.ui.screens.dailychallenge.DailyChallengeScreen
import com.pv239.beelocal.ui.screens.home.HomeScreen
import com.pv239.beelocal.ui.screens.profile.ProfileScreen
import com.pv239.beelocal.ui.screens.routes.ActiveJourneyScreen
import com.pv239.beelocal.ui.screens.routes.RouteCompletionScreen
import com.pv239.beelocal.ui.screens.routes.RouteDetailScreen
import com.pv239.beelocal.ui.screens.routes.RouteViewModel
import com.pv239.beelocal.ui.screens.routes.RoutesScreen
import com.pv239.beelocal.ui.screens.social.SocialScreen
import com.pv239.beelocal.ui.screens.social.SocialTab
import com.pv239.beelocal.ui.screens.userprofile.UserProfileScreen
import com.pv239.beelocal.ui.theme.BeelocalTheme

@Composable
fun BeelocalApp(
    onLogout: () -> Unit = {},
    viewModel: BeeLocalAppViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val profileImageUrl by viewModel.profileImageUrl.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()

    val routeViewModel: RouteViewModel = hiltViewModel()

    // Routing helper: tapping a user's avatar/username from any screen leads
    // to the public profile screen, unless the tapped user *is* the signed-in
    // user — in which case we route to the editable self-profile instead so
    // the user always lands on the right view of their own data.
    val openUserProfile: (String) -> Unit = openUserProfile@{ targetUserId ->
        if (targetUserId.isBlank()) return@openUserProfile
        if (targetUserId == viewModel.currentUserId) {
            navController.navigate(ProfileRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            navController.navigate(UserProfileRoute(targetUserId)) {
                launchSingleTop = true
            }
        }
    }

    val topLevelRoutes = listOf(
        TopLevelRoute(
            stringResource(R.string.nav_home),
            HomeRoute,
            R.drawable.baseline_home_24,
            R.drawable.outline_home_24
        ),
        TopLevelRoute(
            stringResource(R.string.nav_routes),
            RoutesRoute,
            R.drawable.baseline_map_24,
            R.drawable.outline_map_24
        ),
        TopLevelRoute(
            stringResource(R.string.nav_spot),
            DailyChallengeRoute,
            R.drawable.baseline_photo_camera_24,
            R.drawable.outline_photo_camera_24
        ),
        TopLevelRoute(
            stringResource(R.string.nav_bingo),
            BingoRoute,
            R.drawable.baseline_grid_on_24,
            R.drawable.outline_grid_on_24
        ),
        TopLevelRoute(
            stringResource(R.string.nav_social),
            SocialRoute(),
            R.drawable.baseline_group_24,
            R.drawable.outline_group_24
        ),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Header(
                streakCount = statistics.streak,
                honeyCount = statistics.xp,
                profileImageUrl = profileImageUrl,
                username = username,
                onProfileClick = {
                    navController.navigate(ProfileRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 16.dp)
            ) {
                topLevelRoutes.forEach { topLevelRoute ->
                    val isSelected =
                        currentDestination?.hasRoute(topLevelRoute.route::class) == true
                    NavigationItem(
                        label = topLevelRoute.name,
                        icon = if (isSelected) {
                            topLevelRoute.iconSelected
                        } else {
                            topLevelRoute.iconUnselected
                        },
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
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    innerPadding = innerPadding,
                    onDailyChallengeClick = {
                        navController.navigate(DailyChallengeRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRouteClick = { routeId ->
                        navController.navigate(RouteDetailRoute(routeId))
                    },
                    routeViewModel = routeViewModel,
                )
            }

            composable<RoutesRoute> {
                RoutesScreen(
                    innerPadding = innerPadding,
                    onRouteClick = { routeId ->
                        navController.navigate(RouteDetailRoute(routeId))
                    },
                    viewModel = routeViewModel,
                )
            }
            composable<RouteDetailRoute> { backStackEntry ->
                val routeId = backStackEntry.arguments?.getString("routeId") ?: return@composable
                RouteDetailScreen(
                    innerPadding = innerPadding,
                    routeId = routeId,
                    onJourneyStart = {
                        navController.navigate(ActiveJourneyRoute) {
                            launchSingleTop = true
                        }
                    },
                    viewModel = routeViewModel,
                )
            }
            composable<ActiveJourneyRoute> {
                ActiveJourneyScreen(
                    innerPadding = innerPadding,
                    onBack = { navController.popBackStack() },
                    onRouteCompleted = {
                        navController.navigate(RouteCompletionRoute) {
                            launchSingleTop = true
                        }
                    },
                    viewModel = routeViewModel,
                )
            }
            composable<RouteCompletionRoute> {
                RouteCompletionScreen(
                    innerPadding = innerPadding,
                    onDone = {
                        navController.navigate(RoutesRoute) {
                            popUpTo(RoutesRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    viewModel = routeViewModel,
                )
            }

            composable<DailyChallengeRoute> {
                DailyChallengeScreen(innerPadding = innerPadding)
            }
            composable<BingoRoute> { BingoScreen(innerPadding = innerPadding) }
            composable<SocialRoute> { backStackEntry ->
                val route: SocialRoute = backStackEntry.toRoute()
                val startTab = route.startTabOrdinal
                    ?.let { ordinal -> SocialTab.entries.getOrNull(ordinal) }
                SocialScreen(
                    innerPadding = innerPadding,
                    startTab = startTab,
                    onOpenUserProfile = openUserProfile,
                )
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    innerPadding = innerPadding,
                    onLogout = onLogout,
                    onViewAllFriends = {
                        navController.navigate(
                            SocialRoute(startTabOrdinal = SocialTab.FRIENDS.ordinal)
                        ) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onInviteFriend = {
                        navController.navigate(
                            SocialRoute(startTabOrdinal = SocialTab.SEARCH.ordinal)
                        ) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenUserProfile = openUserProfile,
                )
            }
            composable<UserProfileRoute> { backStackEntry ->
                val route: UserProfileRoute = backStackEntry.toRoute()
                UserProfileScreen(
                    innerPadding = innerPadding,
                    userId = route.userId,
                    onBack = { navController.popBackStack() },
                )
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
