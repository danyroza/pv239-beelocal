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
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore
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

    /**
     * Real-time stream of the user document. Emits the current value
     * immediately on collection and re-emits whenever the document changes.
     *
     * Emits `null` when the document does not exist; errors from the listener
     * close the flow so the collector can decide how to recover.
     *
     * The underlying Firestore listener is removed automatically when the
     * collector cancels (via [awaitClose]).
     */
    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val registration =
            firestore.collection(FirestoreCollections.USERS.value)
                .document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.takeIf { it.exists() }?.toObject<User>())
                }
        awaitClose { registration.remove() }
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
            .whereLessThanOrEqualTo(
                "usernameNormalized",
                normalizedQuery + "\uf8ff"
            )
            .orderBy("usernameNormalized")
            .limit(USERS_SEARCH_PAGE_SIZE)

        val firestoreQuery =
            if (lastVisible != null) baseQuery.startAfter(lastVisible) else baseQuery

        val snapshot = firestoreQuery.get().await()
        val hasMore = snapshot.size() == USERS_SEARCH_PAGE_SIZE.toInt()
        return Page(
            items = snapshot.toObjects(User::class.java),
            cursor = if (hasMore) snapshot.documents.lastOrNull() else null,
            hasMore = hasMore
        )
    }

    /**
     * Updates the user's profile image fields (URL + storage id) in Firestore.
     * Pass nulls to clear the picture.
     */
    suspend fun updateUserProfileImage(
        userId: String,
        profileImageUrl: String?,
        profileImageId: String?,
    ) {
        firestore.collection(FirestoreCollections.USERS.value).document(userId)
            .update(
                mapOf(
                    "profileImageUrl" to profileImageUrl,
                    "profileImageId" to profileImageId,
                )
            )
            .await()
    }

    suspend fun addFriend(currentUserId: String, friendId: String) {
        firestore.collection(FirestoreCollections.USERS.value)
            .document(currentUserId)
            .update("friends", FieldValue.arrayUnion(friendId))
            .await()
    }

    suspend fun removeFriend(currentUserId: String, friendId: String) {
        firestore.collection(FirestoreCollections.USERS.value)
            .document(currentUserId)
            .update("friends", FieldValue.arrayRemove(friendId))
            .await()
    }

    // --- User Statistics ---
    suspend fun getStatistics(userId: String): UserStatistics? {
        val snapshot =
            firestore.collection(FirestoreCollections.USER_STATISTICS.value)
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
        firestore.collection(FirestoreCollections.USER_STATISTICS.value)
            .document(userId)
            .update(
                mapOf(
                    "streak" to newStreak,
                    "lastStreakUpdate" to Timestamp.now()
                )
            )
            .await()
    }

    suspend fun updateXp(userId: String, newXp: Int) {
        firestore.collection(FirestoreCollections.USER_STATISTICS.value)
            .document(userId)
            .update("xp", newXp)
            .await()
    }

    // -------------------------------------------------------------------------
    // Daily Challenge
    // -------------------------------------------------------------------------

    suspend fun getDailyChallenge(date: Timestamp): DailyChallenge? {
        val calendar = java.util.Calendar.getInstance().apply {
            time = date.toDate()
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = Timestamp(calendar.time)

        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        val endOfDay = Timestamp(calendar.time)

        return firestore.collection(FirestoreCollections.DAILY_CHALLENGES.value)
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThanOrEqualTo("date", endOfDay)
            .limit(1)
            .get()
            .await()
            .toObjects(DailyChallenge::class.java)
            .firstOrNull()
    }

    suspend fun getDailyChallengeCompletion(
        userId: String,
        challengeId: String,
    ): DailyChallengeCompletion? {
        return firestore
            .collection(FirestoreCollections.USERS.value)
            .document(userId)
            .collection(FirestoreCollections.DAILY_COMPLETIONS.value)
            .document(challengeId)
            .get()
            .await()
            .toObject<DailyChallengeCompletion>()
    }

    /**
     * Fetches today's [DailyChallenge] and, if it exists, the current user's
     * [DailyChallengeCompletion] for it — in two parallel reads.
     *
     * Returns a pair of (challenge, completion). Both can be null:
     *  - challenge == null → no challenge published for today
     *  - completion == null → challenge exists but user hasn't completed it yet
     */
    suspend fun getTodaysChallengeWithCompletion(
        userId: String,
    ): Pair<DailyChallenge?, DailyChallengeCompletion?> {
        val challenge =
            getDailyChallenge(Timestamp.now()) ?: return Pair(null, null)
        val completion = getDailyChallengeCompletion(userId, challenge.id)
        return Pair(challenge, completion)
    }

    /**
     * Atomically writes a daily challenge completion and updates the user's streak.
     *
     * @return `true` if a new completion was created, `false` if a completion for
     *         this challenge already existed (in which case nothing was written).
     */
    suspend fun submitDailyChallenge(
        completion: DailyChallengeCompletion,
        newStreak: Int,
    ): Boolean {
        val completionRef = firestore
            .collection(FirestoreCollections.USERS.value)
            .document(completion.userId)
            .collection(FirestoreCollections.DAILY_COMPLETIONS.value)
            .document(completion.challengeId)

        val statisticsRef = firestore
            .collection(FirestoreCollections.USER_STATISTICS.value)
            .document(completion.userId)

        return firestore.runTransaction { tx ->
            if (tx.get(completionRef).exists()) return@runTransaction false
            tx.set(completionRef, completion)
            tx.set(
                statisticsRef,
                mapOf(
                    "userId" to completion.userId,
                    "streak" to newStreak,
                    "lastStreakUpdate" to Timestamp.now(),
                ),
                SetOptions.merge(),
            )
            true
        }.await()
    }

    suspend fun shareChallengeToFeed(
        userId: String,
        challengeId: String,
        feedEntry: FeedEntry,
    ) {
        val completionRef = firestore
            .collection(FirestoreCollections.USERS.value)
            .document(userId)
            .collection(FirestoreCollections.DAILY_COMPLETIONS.value)
            .document(challengeId)

        val feedRef = firestore
            .collection(FirestoreCollections.FEED.value)
            .document()

        firestore.runTransaction { tx ->
            val completion = tx.get(completionRef)
                .toObject(DailyChallengeCompletion::class.java)
                ?: throw IllegalStateException("Completion $challengeId does not exist")
            if (completion.sharedToFeed) return@runTransaction null
            tx.update(completionRef, "sharedToFeed", true)
            tx.set(feedRef, feedEntry)
        }.await()
    }

    // -------------------------------------------------------------------------
    // Feed
    // -------------------------------------------------------------------------

    /**
     * Returns a page of feed entries from the given friends, newest first.
     *
     * Because Firestore's [whereIn] is capped at 10 IDs and cursors are
     * query-shape-specific, a single merged [DocumentSnapshot] cursor cannot
     * safely be shared across chunks. Instead, each chunk fetches one extra item
     * ([FEED_PAGE_SIZE] + 1) so we can detect whether more data exists without
     * exposing a broken cursor. The merged result is trimmed to [FEED_PAGE_SIZE].
     *
     * [Page.cursor] is always null for feed pages. Use [Page.hasMore] to decide
     * whether to show a "load more" control; offset-based or timestamp-based
     * continuation is left to the caller if deeper paging is required.
     */
    suspend fun getFriendsFeed(friendIds: List<String>): Page<FeedEntry> {
        if (friendIds.isEmpty()) return Page(emptyList(), null, hasMore = false)

        val fetchLimit =
            FEED_PAGE_SIZE + 1          // +1 to probe for a next page

        val allDocuments = friendIds.chunked(10).flatMap { chunk ->
            firestore.collection(FirestoreCollections.FEED.value)
                .whereIn("userId", chunk)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(fetchLimit)
                .get()
                .await()
                .documents
        }

        val sorted =
            allDocuments.sortedByDescending { it.getTimestamp("timestamp") }
        val hasMore = sorted.size > FEED_PAGE_SIZE.toInt()
        val pageDocuments = sorted.take(FEED_PAGE_SIZE.toInt())

        return Page(
            items = pageDocuments.mapNotNull { it.toObject(FeedEntry::class.java) },
            cursor = null,
            hasMore = hasMore
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

        val firestoreQuery =
            if (lastVisible != null) baseQuery.startAfter(lastVisible) else baseQuery

        val snapshot = firestoreQuery.get().await()
        val hasMore = snapshot.size() == ROUTES_BY_CITY_PAGE_SIZE.toInt()
        return Page(
            items = snapshot.toObjects(Route::class.java),
            cursor = if (hasMore) snapshot.documents.lastOrNull() else null,
            hasMore = hasMore
        )
    }

    suspend fun addRouteReview(routeId: String, review: RouteReview) {
        val routeRef = firestore.collection(FirestoreCollections.ROUTES.value)
            .document(routeId)
        firestore.runTransaction { transaction ->
            val route = transaction.get(routeRef).toObject<Route>()
                ?: throw IllegalStateException("Route $routeId does not exist, cannot add review")

            val newCount = route.reviewCount + 1
            val newRating =
                (route.averageRating * route.reviewCount + review.rating) / newCount
            transaction.update(routeRef, "averageRating", newRating)
            transaction.update(routeRef, "reviewCount", newCount)

            val reviewRef =
                routeRef.collection(FirestoreCollections.REVIEWS.value)
                    .document()
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

    suspend fun getBingoTaskCompletions(
        userId: String,
        bingoCardId: String,
    ): List<BingoTaskCompletion> {
        return firestore
            .collection(FirestoreCollections.BINGO_TASK_COMPLETIONS.value)
            .whereEqualTo("userId", userId)
            .whereEqualTo("bingoCardId", bingoCardId)
            .get()
            .await()
            .toObjects(BingoTaskCompletion::class.java)
    }

    suspend fun getUserBingoProgress(
        userId: String,
        bingoCardId: String
    ): UserBingoProgress? {
        return firestore
            .collection(FirestoreCollections.USERS.value)
            .document(userId)
            .collection(FirestoreCollections.BINGO_PROGRESS.value)
            .document(bingoCardId)
            .get()
            .await()
            .toObject<UserBingoProgress>()
    }

    /**
     * Marks a bingo task as completed and writes a [BingoTaskCompletion] to the
     * bingo_task_completions collection. The user's progress document is upserted
     * with the new task ID appended to completedTaskIds.
     *
     * @return `true` if the task was newly completed, `false` if it was already done.
     */
    suspend fun completeBingoTask(
        progress: UserBingoProgress,
        completion: BingoTaskCompletion,
    ): Boolean {
        val progressRef = firestore
            .collection(FirestoreCollections.USERS.value)
            .document(progress.userId)
            .collection(FirestoreCollections.BINGO_PROGRESS.value)
            .document(progress.bingoCardId)

        val completionRef = firestore
            .collection(FirestoreCollections.BINGO_TASK_COMPLETIONS.value)
            .document()

        return firestore.runTransaction { tx ->
            val existing = tx.get(progressRef).toObject<UserBingoProgress>()
            // Idempotency check inside the transaction so it's atomic
            if (existing?.completedTaskIds?.contains(completion.taskId) == true) {
                return@runTransaction false
            }
            val updatedProgress = (existing ?: progress).copy(
                completedTaskIds = (existing?.completedTaskIds
                    ?: emptyList()) + completion.taskId,
                sharedToFeed = existing?.sharedToFeed ?: progress.sharedToFeed,
            )
            tx.set(progressRef, updatedProgress)
            tx.set(completionRef, completion)
            true
        }.await()
    }

    suspend fun shareBingoToFeed(
        userId: String,
        bingoCardId: String,
        feedEntry: FeedEntry,
    ) {
        val progressRef = firestore
            .collection(FirestoreCollections.USERS.value)
            .document(userId)
            .collection(FirestoreCollections.BINGO_PROGRESS.value)
            .document(bingoCardId)

        val feedRef = firestore
            .collection(FirestoreCollections.FEED.value)
            .document()

        firestore.runTransaction { tx ->
            val progress = tx.get(progressRef).toObject<UserBingoProgress>()
            if (progress?.sharedToFeed == true) return@runTransaction null
            tx.update(progressRef, "sharedToFeed", true)
            tx.set(feedRef, feedEntry)
        }.await()
    }
}
