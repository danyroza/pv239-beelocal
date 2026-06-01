package com.pv239.beelocal.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val username: String = "",
    val usernameNormalized: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
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
     *
     * NOTE: do **not** rename this back to an `is`-prefixed property. Kotlin
     * compiles `is`-prefixed boolean properties to a getter named verbatim
     * (e.g. `isProfilePublic()`), and Firestore's reflection serializer then
     * strips the `is` prefix when persisting — so `val isProfilePublic` would
     * be stored as `profilePublic` while any manual `.update("isProfilePublic", ...)`
     * would write to a *different* field, leaving the read-back value stale.
     */
    val profilePublic: Boolean = true,
    val notificationSettings: NotificationSettings = NotificationSettings()
)
