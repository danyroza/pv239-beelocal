package com.pv239.beelocal.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pv239.beelocal.BeelocalApp
import com.pv239.beelocal.permissions.LocationPermissionScreen
import com.pv239.beelocal.permissions.PermissionViewModel

@SuppressLint("RestrictedApi")
@Composable
fun AppNavGraph(permissionViewModel: PermissionViewModel = hiltViewModel()) {

    val navController = rememberNavController()

    val hasLocationPermission = permissionViewModel.hasLocationPermission

    val startDestination = if (!hasLocationPermission) PermissionsRoute else MainGraph

    NavHost(navController = navController, startDestination = startDestination) {
        composable<AuthGraph> {  } // TODO: Add the Auth screen here
        composable<PermissionsRoute> {
            LocationPermissionScreen(permissionViewModel = permissionViewModel)
        }
        composable<MainGraph> {
            BeelocalApp()
        }
    }

    LaunchedEffect(hasLocationPermission) {
        val currentRoute = navController.currentBackStackEntry?.destination
        if (hasLocationPermission && currentRoute?.hierarchy?.any { it.hasRoute<MainGraph>() } != true) {
            navController.navigate(MainGraph) {
                popUpTo<PermissionsRoute> { inclusive = true }
            }
        }
    }
}