package com.pv239.beelocal.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.pv239.beelocal.domain.FirestoreRepository
import com.pv239.beelocal.domain.Page
import com.pv239.beelocal.model.FeedEntry
import com.pv239.beelocal.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Focused repository for the Social screen.
 * Delegates Firestore calls to [FirestoreRepository] and exposes
 * higher-level use-case methods consumed by [SocialViewModel].
 */
@Singleton
class SocialRepository @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
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

    suspend fun getCurrentUser(): User? = firestoreRepository.getUser(currentUserId)

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
        val page = firestoreRepository.searchUsers(query.trim(), lastVisible)
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
            firestoreRepository.getUser(friendId)
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
        return firestoreRepository.requestFollow(fromUser = me, toUserId = targetUserId)
    }

    suspend fun removeFriend(friendId: String) {
        firestoreRepository.removeFriend(currentUserId = currentUserId, friendId = friendId)
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
        return firestoreRepository.getFriendsFeed(user.friends)
    }
}
