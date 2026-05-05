package com.pv239.beelocal.domain

import com.pv239.beelocal.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // --- User Operations ---
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
        firestore.collection(FirestoreCollections.USERS.value).document(user.id).set(userToSave).await()
    }

    suspend fun searchUsers(query: String): List<User> {
        val normalizedQuery = query.lowercase().trim()
        return firestore.collection(FirestoreCollections.USERS.value)
            .whereGreaterThanOrEqualTo("usernameNormalized", normalizedQuery)
            .whereLessThanOrEqualTo("usernameNormalized", normalizedQuery + "\uf8ff")
            .get()
            .await()
            .toObjects(User::class.java)
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

    // --- User Statistics ---
    suspend fun getStatistics(userId: String): UserStatistics? {
        val snapshot = firestore.collection(FirestoreCollections.USER_STATISTICS.value)
            .document(userId)
            .get()
            .await()
        if (!snapshot.exists()) return null
        return snapshot.toObject<UserStatistics>()
    }

    suspend fun saveStatistics(statistics: UserStatistics) {
        firestore.collection(FirestoreCollections.USER_STATISTICS.value)
            .document(statistics.userId)
            .set(statistics)
            .await()
    }

    suspend fun updateStreak(userId: String, newStreak: Int) {
        firestore.collection(FirestoreCollections.USER_STATISTICS.value).document(userId)
            .update(
                mapOf(
                    "streak" to newStreak,
                    "lastStreakUpdate" to Timestamp.now()
                )
            )
            .await()
    }

    suspend fun updateXp(userId: String, newXp: Int) {
        firestore.collection(FirestoreCollections.USER_STATISTICS.value).document(userId)
            .update("xp", newXp)
            .await()
    }

    // --- Daily Challenge ---
    suspend fun getDailyChallenge(date: Timestamp): DailyChallenge? {
        return firestore.collection(FirestoreCollections.DAILY_CHALLENGES.value)
            .whereEqualTo("date", date)
            .limit(1)
            .get()
            .await()
            .toObjects(DailyChallenge::class.java)
            .firstOrNull()
    }

    // --- Feed ---
    suspend fun getFriendsFeed(friendIds: List<String>): List<FeedEntry> {
        if (friendIds.isEmpty()) return emptyList()

        return friendIds.chunked(30).flatMap { chunk ->
            firestore.collection(FirestoreCollections.FEED.value)
                .whereIn("userId", chunk)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(FeedEntry::class.java)
        }.sortedByDescending { it.timestamp }
    }

    suspend fun addFeedEntry(entry: FeedEntry) {
        firestore.collection(FirestoreCollections.FEED.value).add(entry).await()
    }

    // --- Routes ---
    suspend fun getRoutesByCity(city: String): List<Route> {
        return firestore.collection(FirestoreCollections.ROUTES.value)
            .whereEqualTo("city", city)
            .get()
            .await()
            .toObjects(Route::class.java)
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

    // --- Bingo ---
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
