package com.pv239.beelocal.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * A pending follow request created when a user with a public profile is *not*
 * available — i.e. the target user is private and must explicitly accept.
 *
 * Author info (`fromUsername`, `fromUserProfileImageUrl`) is denormalized so the
 * recipient's "follow requests" list can be rendered without fan-out reads.
 *
 * Upon acceptance, the request is deleted and `toUserId` is added to the
 * follower's [User.friends] list (i.e. "I now follow them"), giving the
 * follower access to the target's shared feed entries.
 */
data class FollowRequest(
    @DocumentId
    val id: String = "",

    // Who wants to follow
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromUserProfileImageUrl: String? = null,

    // Who needs to approve
    val toUserId: String = "",

    val requestedAt: Timestamp = Timestamp.now(),
)
