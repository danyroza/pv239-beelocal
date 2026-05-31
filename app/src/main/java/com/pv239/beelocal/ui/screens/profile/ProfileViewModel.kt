package com.pv239.beelocal.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pv239.beelocal.domain.FirestoreRepository
import com.pv239.beelocal.model.FollowRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ProfileViewModel"

/**
 * Backs the user-facing profile screen where the current user can:
 *
 *  - flip their profile between public and private,
 *  - review and accept/deny incoming follow requests (only relevant while
 *    private, but we still show stale pending ones if they switch back).
 *
 * Stays consistent with the rest of the codebase by exposing a single
 * `StateFlow<ProfileUiState>` and refreshing on demand rather than relying on
 * Firestore snapshot listeners. The screen calls [refresh] on entry so the
 * follow-request count is always fresh after the user navigates back.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = ProfileUiState.Error("Not signed in")
            return
        }
        viewModelScope.launch {
            try {
                val user = repository.getUser(userId)
                    ?: throw IllegalStateException("User document not found")
                val pending = repository.getPendingFollowRequests(userId)
                _uiState.value = ProfileUiState.Ready(
                    user = user,
                    pendingRequests = pending,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load profile", e)
                _uiState.value = ProfileUiState.Error(
                    e.message ?: "Failed to load profile"
                )
            }
        }
    }

    /**
     * Toggle the user's profile between public and private. Optimistically
     * updates UI state then persists; on failure rolls back and surfaces an
     * error so the toggle never lies about what the server actually accepted.
     */
    fun setProfilePublic(isPublic: Boolean) {
        val current = _uiState.value as? ProfileUiState.Ready ?: return
        if (current.user.isProfilePublic == isPublic || current.visibilityUpdating) return

        val previous = current.user.isProfilePublic
        _uiState.value = current.copy(
            user = current.user.copy(isProfilePublic = isPublic),
            visibilityUpdating = true,
        )

        viewModelScope.launch {
            try {
                repository.updateProfileVisibility(current.user.id, isPublic)
                _uiState.update { state ->
                    (state as? ProfileUiState.Ready)?.copy(visibilityUpdating = false) ?: state
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update profile visibility", e)
                _uiState.update { state ->
                    (state as? ProfileUiState.Ready)?.copy(
                        user = state.user.copy(isProfilePublic = previous),
                        visibilityUpdating = false,
                    ) ?: state
                }
            }
        }
    }

    fun acceptRequest(request: FollowRequest) {
        mutateRequest(request) { repository.acceptFollowRequest(request) }
    }

    fun denyRequest(request: FollowRequest) {
        mutateRequest(request) { repository.denyFollowRequest(request.id) }
    }

    /**
     * Shared accept/deny plumbing: mark the request as in-flight, run the
     * Firestore mutation, then drop the request from the list on success
     * (or just clear the in-flight flag on failure so the user can retry).
     */
    private inline fun mutateRequest(
        request: FollowRequest,
        crossinline action: suspend () -> Unit,
    ) {
        val current = _uiState.value as? ProfileUiState.Ready ?: return
        if (request.id in current.processingRequestIds) return

        _uiState.value = current.copy(
            processingRequestIds = current.processingRequestIds + request.id,
        )

        viewModelScope.launch {
            try {
                action()
                _uiState.update { state ->
                    (state as? ProfileUiState.Ready)?.copy(
                        pendingRequests = state.pendingRequests.filterNot { it.id == request.id },
                        processingRequestIds = state.processingRequestIds - request.id,
                    ) ?: state
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle follow request ${request.id}", e)
                _uiState.update { state ->
                    (state as? ProfileUiState.Ready)?.copy(
                        processingRequestIds = state.processingRequestIds - request.id,
                    ) ?: state
                }
            }
        }
    }
}
