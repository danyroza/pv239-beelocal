package com.pv239.beelocal.ui.screens.userprofile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pv239.beelocal.data.repository.SocialRepository
import com.pv239.beelocal.data.repository.UserRepository
import com.pv239.beelocal.model.UserStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "UserProfileViewModel"

/**
 * Backs the public user-profile screen.
 *
 * The screen is reachable from anywhere a username/avatar is rendered — the
 * feed cards, friends cluster on the self-profile, Social → Friends / Search
 * tabs. The view model is responsible for:
 *  - loading the target [com.pv239.beelocal.model.User] + their statistics,
 *  - resolving the viewer's relationship to them (self / following /
 *    pending follow request),
 *  - lazily fetching the target's shared feed entries, gated by privacy,
 *  - mutating the relationship via follow / unfollow / cancel-request.
 *
 * Self-profile views are surfaced as `isSelf = true`; the UI is expected to
 * either redirect to the editable [com.pv239.beelocal.navigation.ProfileRoute]
 * or simply hide the follow controls.
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Loading)
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    /** Id of the user currently being displayed. Set by [load]. */
    private var currentUserId: String? = null

    /**
     * Load the profile for [userId]. Safe to call multiple times — repeated
     * invocations with the same id are no-ops after the initial fetch
     * completes, while a different id resets state and triggers a fresh load.
     */
    fun load(userId: String) {
        if (userId.isBlank()) {
            _uiState.value = UserProfileUiState.Error("Missing user id")
            return
        }
        // De-dup repeated load() calls (e.g. recomposition firing LaunchedEffect)
        // for the same target so we don't pummel Firestore on every config change.
        if (currentUserId == userId && _uiState.value is UserProfileUiState.Ready) return

        currentUserId = userId
        _uiState.value = UserProfileUiState.Loading

        viewModelScope.launch {
            try {
                val viewerId = auth.currentUser?.uid
                val isSelf = viewerId == userId

                // Parallelise the three independent reads: user document,
                // statistics document, and the follow-relationship probes
                // (latter two only meaningful for non-self views).
                val userDeferred = async { socialRepository.getUser(userId) }
                val statsDeferred = async { userRepository.getStatistics(userId) }
                val followingDeferred = async {
                    if (isSelf) false else socialRepository.isFollowing(userId)
                }
                val pendingDeferred = async {
                    if (isSelf) false else socialRepository.hasPendingRequestTo(userId)
                }

                val results = awaitAll(
                    userDeferred,
                    statsDeferred,
                    followingDeferred,
                    pendingDeferred,
                )
                @Suppress("UNCHECKED_CAST")
                val user = results[0] as com.pv239.beelocal.model.User?
                @Suppress("UNCHECKED_CAST")
                val statistics = results[1] as UserStatistics?
                val isFollowing = results[2] as Boolean
                val hasPendingRequest = results[3] as Boolean

                if (user == null) {
                    _uiState.value = UserProfileUiState.Error("User not found")
                    return@launch
                }

                val canSeeFeed = isSelf || user.profilePublic || isFollowing

                _uiState.value = UserProfileUiState.Ready(
                    user = user,
                    statistics = statistics ?: UserStatistics(userId = userId),
                    isSelf = isSelf,
                    isFollowing = isFollowing,
                    hasPendingRequest = hasPendingRequest,
                    canSeeFeed = canSeeFeed,
                    feedLoading = canSeeFeed,
                )

                if (canSeeFeed) loadFeed(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load user profile $userId", e)
                _uiState.value = UserProfileUiState.Error(
                    e.message ?: "Failed to load profile"
                )
            }
        }
    }

    private fun loadFeed(userId: String) {
        viewModelScope.launch {
            runCatching { socialRepository.getUserFeed(userId) }
                .onSuccess { page ->
                    _uiState.update { state ->
                        (state as? UserProfileUiState.Ready)?.copy(
                            feedEntries = page.items,
                            feedLoading = false,
                        ) ?: state
                    }
                }
                .onFailure { err ->
                    Log.w(TAG, "Failed to load feed for $userId", err)
                    _uiState.update { state ->
                        (state as? UserProfileUiState.Ready)?.copy(
                            feedLoading = false,
                        ) ?: state
                    }
                }
        }
    }

    /**
     * Send a follow request to the loaded user. Honors the target's profile
     * visibility — public profiles flip [UserProfileUiState.Ready.isFollowing]
     * immediately, while private profiles transition into the
     * [UserProfileUiState.Ready.hasPendingRequest] state.
     */
    fun follow() {
        val current = _uiState.value as? UserProfileUiState.Ready ?: return
        if (current.isSelf || current.followActionInFlight) return
        if (current.isFollowing || current.hasPendingRequest) return

        val targetId = current.user.id
        _uiState.value = current.copy(followActionInFlight = true)

        viewModelScope.launch {
            runCatching { socialRepository.requestFollow(targetId) }
                .onSuccess { accepted ->
                    _uiState.update { state ->
                        val ready = state as? UserProfileUiState.Ready ?: return@update state
                        if (accepted) {
                            ready.copy(
                                isFollowing = true,
                                hasPendingRequest = false,
                                canSeeFeed = true,
                                followActionInFlight = false,
                                snackbarMessage = "Friend added!",
                            )
                        } else {
                            ready.copy(
                                hasPendingRequest = true,
                                followActionInFlight = false,
                                snackbarMessage = "Follow request sent",
                            )
                        }
                    }
                    // Reveal the feed now that we can see it (public profile case).
                    if (accepted) loadFeed(targetId)
                }
                .onFailure { err ->
                    Log.e(TAG, "Failed to follow $targetId", err)
                    _uiState.update { state ->
                        (state as? UserProfileUiState.Ready)?.copy(
                            followActionInFlight = false,
                            snackbarMessage = err.message ?: "Could not send follow request",
                        ) ?: state
                    }
                }
        }
    }

    /** Open the confirmation dialog before actually unfollowing. */
    fun requestUnfollow() {
        val current = _uiState.value as? UserProfileUiState.Ready ?: return
        if (!current.isFollowing || current.followActionInFlight) return
        _uiState.value = current.copy(showUnfollowDialog = true)
    }

    /** Dismiss the unfollow confirmation dialog without taking action. */
    fun dismissUnfollowDialog() {
        _uiState.update { state ->
            (state as? UserProfileUiState.Ready)?.copy(showUnfollowDialog = false) ?: state
        }
    }

    /** Confirm the unfollow action — performs the actual repository write. */
    fun confirmUnfollow() {
        val current = _uiState.value as? UserProfileUiState.Ready ?: return
        if (!current.isFollowing) {
            _uiState.value = current.copy(showUnfollowDialog = false)
            return
        }

        val targetId = current.user.id
        _uiState.value = current.copy(
            showUnfollowDialog = false,
            followActionInFlight = true,
        )

        viewModelScope.launch {
            runCatching { socialRepository.removeFriend(targetId) }
                .onSuccess {
                    _uiState.update { state ->
                        val ready = state as? UserProfileUiState.Ready ?: return@update state
                        // Once unfollowed, a private profile's feed is no longer
                        // visible — clear it so we don't keep stale entries on screen.
                        val nowVisible = ready.isSelf || ready.user.profilePublic
                        ready.copy(
                            isFollowing = false,
                            canSeeFeed = nowVisible,
                            feedEntries = if (nowVisible) ready.feedEntries else emptyList(),
                            followActionInFlight = false,
                            snackbarMessage = "Unfollowed @${ready.user.username}",
                        )
                    }
                }
                .onFailure { err ->
                    Log.e(TAG, "Failed to unfollow $targetId", err)
                    _uiState.update { state ->
                        (state as? UserProfileUiState.Ready)?.copy(
                            followActionInFlight = false,
                            snackbarMessage = err.message ?: "Could not unfollow",
                        ) ?: state
                    }
                }
        }
    }

    /** Cancel an outstanding follow request that has not yet been approved. */
    fun cancelPendingRequest() {
        val current = _uiState.value as? UserProfileUiState.Ready ?: return
        if (!current.hasPendingRequest || current.followActionInFlight) return

        val targetId = current.user.id
        _uiState.value = current.copy(followActionInFlight = true)

        viewModelScope.launch {
            runCatching { socialRepository.cancelFollowRequestTo(targetId) }
                .onSuccess {
                    _uiState.update { state ->
                        (state as? UserProfileUiState.Ready)?.copy(
                            hasPendingRequest = false,
                            followActionInFlight = false,
                            snackbarMessage = "Follow request cancelled",
                        ) ?: state
                    }
                }
                .onFailure { err ->
                    Log.e(TAG, "Failed to cancel follow request to $targetId", err)
                    _uiState.update { state ->
                        (state as? UserProfileUiState.Ready)?.copy(
                            followActionInFlight = false,
                            snackbarMessage = err.message ?: "Could not cancel request",
                        ) ?: state
                    }
                }
        }
    }

    fun snackbarShown() {
        _uiState.update { state ->
            (state as? UserProfileUiState.Ready)?.copy(snackbarMessage = null) ?: state
        }
    }
}
