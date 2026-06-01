package com.pv239.beelocal.model

import com.google.firebase.firestore.DocumentId

/**
 * Tracks which paid hints the user has unlocked for a specific [DailyChallenge].
 *
 * Stored at `users/{uid}/daily_hints/{challengeId}`. The document is created
 * lazily the first time a hint is unlocked — its absence means "no hints
 * unlocked yet", same as a doc with all flags `false`.
 *
 * Once a hint is unlocked it is **permanent for that challenge**: the XP is
 * spent and can't be refunded by skipping the hint. This keeps the XP economy
 * decision simple and impossible to game by toggling.
 */
data class DailyChallengeHints(
    @DocumentId
    val id: String = "",
    /** True once the user has paid to reveal the direction arrow + bearing. */
    val directionUnlocked: Boolean = false,
    /** True once the user has paid to reveal the approximate-area map. */
    val mapUnlocked: Boolean = false,
) {
    /** Number of paid hints unlocked — drives the submission reward computation. */
    val unlockedCount: Int
        get() = (if (directionUnlocked) 1 else 0) + (if (mapUnlocked) 1 else 0)
}
