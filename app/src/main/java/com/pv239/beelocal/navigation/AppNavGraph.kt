package com.pv239.beelocal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.pv239.beelocal.permissions.PermissionsScreen
import com.pv239.beelocal.permissions.PermissionViewModel
import com.pv239.beelocal.ui.BeeLocalAppViewModel
import com.pv239.beelocal.ui.BeelocalApp
import com.pv239.beelocal.ui.screens.auth.LoginScreen
import com.pv239.beelocal.ui.screens.auth.RegisterScreen
import com.pv239.beelocal.ui.screens.onboarding.OnboardingProfilePictureScreen

@Composable
fun AppNavGraph(
    permissionViewModel: PermissionViewModel = hiltViewModel(),
    appViewModel: BeeLocalAppViewModel = hiltViewModel()
) {

    val navController = rememberNavController()

    val hasLocationPermission = permissionViewModel.hasLocationPermission
    val hasCameraPermission = permissionViewModel.hasCameraPermission
    val hasNotificationPermission = permissionViewModel.hasNotificationPermission

    val allPermissionsGranted = hasLocationPermission && hasCameraPermission && hasNotificationPermission

    val startDestination = when {
        !appViewModel.isLoggedIn -> AuthGraph
        !allPermissionsGranted -> PermissionsRoute
        else -> MainGraph
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        navigation<AuthGraph>(startDestination = LoginRoute) {
            composable<LoginRoute> {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigateAfterAuth<AuthGraph>(allPermissionsGranted)
                    },
                    onNavigateToRegister = {
                        navController.navigate(RegisterRoute) { launchSingleTop = true }
                    }
                )
            }
            composable<RegisterRoute> {
                RegisterScreen(
                    // First-time users go through onboarding (pick a profile
                    // picture) before continuing on to permissions / main.
                    onRegisterSuccess = {
                        navController.navigate(OnboardingProfilePictureRoute) {
                            popUpTo<AuthGraph> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(LoginRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        composable<OnboardingProfilePictureRoute> {
            OnboardingProfilePictureScreen(
                onFinished = {
                    navController.navigateAfterAuth<OnboardingProfilePictureRoute>(allPermissionsGranted)
                }
            )
        }
        composable<PermissionsRoute> {
            PermissionsScreen(permissionViewModel = permissionViewModel)
        }
        composable<MainGraph> {
            BeelocalApp(
                onLogout = {
                    navController.navigate(AuthGraph) {
                        popUpTo<MainGraph> { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }

    LaunchedEffect(allPermissionsGranted) {
        val currentRoute = navController.currentDestination
        val isOnPermissions =
            currentRoute?.hierarchy?.any { it.hasRoute<PermissionsRoute>() } == true

        if (allPermissionsGranted && isOnPermissions) {
            navController.navigate(MainGraph) {
                launchSingleTop = true
                popUpTo<PermissionsRoute> { inclusive = true }
            }
        }
    }
}

/**
 * Helper used by the auth & onboarding screens to push the user into either
 * the permissions flow or the main app, depending on whether all required
 * permissions have already been granted. The caller specifies via the reified
 * type parameter [T] which destination should be popped (inclusively) from the
 * back stack so the user cannot navigate back into those one-off flows.
 */
private inline fun <reified T : Any> NavHostController.navigateAfterAuth(
    permissionsGranted: Boolean
) {
    val target = if (permissionsGranted) MainGraph else PermissionsRoute
    navigate(target) {
        popUpTo<T> { inclusive = true }
        launchSingleTop = true
    }
}
