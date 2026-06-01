package com.pv239.beelocal.ui.screens.dailychallenge

import com.pv239.beelocal.model.DailyChallenge

sealed interface DailyChallengeUiState {
    data object Loading : DailyChallengeUiState

    data class Ready(
        val challenge: DailyChallenge,
        val secondsRemaining: Long,
        val distanceMeters: Int?,
        val completion: CompletionState,
        val userLatLng: Pair<Double, Double>? = null,
        /**
         * User bearing-from-north heading toward the (real, not obfuscated)
         * challenge location. Recomputed on every GPS fix in the view model
         * so the UI only needs to read it. Null until the first fix arrives.
         */
        val bearingDegrees: Float? = null,
        // ── Hints (paid reveals) ──────────────────────────────────────────
        /** True once the user has paid to reveal the direction arrow. */
        val directionUnlocked: Boolean = false,
        /** True once the user has paid to reveal the approximate-area map. */
        val mapUnlocked: Boolean = false,
        /** Live XP balance, mirrored from `user_statistics` so we can disable
         *  unlock buttons the user can't afford. */
        val currentXp: Int = 0,
        /** Which hint, if any, is currently being unlocked (button disable + spinner). */
        val hintUnlockInFlight: HintKind? = null,
        /** Transient error message after a failed hint unlock attempt. Cleared
         *  automatically on the next successful unlock or screen refresh. */
        val hintUnlockError: String? = null,
    ) : DailyChallengeUiState {
        val hintsUnlockedCount: Int
            get() = (if (directionUnlocked) 1 else 0) + (if (mapUnlocked) 1 else 0)
    }

    data object NoChallengeToday : DailyChallengeUiState

    data class Error(val message: String) : DailyChallengeUiState
}

/** Identifies a specific paid hint on the daily challenge screen. */
enum class HintKind { DIRECTION, MAP }

sealed interface CompletionState {
    data object NotCompleted : CompletionState
    data object Submitting : CompletionState
    data class SubmissionFailed(val errorMessage: String) : CompletionState
    data class Completed(
        val imageId: String,
        val photoUrl: String,
        val streakCount: Int,
        val sharedToFeed: Boolean,
        /** XP that was awarded for this specific submission, used to celebrate the result. */
        val xpAwarded: Int = 0,
    ) : CompletionState
}
