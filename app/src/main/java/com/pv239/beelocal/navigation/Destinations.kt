package com.pv239.beelocal.navigation

import kotlinx.serialization.Serializable

@Serializable object AuthGraph
@Serializable object PermissionsRoute
@Serializable object MainGraph

@Serializable object HomeRoute
@Serializable object RoutesRoute
@Serializable object BingoRoute
@Serializable object SocialRoute
@Serializable object ProfileRoute

data class TopLevelRoute<T : Any>(
    val name: String,
    val route: T,
    val iconSelected: Int,
    val iconUnselected: Int
)