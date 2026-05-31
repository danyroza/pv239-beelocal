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
    ) : DailyChallengeUiState

    data object NoChallengeToday : DailyChallengeUiState

    data class Error(val message: String) : DailyChallengeUiState
}

sealed interface CompletionState {
    data object NotCompleted : CompletionState
    data object Submitting : CompletionState
    data class SubmissionFailed(val errorMessage: String) : CompletionState
    data class Completed(
        val imageId: String,
        val photoUrl: String,
        val streakCount: Int,
        val sharedToFeed: Boolean,
    ) : CompletionState
}
