package com.example.beelocal.navigation

sealed class RootRoute(val route: String) {
    object Login : RootRoute("auth")
    object Permissions : RootRoute("permissions")
    object Main : RootRoute("main")
}