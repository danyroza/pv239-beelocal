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
    val currentUserId: String?
        get() = auth.currentUser?.uid ?: "test-user-001" // TODO: remove hardcoded test user

    // -------------------------------------------------------------------------
    // Current user
    // -------------------------------------------------------------------------

    suspend fun getCurrentUser(): User? {
        val uid = currentUserId ?: return null
        return firestoreRepository.getUser(uid)
    }

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

    suspend fun addFriend(friendId: String) {
        val uid = currentUserId ?: return
        firestoreRepository.addFriend(currentUserId = uid, friendId = friendId)
    }

    suspend fun removeFriend(friendId: String) {
        val uid = currentUserId ?: return
        firestoreRepository.removeFriend(currentUserId = uid, friendId = friendId)
    }

    /** Returns true if [userId] is in the current user's friends list. */
    suspend fun isFriend(userId: String): Boolean {
        val user = getCurrentUser() ?: return false
        return user.friends.contains(userId)
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