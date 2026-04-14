package com.pv239.beelocal.navigation

sealed class RootRoute(val route: String) {
    object Login : RootRoute("auth")
    object Permissions : RootRoute("permissions")
    object Main : RootRoute("main")
}