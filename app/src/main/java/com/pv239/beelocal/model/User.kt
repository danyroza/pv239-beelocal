package com.pv239.beelocal.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val username: String = "",
    val usernameNormalized: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val profileImageId: String? = null,
    /**
     * IDs of users this account follows. The home/social feed is derived from
     * the [com.pv239.beelocal.model.FeedEntry] documents authored by any user in
     * this list (see [com.pv239.beelocal.domain.FirestoreRepository.getFriendsFeed]).
     *
     * Inserting an ID here means **I follow them** — and is only done after the
     * followee approves (private profiles) or implicitly (public profiles).
     */
    val friends: List<String> = emptyList(),
    /**
     * If `true`, anyone can follow this user without approval and immediately
     * sees their shared feed entries. If `false`, every incoming follow goes
     * through a [FollowRequest] that the owner must accept.
     *
     * New accounts default to public to match the most common social-app expectation.
     */
    val isProfilePublic: Boolean = true,
    val notificationSettings: NotificationSettings = NotificationSettings()
)
