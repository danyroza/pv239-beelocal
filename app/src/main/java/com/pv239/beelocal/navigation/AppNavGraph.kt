package com.pv239.beelocal.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.pv239.beelocal.BeelocalApp
import com.pv239.beelocal.permissions.LocationPermissionScreen
import com.pv239.beelocal.permissions.PermissionViewModel
import com.pv239.beelocal.ui.screens.auth.LoginScreen
import com.pv239.beelocal.ui.screens.auth.RegisterScreen

@Composable
fun AppNavGraph(permissionViewModel: PermissionViewModel = hiltViewModel()) {

    val navController = rememberNavController()

    val hasLocationPermission = permissionViewModel.hasLocationPermission

    // Mockup auth flow: always start at the auth graph. Once Firebase auth is
    // wired up, this start destination should be derived from the current
    // user's session state instead.
    NavHost(
        navController = navController,
        startDestination = AuthGraph
    ) {
        navigation<AuthGraph>(startDestination = LoginRoute) {
            composable<LoginRoute> {
                LoginScreen(
                    onLoginSuccess = { navController.navigateAfterAuth(hasLocationPermission) },
                    onNavigateToRegister = {
                        navController.navigate(RegisterRoute) { launchSingleTop = true }
                    }
                )
            }
            composable<RegisterRoute> {
                RegisterScreen(
                    onRegisterSuccess = { navController.navigateAfterAuth(hasLocationPermission) },
                    onNavigateToLogin = {
                        navController.navigate(LoginRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        composable<PermissionsRoute> {
            LocationPermissionScreen(permissionViewModel = permissionViewModel)
        }
        composable<MainGraph> {
            BeelocalApp()
        }
    }

    LifecycleResumeEffect(Unit) {
        permissionViewModel.checkLocationPermission()

        // If the user has just granted location permission while sitting on the
        // permissions screen, advance them into the main app graph.
        val currentRoute = navController.currentDestination
        val isOnPermissions =
            currentRoute?.hierarchy?.any { it.hasRoute<PermissionsRoute>() } == true
        if (hasLocationPermission && isOnPermissions) {
            navController.navigate(MainGraph) {
                launchSingleTop = true
                popUpTo<PermissionsRoute> { inclusive = true }
            }
        }

        onPauseOrDispose {}
    }
}

/**
 * Helper used by the auth screens to push the user into either the
 * permissions flow or the main app, depending on whether location access has
 * already been granted. The auth graph is cleared from the back stack so the
 * user cannot navigate back to login after authenticating.
 */
private fun NavHostController.navigateAfterAuth(hasLocationPermission: Boolean) {
    val target = if (hasLocationPermission) MainGraph else PermissionsRoute
    navigate(target) {
        popUpTo<AuthGraph> { inclusive = true }
        launchSingleTop = true
    }
}
