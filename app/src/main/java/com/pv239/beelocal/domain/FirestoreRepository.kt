package com.pv239.beelocal.domain

import com.pv239.beelocal.domain.FirestoreConfig.FEED_PAGE_SIZE
import com.pv239.beelocal.domain.FirestoreConfig.ROUTES_BY_CITY_PAGE_SIZE
import com.pv239.beelocal.domain.FirestoreConfig.USERS_SEARCH_PAGE_SIZE
import com.pv239.beelocal.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // -------------------------------------------------------------------------
    // User
    // -------------------------------------------------------------------------

    suspend fun getUser(userId: String): User? {
        val snapshot = firestore.collection(FirestoreCollections.USERS.value)
            .document(userId)
            .get()
            .await()
        if (!snapshot.exists()) return null
        return snapshot.toObject<User>()
    }

    suspend fun saveUser(user: User) {
        val userToSave = user.copy(
            usernameNormalized = user.username.lowercase().trim()
        )
        firestore.collection(FirestoreCollections.USERS.value)
            .document(user.id)
            .set(userToSave)
            .await()
    }

    /**
     * Returns the first page of users whose username starts with [query].
     *
     * Usage:
     * ```
     * val first = repo.searchUsers("an")
     * val second = repo.searchUsers("an", lastVisible = first.cursor)
     * ```
     */
    suspend fun searchUsers(
        query: String,
        lastVisible: DocumentSnapshot? = null
    ): Page<User> {
        val normalizedQuery = query.lowercase().trim()

        val baseQuery = firestore.collection(FirestoreCollections.USERS.value)
            .whereGreaterThanOrEqualTo("usernameNormalized", normalizedQuery)
            .whereLessThanOrEqualTo("usernameNormalized", normalizedQuery + "\uf8ff")
            .orderBy("usernameNormalized")
            .limit(USERS_SEARCH_PAGE_SIZE)

        val firestoreQuery = if (lastVisible != null) baseQuery.startAfter(lastVisible) else baseQuery

        val snapshot = firestoreQuery.get().await()
        return Page(
            items = snapshot.toObjects(User::class.java),
            cursor = snapshot.documents.lastOrNull()
        )
    }

    suspend fun addFriend(currentUserId: String, friendId: String) {
        firestore.collection(FirestoreCollections.USERS.value).document(currentUserId)
            .update("friends", FieldValue.arrayUnion(friendId))
            .await()
    }

    suspend fun removeFriend(currentUserId: String, friendId: String) {
        firestore.collection(FirestoreCollections.USERS.value).document(currentUserId)
            .update("friends", FieldValue.arrayRemove(friendId))
            .await()
    }

    suspend fun updateStreak(userId: String, newStreak: Int) {
        firestore.collection(FirestoreCollections.USERS.value).document(userId)
            .update("streak", newStreak)
            .await()
    }

    // -------------------------------------------------------------------------
    // Daily Challenge
    // -------------------------------------------------------------------------

    suspend fun getDailyChallenge(date: Timestamp): DailyChallenge? {
        return firestore.collection(FirestoreCollections.DAILY_CHALLENGES.value)
            .whereEqualTo("date", date)
            .limit(1)
            .get()
            .await()
            .toObjects(DailyChallenge::class.java)
            .firstOrNull()
    }

    // -------------------------------------------------------------------------
    // Feed
    // -------------------------------------------------------------------------

    /**
     * Returns a page of feed entries from the given friends, newest first.
     *
     * Because Firestore's [whereIn] is capped at 10 IDs and does not support
     * a shared cursor across multiple sub-queries, each chunk is independently
     * limited to [FEED_PAGE_SIZE], then all chunks are merged, re-sorted and
     * trimmed to [FEED_PAGE_SIZE] in memory.
     *
     * The returned [Page.cursor] is a snapshot of the oldest entry on this page
     * and can be used as [lastVisible] on the next call.
     *
     * Usage:
     * ```
     * val first = repo.getFriendsFeed(friendIds)
     * val second = repo.getFriendsFeed(friendIds, lastVisible = first.cursor)
     * ```
     */
    suspend fun getFriendsFeed(
        friendIds: List<String>,
        lastVisible: DocumentSnapshot? = null
    ): Page<FeedEntry> {
        if (friendIds.isEmpty()) return Page(emptyList(), null)

        val allDocuments = friendIds.chunked(10).flatMap { chunk ->
            val baseQuery = firestore.collection(FirestoreCollections.FEED.value)
                .whereIn("userId", chunk)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(FEED_PAGE_SIZE)

            val firestoreQuery = if (lastVisible != null) baseQuery.startAfter(lastVisible) else baseQuery

            firestoreQuery.get().await().documents
        }

        val pageDocuments = allDocuments
            .sortedByDescending { it.getTimestamp("timestamp") }
            .take(FEED_PAGE_SIZE.toInt())

        return Page(
            items = pageDocuments.mapNotNull { it.toObject(FeedEntry::class.java) },
            cursor = pageDocuments.lastOrNull()
        )
    }

    suspend fun addFeedEntry(entry: FeedEntry) {
        firestore.collection(FirestoreCollections.FEED.value).add(entry).await()
    }

    // -------------------------------------------------------------------------
    // Routes
    // -------------------------------------------------------------------------

    /**
     * Returns a page of routes for [city], ordered by rating descending.
     *
     * Usage:
     * ```
     * val first = repo.getRoutesByCity("Brno")
     * val second = repo.getRoutesByCity("Brno", lastVisible = first.cursor)
     * ```
     */
    suspend fun getRoutesByCity(
        city: String,
        lastVisible: DocumentSnapshot? = null
    ): Page<Route> {
        val baseQuery = firestore.collection(FirestoreCollections.ROUTES.value)
            .whereEqualTo("city", city)
            .orderBy("averageRating", Query.Direction.DESCENDING)
            .limit(ROUTES_BY_CITY_PAGE_SIZE)

        val firestoreQuery = if (lastVisible != null) baseQuery.startAfter(lastVisible) else baseQuery

        val snapshot = firestoreQuery.get().await()
        return Page(
            items = snapshot.toObjects(Route::class.java),
            cursor = snapshot.documents.lastOrNull()
        )
    }

    suspend fun addRouteReview(routeId: String, review: RouteReview) {
        val routeRef = firestore.collection(FirestoreCollections.ROUTES.value).document(routeId)
        firestore.runTransaction { transaction ->
            val route = transaction.get(routeRef).toObject<Route>()
                ?: throw IllegalStateException("Route $routeId does not exist, cannot add review")

            val newCount = route.reviewCount + 1
            val newRating = (route.averageRating * route.reviewCount + review.rating) / newCount
            transaction.update(routeRef, "averageRating", newRating)
            transaction.update(routeRef, "reviewCount", newCount)

            val reviewRef = routeRef.collection(FirestoreCollections.REVIEWS.value).document()
            transaction.set(reviewRef, review)
        }.await()
    }

    // -------------------------------------------------------------------------
    // Bingo
    // -------------------------------------------------------------------------

    suspend fun getCurrentBingoCard(): BingoCard? {
        return firestore.collection(FirestoreCollections.BINGO_CARDS.value)
            .orderBy("weekStartDate", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .toObjects(BingoCard::class.java)
            .firstOrNull()
    }
}