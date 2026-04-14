package com.pv239.beelocal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pv239.beelocal.BeelocalApp
import com.pv239.beelocal.permissions.LocationPermissionScreen
import com.pv239.beelocal.permissions.PermissionViewModel

@Composable
fun AppNavGraph(permissionViewModel: PermissionViewModel = hiltViewModel()) {

    val navController = rememberNavController()

    val hasLocationPermission = permissionViewModel.hasLocationPermission

    val startDestination = when {
        !hasLocationPermission -> RootRoute.Permissions.route
        else -> RootRoute.Main.route
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(RootRoute.Login.route) {  } // TODO: Add the Auth screen here
        composable(RootRoute.Permissions.route) {
            LocationPermissionScreen(permissionViewModel = permissionViewModel)
        }
        composable(RootRoute.Main.route) {
            BeelocalApp()
        }
    }

    LaunchedEffect(hasLocationPermission) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (hasLocationPermission && currentRoute != RootRoute.Main.route) {
            navController.navigate(RootRoute.Main.route) {
                popUpTo(RootRoute.Permissions.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}