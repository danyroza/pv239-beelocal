package com.pv239.beelocal.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import com.pv239.beelocal.domain.FirestoreCollections
import com.pv239.beelocal.domain.FirestoreConfig
import com.pv239.beelocal.domain.Page
import com.pv239.beelocal.model.FeedEntry
import com.pv239.beelocal.model.Route
import com.pv239.beelocal.model.RouteCompletion
import com.pv239.beelocal.model.RouteProgressSnapshot
import com.pv239.beelocal.model.RouteReview
import com.pv239.beelocal.model.UserStatistics
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for all route-related Firestore operations.
 */
@Singleton
class RouteRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    companion object {
        const val ROUTE_COMPLETION_XP = 150
    }

    // -------------------------------------------------------------------------
    // Browsing
    // -------------------------------------------------------------------------

    suspend fun getRoutesByCity(
        city: String,
        lastVisible: DocumentSnapshot? = null,
    ): Page<Route> {
        val baseQuery = firestore.collection(FirestoreCollections.ROUTES.value)
            .whereEqualTo("city", city)
            .orderBy("averageRating", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(FirestoreConfig.ROUTES_BY_CITY_PAGE_SIZE)

        val query = if (lastVisible != null) baseQuery.startAfter(lastVisible) else baseQuery
        val snapshot = query.get().await()
        val hasMore = snapshot.size() == FirestoreConfig.ROUTES_BY_CITY_PAGE_SIZE.toInt()
        return Page(
            items = snapshot.toObjects(Route::class.java),
            cursor = if (hasMore) snapshot.documents.lastOrNull() else null,
            hasMore = hasMore,
        )
    }

    suspend fun getRoute(routeId: String): Route? {
        return firestore.collection(FirestoreCollections.ROUTES.value)
            .document(routeId)
            .get()
            .await()
            .toObject<Route>()
    }

    // -------------------------------------------------------------------------
    // Progress  (RouteProgressSnapshot)
    // -------------------------------------------------------------------------

    /** Returns the user's progress snapshot for a single route, or null if not started. */
    suspend fun getRouteProgress(userId: String, routeId: String): RouteProgressSnapshot? {
        val snapshot = firestore
            .collection(FirestoreCollections.USERS.value)
            .document(userId)
            .collection(FirestoreCollections.ROUTE_PROGRESS.value)
            .document(routeId)
            .get()
            .await()
        if (!snapshot.exists()) return null
        return snapshot.toObject<RouteProgressSnapshot>()
    }

    /**
     * Returns a map of routeId → [RouteProgressSnapshot] for a batch of route IDs.
     * Firestore does not support `whereIn` on sub-collections, so we fetch individually.
     */
    suspend fun getRouteProgressSummary(
        userId: String,
        routeIds: List<String>,
    ): Map<String, RouteProgressSnapshot> {
        if (routeIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, RouteProgressSnapshot>()
        routeIds.forEach { routeId ->
            val progress = getRouteProgress(userId, routeId)
            if (progress != null) result[routeId] = progress
        }
        return result
    }

    /** Creates (or resets) the progress document when the user begins a route. */
    suspend fun startRoute(userId: String, routeId: String) {
        val progress = RouteProgressSnapshot(
            id = routeId,
            userId = userId,
            routeId = routeId,
            completedPointIds = emptyList(),
            lastAnswers = emptyMap(),
            isCompleted = false,
            startedAt = Timestamp.now(),
            completedAt = null,
        )
        firestore
            .collection(FirestoreCollections.USERS.value)
            .document(userId)
            .collection(FirestoreCollections.ROUTE_PROGRESS.value)
            .document(routeId)
            .set(progress)
            .await()
    }

    /**
     * Marks a single checkpoint as complete and persists the user's [answer] so it can
     * be pre-filled when they navigate back to that checkpoint.
     */
    suspend fun completeRoutePoint(
        userId: String,
        routeId: String,
        pointId: String,
        answer: String,
    ) {
        val docRef = firestore
            .collection(FirestoreCollections.USERS.value)
            .document(userId)
            .collection(FirestoreCollections.ROUTE_PROGRESS.value)
            .document(routeId)

        docRef.update(
            mapOf(
                "completedPointIds" to FieldValue.arrayUnion(pointId),
                "lastAnswers.$pointId" to answer,
            )
        ).await()
    }

    // -------------------------------------------------------------------------
    // Completion
    // -------------------------------------------------------------------------

    suspend fun completeRoute(
        userId: String,
        username: String,
        userProfileImageUrl: String?,
        route: Route,
        startedAt: Timestamp?,
    ): RouteCompletion {
        val progressRef = firestore
            .collection(FirestoreCollections.USERS.value)
            .document(userId)
            .collection(FirestoreCollections.ROUTE_PROGRESS.value)
            .document(route.id)

        val statisticsRef = firestore
            .collection(FirestoreCollections.USER_STATISTICS.value)
            .document(userId)

        val completionRef = firestore
            .collection(FirestoreCollections.ROUTE_COMPLETIONS.value)
            .document()

        val now = Timestamp.now()
        val completion = RouteCompletion(
            id = completionRef.id,
            userId = userId,
            username = username,
            userProfileImageUrl = userProfileImageUrl,
            routeId = route.id,
            routeName = route.name,
            city = route.city,
            totalPoints = route.points.size,
            startedAt = startedAt,
            completedAt = now,
        )

        firestore.runTransaction { tx ->
            val statsSnapshot = tx.get(statisticsRef)
            val currentXp = statsSnapshot.getLong("xp")?.toInt() ?: 0
            val newXp = currentXp + ROUTE_COMPLETION_XP

            tx.set(completionRef, completion)
            tx.update(progressRef, mapOf("isCompleted" to true, "completedAt" to now))
            tx.set(
                statisticsRef,
                mapOf("userId" to userId, "xp" to newXp),
                SetOptions.merge(),
            )
        }.await()

        return completion
    }

    // -------------------------------------------------------------------------
    // Review
    // -------------------------------------------------------------------------

    suspend fun addRouteReview(routeId: String, review: RouteReview) {
        val routeRef = firestore.collection(FirestoreCollections.ROUTES.value).document(routeId)
        firestore.runTransaction { tx ->
            val route = tx.get(routeRef).toObject<Route>()
                ?: throw IllegalStateException("Route $routeId does not exist")
            val newCount = route.reviewCount + 1
            val newRating = (route.averageRating * route.reviewCount + review.rating) / newCount
            tx.update(routeRef, "averageRating", newRating)
            tx.update(routeRef, "reviewCount", newCount)
            val reviewRef = routeRef.collection(FirestoreCollections.REVIEWS.value).document()
            tx.set(reviewRef, review)
        }.await()
    }

    suspend fun getRouteReviews(routeId: String): List<RouteReview> {
        val snapshot = firestore
            .collection(FirestoreCollections.ROUTES.value)
            .document(routeId)
            .collection(FirestoreCollections.REVIEWS.value)
            .get()
            .await()
        return snapshot.toObjects(RouteReview::class.java)
    }

    // -------------------------------------------------------------------------
    // Feed sharing
    // -------------------------------------------------------------------------

    suspend fun shareRouteCompletionToFeed(
        completion: RouteCompletion,
        feedEntry: FeedEntry,
    ) {
        val completionRef = firestore
            .collection(FirestoreCollections.ROUTE_COMPLETIONS.value)
            .document(completion.id)

        val feedRef = firestore
            .collection(FirestoreCollections.FEED.value)
            .document()

        firestore.runTransaction { tx ->
            val snap = tx.get(completionRef).toObject(RouteCompletion::class.java)
                ?: return@runTransaction
            if (snap.sharedToFeed) return@runTransaction
            tx.update(completionRef, "sharedToFeed", true)
            tx.set(feedRef, feedEntry)
        }.await()
    }

    // -------------------------------------------------------------------------
    // User statistics
    // -------------------------------------------------------------------------

    suspend fun getUserStatistics(userId: String): UserStatistics? {
        val snapshot = firestore
            .collection(FirestoreCollections.USER_STATISTICS.value)
            .document(userId)
            .get()
            .await()
        if (!snapshot.exists()) return null
        return snapshot.toObject<UserStatistics>()
    }
}