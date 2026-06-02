package com.pv239.beelocal.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import com.pv239.beelocal.domain.FirestoreCollections
import com.pv239.beelocal.domain.XpRewards
import com.pv239.beelocal.model.DailyChallenge
import com.pv239.beelocal.model.DailyChallengeCompletion
import com.pv239.beelocal.model.DailyChallengeHints
import com.pv239.beelocal.model.FeedEntry
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daily-challenge document + per-user completion / hint state, plus the
 * transactional bridge into the feed when the user shares their submission.
 */
@Singleton
class DailyChallengeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    private val challengesCollection
        get() = firestore.collection(FirestoreCollections.DAILY_CHALLENGES.value)

    private fun userDoc(userId: String) =
        firestore.collection(FirestoreCollections.USERS.value).document(userId)

    private fun completionsCollection(userId: String) =
        userDoc(userId).collection(FirestoreCollections.DAILY_COMPLETIONS.value)

    private fun hintsCollection(userId: String) =
        userDoc(userId).collection(FirestoreCollections.DAILY_HINTS.value)

    private fun statisticsDoc(userId: String) =
        firestore.collection(FirestoreCollections.USER_STATISTICS.value).document(userId)

    // -------------------------------------------------------------------------
    // Challenge
    // -------------------------------------------------------------------------

    /**
     * Returns the daily challenge published for the same calendar day as
     * [date] in UTC, or null if no challenge exists for that day.
     *
     * The day boundaries are computed against UTC explicitly so the lookup
     * is independent of the device's default timezone.
     */
    suspend fun getDailyChallenge(date: Timestamp): DailyChallenge? {
        val calendar = java.util.Calendar.getInstance(
            java.util.TimeZone.getTimeZone("UTC")
        ).apply {
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

        return challengesCollection
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThanOrEqualTo("date", endOfDay)
            .limit(1)
            .get()
            .await()
            .toObjects(DailyChallenge::class.java)
            .firstOrNull()
    }

    // -------------------------------------------------------------------------
    // Completion
    // -------------------------------------------------------------------------

    suspend fun getDailyChallengeCompletion(
        userId: String,
        challengeId: String,
    ): DailyChallengeCompletion? {
        return completionsCollection(userId)
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
        val completionRef = completionsCollection(completion.userId)
            .document(completion.challengeId)
        val statisticsRef = statisticsDoc(completion.userId)

        return firestore.runTransaction { tx ->
            if (tx.get(completionRef).exists()) return@runTransaction false
            tx.set(completionRef, completion)
            // Build the statistics merge map dynamically so we never write
            // `xp = increment(0)` (which would still touch the field).
            val statisticsUpdate = buildMap<String, Any> {
                put("userId", completion.userId)
                put("streak", newStreak)
                put("lastStreakUpdate", Timestamp.now())
                if (xpReward > 0) put("xp", FieldValue.increment(xpReward.toLong()))
            }
            tx.set(statisticsRef, statisticsUpdate, SetOptions.merge())
            true
        }.await()
    }

    // -------------------------------------------------------------------------
    // Hints
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
        return hintsCollection(userId)
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

        val hintsRef = hintsCollection(userId).document(challengeId)

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
    // Feed sharing
    // -------------------------------------------------------------------------

    suspend fun shareChallengeToFeed(
        userId: String,
        challengeId: String,
        feedEntry: FeedEntry,
    ) {
        val completionRef = completionsCollection(userId).document(challengeId)
        val feedRef = firestore.collection(FirestoreCollections.FEED.value).document()

        firestore.runTransaction { tx ->
            val completion = tx.get(completionRef)
                .toObject(DailyChallengeCompletion::class.java)
                ?: throw IllegalStateException("Completion $challengeId does not exist")
            if (completion.sharedToFeed) return@runTransaction null
            tx.update(completionRef, "sharedToFeed", true)
            tx.set(feedRef, feedEntry)
        }.await()
    }
}
