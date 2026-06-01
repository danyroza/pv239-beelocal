package com.pv239.beelocal.navigation

import kotlinx.serialization.Serializable

@Serializable object AuthGraph
@Serializable object LoginRoute
@Serializable object RegisterRoute
@Serializable object OnboardingProfilePictureRoute

@Serializable object PermissionsRoute
@Serializable object MainGraph

@Serializable object HomeRoute
@Serializable object RoutesRoute
@Serializable object DailyChallengeRoute
@Serializable object BingoRoute
@Serializable object SocialRoute
@Serializable object ProfileRoute

@Serializable data class RouteDetailRoute(val routeId: String)
@Serializable object ActiveJourneyRoute
@Serializable object RouteCompletionRoute

data class TopLevelRoute<T : Any>(
    val name: String,
    val route: T,
    val iconSelected: Int,
    val iconUnselected: Int
)
