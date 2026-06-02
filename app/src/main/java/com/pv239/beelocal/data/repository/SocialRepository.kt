package com.pv239.beelocal.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.pv239.beelocal.domain.Page
import com.pv239.beelocal.model.FeedEntry
import com.pv239.beelocal.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Focused repository for the Social screen.
 *
 * Composes the smaller, per-domain repositories ([UserRepository],
 * [FollowRepository], [FeedRepository]) into the few use-cases the social UI
 * actually needs, and consistently scopes them to the signed-in user.
 */
@Singleton
class SocialRepository @Inject constructor(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val feedRepository: FeedRepository,
    private val auth: FirebaseAuth,
) {
    /**
     * UID of the signed-in user. The Social screen is only reachable from the
     * authenticated `MainGraph`, so a missing uid would be a programmer error —
     * we fail fast instead of silently falling back to a stub account.
     */
    private val currentUserId: String
        get() = checkNotNull(auth.currentUser?.uid) {
            "SocialRepository accessed without an authenticated user"
        }

    // -------------------------------------------------------------------------
    // Current user
    // -------------------------------------------------------------------------

    suspend fun getCurrentUser(): User? = userRepository.getUser(currentUserId)

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    /**
     * Searches users by username prefix, excluding the current user from results.
     * Returns the first page; pass [lastVisible] for subsequent pages.
     */
    suspend fun searchUsers(
        query: String,
        lastVisible: DocumentSnapshot? = null,
    ): Page<User> {
        if (query.isBlank()) return Page(emptyList(), null, hasMore = false)
        val uid = currentUserId
        val page = userRepository.searchUsers(query.trim(), lastVisible)
        // Filter out self
        return page.copy(items = page.items.filter { it.id != uid })
    }

    // -------------------------------------------------------------------------
    // Friends
    // -------------------------------------------------------------------------

    /** Loads full [User] objects for every friend id in the current user's list. */
    suspend fun getFriends(): List<User> {
        val user = getCurrentUser() ?: return emptyList()
        return user.friends.mapNotNull { friendId ->
            userRepository.getUser(friendId)
        }
    }

    /**
     * Request to follow [targetUserId]. Honors the target's profile visibility:
     * - **Public** profile → instantly added to the current user's friends list
     *   (returns `true`).
     * - **Private** profile → a [com.pv239.beelocal.model.FollowRequest] is
     *   created and must be accepted by the target before the follow takes
     *   effect (returns `false`).
     *
     * Throws if the current user document cannot be loaded.
     */
    suspend fun requestFollow(targetUserId: String): Boolean {
        val me = getCurrentUser()
            ?: throw IllegalStateException("Current user document not found")
        return followRepository.requestFollow(fromUser = me, toUserId = targetUserId)
    }

    suspend fun removeFriend(friendId: String) {
        userRepository.removeFriend(currentUserId = currentUserId, friendId = friendId)
    }

    // -------------------------------------------------------------------------
    // Feed
    // -------------------------------------------------------------------------

    /**
     * Loads the friends-only feed.
     * Returns an empty page when the current user has no friends yet.
     */
    suspend fun getFriendsFeed(): Page<FeedEntry> {
        val user = getCurrentUser()
            ?: return Page(emptyList(), null, hasMore = false)
        return feedRepository.getFriendsFeed(user.friends)
    }

    // -------------------------------------------------------------------------
    // Public user profile
    // -------------------------------------------------------------------------

    /** Convenience id of the signed-in user, exposed for UI redirects. */
    val signedInUserId: String get() = currentUserId

    /** Load the [User] document for the given id, regardless of friend status. */
    suspend fun getUser(userId: String): User? = userRepository.getUser(userId)

    /**
     * Whether the signed-in user already follows [userId] (i.e. [userId] is
     * present in the current user's friends list).
     */
    suspend fun isFollowing(userId: String): Boolean {
        if (userId == currentUserId) return false
        val me = getCurrentUser() ?: return false
        return userId in me.friends
    }

    /**
     * Whether the signed-in user has an outstanding follow request awaiting
     * approval from [userId].
     */
    suspend fun hasPendingRequestTo(userId: String): Boolean =
        followRepository.hasPendingRequest(fromUserId = currentUserId, toUserId = userId)

    /** Cancel an outstanding follow request previously sent to [userId]. */
    suspend fun cancelFollowRequestTo(userId: String) {
        followRepository.cancelFollowRequest(fromUserId = currentUserId, toUserId = userId)
    }

    /**
     * Returns the most recent page of [userId]'s shared activity. Callers are
     * expected to gate visibility client-side based on the target user's
     * [User.profilePublic] flag + follow status — this method itself does not
     * enforce privacy.
     */
    suspend fun getUserFeed(userId: String): Page<FeedEntry> =
        feedRepository.getUserFeed(userId)
}
