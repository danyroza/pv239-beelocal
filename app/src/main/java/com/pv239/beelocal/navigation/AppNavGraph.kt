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
                    onLoginSuccess = { navController.navigateAfterAuth(allPermissionsGranted) },
                    onNavigateToRegister = {
                        navController.navigate(RegisterRoute) { launchSingleTop = true }
                    }
                )
            }
            composable<RegisterRoute> {
                RegisterScreen(
                    onRegisterSuccess = {
                        // After registration we send the user to the profile
                        // picture onboarding step. The auth stack is cleared so
                        // back-navigation from onboarding exits the app rather
                        // than returning to the registration form.
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
                    // AuthGraph was already popped when we entered onboarding,
                    // so here we only need to pop the onboarding step itself.
                    val target =
                        if (allPermissionsGranted) MainGraph else PermissionsRoute
                    navController.navigate(target) {
                        popUpTo<OnboardingProfilePictureRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<PermissionsRoute> {
            PermissionsScreen(permissionViewModel = permissionViewModel)
        }
        composable<MainGraph> {
            BeelocalApp()
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
 * Helper used by the auth screens to push the user into either the
 * permissions flow or the main app, depending on whether location access has
 * already been granted. The auth graph is cleared from the back stack so the
 * user cannot navigate back to login after authenticating.
 */
private fun NavHostController.navigateAfterAuth(permissionsGranted: Boolean) {
    val target = if (permissionsGranted) MainGraph else PermissionsRoute
    navigate(target) {
        popUpTo<AuthGraph> { inclusive = true }
        launchSingleTop = true
    }
}
