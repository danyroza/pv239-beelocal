package com.pv239.beelocal.ui.screens.routes

import com.google.firebase.firestore.DocumentSnapshot
import com.pv239.beelocal.model.Route
import com.pv239.beelocal.model.RouteCompletion
import com.pv239.beelocal.model.RoutePoint
import com.pv239.beelocal.model.RouteReview

// ---------------------------------------------------------------------------
// Routes list screen
// ---------------------------------------------------------------------------

/**
 * The list screen is divided into two sections:
 *
 * - [activeRoutes]  – routes the user has already started but not finished.
 *                    Shown at the top as "Continue exploring".
 * - [exploreRoutes] – the full paginated catalogue for the selected [city].
 *                    Already-completed routes are still listed here (they show
 *                    a "Completed" badge) but are *not* shown in [activeRoutes].
 */
data class RoutesListUiState(
    /** Routes the current user has started but not yet completed. */
    val activeRoutes: List<Route> = emptyList(),
    /** Full paginated route catalogue for [city]. */
    val exploreRoutes: List<Route> = emptyList(),
    val completedRoutes: List<Route> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val cursor: DocumentSnapshot? = null,
    val city: String = "Brno",
    val error: String? = null,
    /** Route IDs the current user has fully completed. */
    val completedRouteIds: Set<String> = emptySet(),
    /** Route IDs the current user has started but not yet finished. */
    val inProgressRouteIds: Set<String> = emptySet(),
) {
    /** All routes combined – used when we need a flat list (e.g. paging). */
    val routes: List<Route> get() = exploreRoutes
}

// ---------------------------------------------------------------------------
// Route detail screen
// ---------------------------------------------------------------------------

data class RouteDetailUiState(
    val route: Route? = null,
    val isLoading: Boolean = false,
    val isStarting: Boolean = false,
    val error: String? = null,
    /** String indices ("0", "1", …) of checkpoints the user has already answered. */
    val completedPointIds: List<String> = emptyList(),
    /** True once the route has been fully completed by this user. */
    val isAlreadyCompleted: Boolean = false,
    val routeReviews: List<RouteReview> = emptyList(),
)

// ---------------------------------------------------------------------------
// Active journey screen (point-by-point)
// ---------------------------------------------------------------------------

/**
 * State for the currently active checkpoint page.
 *
 * [currentPointIndex] is the index into [Route.points] the user is on.
 * [isCheckingAnswer] is true while the answer verification is in progress.
 * [answerResult] is null if not yet checked, true/false after the check.
 * [savedAnswers] maps checkpoint index → last correct answer the user submitted,
 *   allowing the answer field to be pre-filled when navigating back.
 */
data class ActiveJourneyUiState(
    val route: Route? = null,
    val currentPointIndex: Int = 0,
    val completedPointIndices: Set<Int> = emptySet(),
    /** Maps checkpoint index → the answer the user typed for that checkpoint. */
    val savedAnswers: Map<Int, String> = emptyMap(),
    val isCheckingAnswer: Boolean = false,
    val answerResult: Boolean? = null,
    val answerInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Live user location for the map overlay (latitude, longitude). */
    val userLatLng: Pair<Double, Double>? = null,
    /**
     * Incremented each time the detail screen should navigate to the active
     * journey screen. Using a counter (rather than a Boolean) means tapping
     * Start twice in quick succession still triggers exactly one navigation.
     */
    val navigationTrigger: Int = 0,
) {
    val currentPoint: RoutePoint?
        get() = route?.points?.getOrNull(currentPointIndex)

    val totalPoints: Int
        get() = route?.points?.size ?: 0

    val isLastPoint: Boolean
        get() = currentPointIndex == totalPoints - 1

    val progressFraction: Float
        get() = if (totalPoints == 0) 0f
        else completedPointIndices.size.toFloat() / totalPoints.toFloat()
}

// ---------------------------------------------------------------------------
// Route completion screen
// ---------------------------------------------------------------------------

/**
 * State for the post-completion screen.
 *
 * [completion] is populated once [RouteViewModel.completeRoute] succeeds.
 * [rating] is the star value the user has selected (1–5).
 * [reviewText] is the comment body.
 * [selectedPhotoUris] holds local URIs the user picked from the gallery.
 * [isSubmitting] is true while the review + feed share is being written.
 * [isDone] becomes true once the user has submitted or skipped — the screen
 * should not be re-entered after this flag is set.
 */
data class RouteCompletionUiState(
    val completion: RouteCompletion? = null,
    val xpAwarded: Int = 0,
    val rating: Int = 0,
    val reviewText: String = "",
    val selectedPhotoUris: List<String> = emptyList(),
    val isSubmitting: Boolean = false,
    val isDone: Boolean = false,
    val error: String? = null,
)