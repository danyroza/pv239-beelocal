package com.pv239.beelocal.ui.screens.dailychallenge

import com.pv239.beelocal.model.DailyChallenge

sealed interface DailyChallengeUiState {
    data object Loading : DailyChallengeUiState

    data class Ready(
        val challenge: DailyChallenge,
        val secondsRemaining: Long,
        val distanceMeters: Int?,
        val completion: CompletionState,
    ) : DailyChallengeUiState

    data object NoChallengeToday : DailyChallengeUiState

    data class Error(val message: String) : DailyChallengeUiState
}

sealed interface CompletionState {
    data object NotCompleted : CompletionState
    data object Submitting : CompletionState
    data class Completed(
        val photoUrl: String,
        val streakCount: Int,
        val sharedToFeed: Boolean,
    ) : CompletionState
}