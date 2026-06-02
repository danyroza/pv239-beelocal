package com.pv239.beelocal.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import com.pv239.beelocal.domain.FirestoreCollections
import com.pv239.beelocal.domain.XpRewards
import com.pv239.beelocal.model.BingoCard
import com.pv239.beelocal.model.BingoTaskCompletion
import com.pv239.beelocal.model.FeedEntry
import com.pv239.beelocal.model.UserBingoProgress
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bingo card publishing + per-user task completion / card-share state.
 *
 * The "complete a task" transaction is the heart of this repository: it must
 * atomically record the [BingoTaskCompletion], extend the user's
 * [UserBingoProgress.completedTaskIds] list, and award per-task / per-card XP
 * — without letting concurrent clients double-credit anything.
 */
@Singleton
class BingoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    private val cardsCollection
        get() = firestore.collection(FirestoreCollections.BINGO_CARDS.value)

    private val taskCompletionsCollection
        get() = firestore.collection(FirestoreCollections.BINGO_TASK_COMPLETIONS.value)

    private fun progressCollection(userId: String) = firestore
        .collection(FirestoreCollections.USERS.value)
        .document(userId)
        .collection(FirestoreCollections.BINGO_PROGRESS.value)

    private fun statisticsDoc(userId: String) =
        firestore.collection(FirestoreCollections.USER_STATISTICS.value).document(userId)

    // -------------------------------------------------------------------------
    // Card
    // -------------------------------------------------------------------------

    suspend fun getCurrentBingoCard(): BingoCard? {
        return cardsCollection
            .orderBy("weekStartDate", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .toObjects(BingoCard::class.java)
            .firstOrNull()
    }

    // -------------------------------------------------------------------------
    // Progress
    // -------------------------------------------------------------------------

    suspend fun getBingoTaskCompletions(
        userId: String,
        bingoCardId: String,
    ): List<BingoTaskCompletion> {
        return taskCompletionsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("bingoCardId", bingoCardId)
            .get()
            .await()
            .toObjects(BingoTaskCompletion::class.java)
    }

    suspend fun getUserBingoProgress(
        userId: String,
        bingoCardId: String,
    ): UserBingoProgress? {
        return progressCollection(userId)
            .document(bingoCardId)
            .get()
            .await()
            .toObject<UserBingoProgress>()
    }

    // -------------------------------------------------------------------------
    // Completion
    // -------------------------------------------------------------------------

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
        val progressRef = progressCollection(progress.userId).document(progress.bingoCardId)
        val completionRef = taskCompletionsCollection.document()
        val statisticsRef = statisticsDoc(progress.userId)

        return firestore.runTransaction { tx ->
            val existing = tx.get(progressRef).toObject<UserBingoProgress>()
            // Idempotency check inside the transaction so it's atomic
            if (existing?.completedTaskIds?.contains(completion.taskId) == true) {
                return@runTransaction BingoCompletionResult.AlreadyDone
            }
            val newCompletedTaskIds =
                (existing?.completedTaskIds ?: emptyList()) + completion.taskId
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

    // -------------------------------------------------------------------------
    // Feed sharing
    // -------------------------------------------------------------------------

    suspend fun shareBingoToFeed(
        userId: String,
        bingoCardId: String,
        feedEntry: FeedEntry,
    ) {
        val progressRef = progressCollection(userId).document(bingoCardId)
        val feedRef = firestore.collection(FirestoreCollections.FEED.value).document()

        firestore.runTransaction { tx ->
            val progress = tx.get(progressRef).toObject<UserBingoProgress>()
                ?: throw IllegalStateException("Bingo progress $bingoCardId does not exist")
            if (progress.sharedToFeed) return@runTransaction null
            tx.update(progressRef, "sharedToFeed", true)
            tx.set(feedRef, feedEntry)
        }.await()
    }
}
