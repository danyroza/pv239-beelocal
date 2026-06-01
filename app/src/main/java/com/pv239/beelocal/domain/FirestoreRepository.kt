package com.pv239.beelocal.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import com.pv239.beelocal.domain.FirestoreConfig.FEED_PAGE_SIZE
import com.pv239.beelocal.domain.FirestoreConfig.USERS_SEARCH_PAGE_SIZE
import com.pv239.beelocal.model.BingoCard
import com.pv239.beelocal.model.BingoTaskCompletion
import com.pv239.beelocal.model.DailyChallenge
import com.pv239.beelocal.model.DailyChallengeCompletion
import com.pv239.beelocal.model.DailyChallengeHints
import com.pv239.beelocal.model.FeedEntry
import com.pv239.beelocal.model.FollowRequest
import com.pv239.beelocal.model.User
import com.pv239.beelocal.model.UserBingoProgress
import com.pv239.beelocal.model.UserStatistics
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
     * Reactive stream of [userId]'s user document. Emits the latest [User]
     * whenever it changes in Firestore (e.g. profile picture upload, friends
     * list mutation). Emits `null` if the document does not exist.
     *
     * The underlying Firestore snapshot listener is automatically detached
     * when the collector cancels.
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

    /**
     * Reactive stream of [userId]'s [UserStatistics] document. Emits whenever
     * streak / xp changes so headers and summaries can react to mutations
     * made elsewhere in the app.
     */
    fun observeStatistics(userId: String): Flow<UserStatistics?> = callbackFlow {
        val registration = firestore.collection(FirestoreCollections.USER_STATISTICS.value)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.toObject<UserStatistics>())
            }
        awaitClose { registration.remove() }
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
     * Update the user's profile picture URL (typically after a fresh upload to
     * Cloud Storage). Pass `null` to clear it back to the default avatar.
     */
    suspend fun updateProfileImage(userId: String, profileImageUrl: String?) {
        firestore.collection(FirestoreCollections.USERS.value).document(userId)
            .update("profileImageUrl", profileImageUrl)
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

    // -------------------------------------------------------------------------
    // Profile visibility & follow requests
    // -------------------------------------------------------------------------

    /**
     * Toggle whether the user's profile is publicly followable.
     *
     * Switching to public does **not** auto-accept already-pending requests;
     * those still require an explicit accept/deny (so the user always
     * consciously sees who asked while they were private).
     */
    suspend fun updateProfileVisibility(userId: String, isPublic: Boolean) {
        firestore.collection(FirestoreCollections.USERS.value).document(userId)
            .update("isProfilePublic", isPublic)
            .await()
    }

    /**
     * Request to follow [toUserId] from [fromUser].
     *
     * - If the target user has a **public** profile, no confirmation is needed
     *   and [toUserId] is immediately added to [fromUser]'s friends list (i.e.
     *   the follower now sees the target's feed entries).
     * - If the target is **private**, a [FollowRequest] document is created
     *   instead and must be accepted by the target via [acceptFollowRequest].
     *
     * Returns `true` if the follow was accepted immediately, `false` if a
     * request was created and is awaiting approval.
     */
    suspend fun requestFollow(fromUser: User, toUserId: String): Boolean {
        if (fromUser.id == toUserId) return false
        val target = getUser(toUserId)
            ?: throw IllegalStateException("Target user $toUserId does not exist")

        return if (target.isProfilePublic) {
            addFriend(fromUser.id, toUserId)
            true
        } else {
            // Use a deterministic document id so duplicate requests are
            // impossible by construction, and perform the existence check +
            // write inside a single transaction to eliminate the TOCTOU race condition
            val requestRef = firestore.collection(FirestoreCollections.FOLLOW_REQUESTS.value)
                .document("${fromUser.id}_$toUserId")

            firestore.runTransaction { tx ->
                val snapshot = tx.get(requestRef)
                if (!snapshot.exists()) {
                    val request = FollowRequest(
                        fromUserId = fromUser.id,
                        fromUsername = fromUser.username,
                        fromUserProfileImageUrl = fromUser.profileImageUrl,
                        toUserId = toUserId,
                    )
                    tx.set(requestRef, request)
                }
                null
            }.await()
            false
        }
    }

    /**
     * Returns the list of [FollowRequest]s currently awaiting [userId]'s
     * approval, newest first.
     */
    suspend fun getPendingFollowRequests(userId: String): List<FollowRequest> {
        return firestore.collection(FirestoreCollections.FOLLOW_REQUESTS.value)
            .whereEqualTo("toUserId", userId)
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(FollowRequest::class.java)
    }

    /**
     * Accepts a pending follow request: atomically deletes the request and
     * adds the target (`toUserId`) to the requester's (`fromUserId`) friends
     * list — meaning the requester now sees the target's shared feed.
     */
    suspend fun acceptFollowRequest(request: FollowRequest) {
        val requestRef = firestore.collection(FirestoreCollections.FOLLOW_REQUESTS.value)
            .document(request.id)
        val followerRef = firestore.collection(FirestoreCollections.USERS.value)
            .document(request.fromUserId)

        firestore.runTransaction { tx ->
            tx.update(followerRef, "friends", FieldValue.arrayUnion(request.toUserId))
            tx.delete(requestRef)
        }.await()
    }

    /** Deny a pending follow request — just deletes the document. */
    suspend fun denyFollowRequest(requestId: String) {
        firestore.collection(FirestoreCollections.FOLLOW_REQUESTS.value)
            .document(requestId)
            .delete()
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

    // -------------------------------------------------------------------------
    // Daily Challenge hints (paid reveals — direction & map)
    // -------------------------------------------------------------------------

    /**
     * Returns the per-challenge hint document for [userId]. A `null` return
     * (i.e. the document does not exist) is semantically identical to "no
     * hints unlocked yet", so callers should fall back to a default
     * [DailyChallengeHints] in that case.
     */
    suspend fun getDailyChallengeHints(
        userId: String,
        challengeId: String,
    ): DailyChallengeHints? {
        return firestore
            .collection(FirestoreCollections.USERS.value)
            .document(userId)
            .collection(FirestoreCollections.DAILY_HINTS.value)
            .document(challengeId)
            .get()
            .await()
            .toObject<DailyChallengeHints>()
    }

    /**
     * Atomically unlocks a single hint for the given challenge.
     *
     * The hint cost is **not** deducted from the user's current XP balance —
     * it instead reduces the eventual submission reward (see
     * [XpRewards.dailyChallengeReward]). So this transaction merely flips
     * the hint flag; the cost is realised later when the user submits.
     *
     * If the flag is already true the transaction is a no-op.
     *
     * @param hintField must be `"directionUnlocked"` or `"mapUnlocked"` — those are
     *   the only writable fields on a [DailyChallengeHints] document.
     */
    suspend fun unlockDailyChallengeHint(
        userId: String,
        challengeId: String,
        hintField: String,
    ): HintUnlockResult {
        require(hintField == "directionUnlocked" || hintField == "mapUnlocked") {
            "Unknown hint field: $hintField"
        }

        val hintsRef = firestore
            .collection(FirestoreCollections.USERS.value)
            .document(userId)
            .collection(FirestoreCollections.DAILY_HINTS.value)
            .document(challengeId)

        return firestore.runTransaction { tx ->
            val hintsSnapshot = tx.get(hintsRef)
            val existingHints = hintsSnapshot.toObject<DailyChallengeHints>()
                ?: DailyChallengeHints(id = challengeId)
            val alreadyUnlocked = when (hintField) {
                "directionUnlocked" -> existingHints.directionUnlocked
                "mapUnlocked" -> existingHints.mapUnlocked
                else -> false
            }
            if (alreadyUnlocked) {
                return@runTransaction HintUnlockResult.AlreadyUnlocked(existingHints)
            }

            // Merge so this works whether or not the hints doc already exists,
            // and survives future schema additions.
            tx.set(
                hintsRef,
                mapOf(hintField to true),
                SetOptions.merge(),
            )

            val updated = when (hintField) {
                "directionUnlocked" -> existingHints.copy(directionUnlocked = true)
                "mapUnlocked" -> existingHints.copy(mapUnlocked = true)
                else -> existingHints
            }
            HintUnlockResult.Unlocked(updated)
        }.await()
    }

    /** Result of an atomic hint-unlock attempt. */
    sealed interface HintUnlockResult {
        /** The hint flag was newly set on the hints document. */
        data class Unlocked(val hints: DailyChallengeHints) : HintUnlockResult
        /** The flag was already set; document was untouched. */
        data class AlreadyUnlocked(val hints: DailyChallengeHints) : HintUnlockResult
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
     * Atomically writes a daily challenge completion, updates the user's
     * streak, and awards [xpReward] XP — all inside a single Firestore
     * transaction so the user can never be left in a half-credited state.
     *
     * Callers compute [xpReward] from [XpRewards.dailyChallengeReward] based
     * on how many paid hints were unlocked for this challenge.
     *
     * @return `true` if a new completion was created, `false` if a completion for
     *         this challenge already existed (in which case nothing was written —
     *         including no XP award, so duplicate submits can't double-credit).
     */
    suspend fun submitDailyChallenge(
        completion: DailyChallengeCompletion,
        newStreak: Int,
        xpReward: Int = XpRewards.DAILY_CHALLENGE_FULL,
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
            // Build the statistics merge map dynamically so we never write
            // `xp = increment(0)` (which would still touch the field).
            val statisticsUpdate = buildMap {
                put("userId", completion.userId)
                put("streak", newStreak)
                put("lastStreakUpdate", Timestamp.now())
                if (xpReward > 0) put("xp", FieldValue.increment(xpReward.toLong()))
            }
            tx.set(statisticsRef, statisticsUpdate, SetOptions.merge())
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
     * Atomically also awards the user [XpRewards.BINGO_TASK] XP, and an
     * additional [XpRewards.BINGO_CARD_FULL] bonus on the completion that fills
     * the 16th cell. The whole-card bonus is naturally one-time because the
     * transaction short-circuits on duplicate task IDs.
     *
     * @return [BingoCompletionResult.AlreadyDone] if the task was already completed,
     *         otherwise [BingoCompletionResult.Awarded] carrying the XP amounts so the
     *         UI can show "+50 XP" or "+250 XP" toasts.
     */
    suspend fun completeBingoTask(
        progress: UserBingoProgress,
        completion: BingoTaskCompletion,
    ): BingoCompletionResult {
        val progressRef = firestore
            .collection(FirestoreCollections.USERS.value)
            .document(progress.userId)
            .collection(FirestoreCollections.BINGO_PROGRESS.value)
            .document(progress.bingoCardId)

        val completionRef = firestore
            .collection(FirestoreCollections.BINGO_TASK_COMPLETIONS.value)
            .document()

        val statisticsRef = firestore
            .collection(FirestoreCollections.USER_STATISTICS.value)
            .document(progress.userId)

        return firestore.runTransaction { tx ->
            val existing = tx.get(progressRef).toObject<UserBingoProgress>()
            // Idempotency check inside the transaction so it's atomic
            if (existing?.completedTaskIds?.contains(completion.taskId) == true) {
                return@runTransaction BingoCompletionResult.AlreadyDone
            }
            val newCompletedTaskIds = (existing?.completedTaskIds
                ?: emptyList()) + completion.taskId
            val updatedProgress = (existing ?: progress).copy(
                completedTaskIds = newCompletedTaskIds,
                sharedToFeed = existing?.sharedToFeed ?: progress.sharedToFeed,
            )
            tx.set(progressRef, updatedProgress)
            tx.set(completionRef, completion)

            // Per-task reward; plus a one-time bonus when this completion is
            // the one that fills the final cell of the 4x4 card.
            val cardCompletedNow = newCompletedTaskIds.size == XpRewards.BINGO_CARD_SIZE
            val taskXp = XpRewards.BINGO_TASK
            val bonusXp = if (cardCompletedNow) XpRewards.BINGO_CARD_FULL else 0
            val totalXp = taskXp + bonusXp
            tx.set(
                statisticsRef,
                mapOf(
                    "userId" to progress.userId,
                    "xp" to FieldValue.increment(totalXp.toLong()),
                ),
                SetOptions.merge(),
            )

            BingoCompletionResult.Awarded(
                taskXp = taskXp,
                cardBonusXp = bonusXp,
                cardCompleted = cardCompletedNow,
            )
        }.await()
    }

    /** Outcome of attempting to complete a bingo cell, including XP awarded. */
    sealed interface BingoCompletionResult {
        /** The task was already marked completed — nothing was written, no XP awarded. */
        data object AlreadyDone : BingoCompletionResult

        /**
         * The completion was successfully recorded.
         *
         * @property taskXp Per-task reward awarded (always [XpRewards.BINGO_TASK]).
         * @property cardBonusXp Extra bonus awarded if this completion finished the card.
         * @property cardCompleted True if this completion filled the 16th cell.
         */
        data class Awarded(
            val taskXp: Int,
            val cardBonusXp: Int,
            val cardCompleted: Boolean,
        ) : BingoCompletionResult {
            val totalXp: Int get() = taskXp + cardBonusXp
        }
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
