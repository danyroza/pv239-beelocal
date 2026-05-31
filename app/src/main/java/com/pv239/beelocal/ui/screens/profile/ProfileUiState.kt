package com.pv239.beelocal.ui.screens.profile

import com.pv239.beelocal.model.FollowRequest
import com.pv239.beelocal.model.User
import com.pv239.beelocal.model.UserStatistics

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Error(val message: String) : ProfileUiState
    data class Ready(
        val user: User,
        val statistics: UserStatistics = UserStatistics(),
        val pendingRequests: List<FollowRequest> = emptyList(),
        val visibilityUpdating: Boolean = false,
        /** True while a new profile picture is being uploaded to storage. */
        val pictureUploading: Boolean = false,
        /** Last upload error message, surfaced inline; null while idle/successful. */
        val pictureUploadError: String? = null,
        /**
         * IDs of [FollowRequest]s currently being accepted/denied. Used to disable
         * the relevant buttons while their Firestore mutation is in flight, so a
         * tap doesn't double-fire.
         */
        val processingRequestIds: Set<String> = emptySet(),
    ) : ProfileUiState
}
