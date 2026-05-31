package com.pv239.beelocal.ui.screens.profile

import com.pv239.beelocal.model.FollowRequest
import com.pv239.beelocal.model.User

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Error(val message: String) : ProfileUiState
    data class Ready(
        val user: User,
        val pendingRequests: List<FollowRequest> = emptyList(),
        val visibilityUpdating: Boolean = false,
        /**
         * IDs of [FollowRequest]s currently being accepted/denied. Used to disable
         * the relevant buttons while their Firestore mutation is in flight, so a
         * tap doesn't double-fire.
         */
        val processingRequestIds: Set<String> = emptySet(),
    ) : ProfileUiState
}
