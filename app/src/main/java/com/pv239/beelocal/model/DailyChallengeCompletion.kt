package com.pv239.beelocal.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

/**
 * Represents a user's completion of a [DailyChallenge].
 *
 * Created right after the user submits the photo of the daily challenge.
 * Stores all the data necessary to render the entry in the feed without having
 * to perform additional lookups (denormalized author info).
 */
data class DailyChallengeCompletion(
    @DocumentId
    val id: String = "",

    // Author info (denormalized for feed display)
    val userId: String = "",
    val username: String = "",
    val userProfileImageUrl: String? = null,

    // Reference to the challenge that was completed
    val challengeId: String = "",

    // Submission content
    val photoUrl: String = "",
    val caption: String = "",
    val location: GeoPoint? = null,
    val is360View: Boolean = false,

    // When the completion was submitted
    val completedAt: Timestamp = Timestamp.now(),
    val sharedToFeed: Boolean = false,
)
