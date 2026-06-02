package com.pv239.beelocal.ui.screens.userprofile

import com.pv239.beelocal.model.FeedEntry
import com.pv239.beelocal.model.User
import com.pv239.beelocal.model.UserStatistics

/**
 * UI state for the public user-profile screen.
 *
 * Distinct from [com.pv239.beelocal.ui.screens.profile.ProfileUiState] in two
 * important ways:
 *  - It models *another* user (or the self-redirect edge case), so it carries
 *    `isSelf` + follow status flags the editable self-profile screen has no
 *    need for.
 *  - The feed list is the **target user's own** activity, not the friends
 *    feed — and is gated behind [canSeeFeed] so private profiles only expose
 *    posts to followers.
 */
sealed interface UserProfileUiState {
    data object Loading : UserProfileUiState
    data class Error(val message: String) : UserProfileUiState
    data class Ready(
        val user: User,
        val statistics: UserStatistics = UserStatistics(),
        val feedEntries: List<FeedEntry> = emptyList(),
        /** True if the viewer is looking at their own id (should be redirected). */
        val isSelf: Boolean = false,
        /** True if the viewer already has [user] in their friends list. */
        val isFollowing: Boolean = false,
        /** True if the viewer has an outstanding follow request to [user]. */
        val hasPendingRequest: Boolean = false,
        /**
         * True when the viewer is allowed to read [feedEntries] — either the
         * target profile is public, the viewer follows the target, or the
         * viewer *is* the target.
         */
        val canSeeFeed: Boolean = false,
        /** True while a follow / unfollow / cancel-request action is in flight. */
        val followActionInFlight: Boolean = false,
        /** True if the feed page is still being fetched. */
        val feedLoading: Boolean = false,
        /** Non-null while the unfollow confirmation dialog is open. */
        val showUnfollowDialog: Boolean = false,
        /** Transient snackbar message; cleared via [UserProfileViewModel.snackbarShown]. */
        val snackbarMessage: String? = null,
    ) : UserProfileUiState
}
