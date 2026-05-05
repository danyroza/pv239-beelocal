package com.pv239.beelocal.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Represents a user's completion of a [Route] challenge.
 *
 * Created when the user finishes all points of a route (and optionally
 * uploads a finishing photo). Contains all the info needed to display
 * the achievement on the feed without resolving extra references.
 */
data class RouteCompletion(
    @DocumentId
    val id: String = "",

    // Author info (denormalized for feed display)
    val userId: String = "",
    val username: String = "",
    val userProfileImageUrl: String? = null,

    // Reference + denormalized info of the completed route
    val routeId: String = "",
    val routeName: String = "",
    val city: String = "",
    val totalPoints: Int = 0,

    // Submission content
    val photoUrl: String? = null,
    val caption: String = "",

    // Timing info
    val startedAt: Timestamp? = null,
    val completedAt: Timestamp = Timestamp.now()
)
