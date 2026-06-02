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
/**
 * Social hub destination. The optional [startTabOrdinal] lets callers
 * deep-link into a specific tab of [com.pv239.beelocal.ui.screens.social.SocialScreen]
 * (e.g. the profile "View all friends" / "Invite" affordances open the Friends
 * and Search tabs respectively). It's an `Int?` so that the route stays purely
 * inside the navigation module without taking a dependency on the social UI
 * package's [com.pv239.beelocal.ui.screens.social.SocialTab] enum.
 */
@Serializable data class SocialRoute(val startTabOrdinal: Int? = null)
@Serializable object ProfileRoute

/**
 * Public profile view for another user (or the signed-in user, in which case
 * the caller is expected to redirect to [ProfileRoute] for the editable
 * self-profile instead).
 */
@Serializable data class UserProfileRoute(val userId: String)

@Serializable data class RouteDetailRoute(val routeId: String)
@Serializable object ActiveJourneyRoute
@Serializable object RouteCompletionRoute

data class TopLevelRoute<T : Any>(
    val name: String,
    val route: T,
    val iconSelected: Int,
    val iconUnselected: Int
)
