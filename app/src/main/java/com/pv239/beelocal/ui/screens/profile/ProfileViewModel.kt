package com.pv239.beelocal.ui.screens.profile

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pv239.beelocal.data.repository.AuthRepository
import com.pv239.beelocal.domain.FirestoreRepository
import com.pv239.beelocal.domain.StorageRepository
import com.pv239.beelocal.model.FollowRequest
import com.pv239.beelocal.model.UserStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
 *  - upload / replace their profile picture,
 *  - review and accept/deny incoming follow requests (only relevant while
 *    private, but we still show stale pending ones if they switch back).
 *
 * The user document and statistics document are tracked via Firestore
 * snapshot listeners so that XP / streak / friends-list changes made
 * elsewhere in the app (daily challenge submissions, bingo completions,
 * hint unlocks) are reflected on the profile screen immediately, without
 * the user having to navigate away and back.
 *
 * Follow requests use a separate one-shot query since they don't need to
 * react to every individual mutation — we refresh them on screen entry and
 * after accept/deny.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    application: Application,
    private val repository: FirestoreRepository,
    private val storageRepository: StorageRepository,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        startObserving()
    }

    /**
     * Re-fetch pending follow requests. Called on screen entry, after
     * accept/deny, and from manual refresh actions if any are wired in.
     * User + statistics data is delivered automatically by the snapshot
     * listeners started in [startObserving].
     */
    fun refresh() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val pending = repository.getPendingFollowRequests(userId)
                _uiState.update { state ->
                    (state as? ProfileUiState.Ready)?.copy(pendingRequests = pending) ?: state
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh follow requests", e)
            }
        }
    }

    /**
     * Attaches snapshot listeners to the user + statistics documents. Each
     * emission updates [uiState] in place so the screen never has to manually
     * reload after writes triggered elsewhere (e.g. an XP award from the
     * daily challenge view model).
     */
    /**
     * Cached latest [UserStatistics] from the snapshot listener. Holding this
     * outside [_uiState] means a statistics emission that arrives **before**
     * the user document's first emission isn't dropped on the floor; we
     * apply it as soon as the user listener promotes the state to Ready.
     */
    private val cachedStatistics = MutableStateFlow<UserStatistics?>(null)

    private fun startObserving() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = ProfileUiState.Error("Not signed in")
            return
        }

        observeJob?.cancel()
        cachedStatistics.value = null
        observeJob = viewModelScope.launch {
            try {
                // Kick off an immediate fetch of pending follow requests so the
                // ready state has something to show before any listener fires.
                val initialPending = runCatching {
                    repository.getPendingFollowRequests(userId)
                }.getOrDefault(emptyList())

                // We launch two collectors in parallel; either source can fire
                // first depending on Firestore latency. The user collector is
                // responsible for promoting Loading → Ready; the statistics
                // collector pipes through `cachedStatistics` so any stats
                // emitted before the first user emission are preserved.
                launch {
                    repository.observeUser(userId).collect { user ->
                        if (user == null) {
                            _uiState.value =
                                ProfileUiState.Error("User document not found")
                            return@collect
                        }
                        _uiState.update { state ->
                            when (state) {
                                is ProfileUiState.Ready -> state.copy(user = user)
                                else -> ProfileUiState.Ready(
                                    user = user,
                                    statistics = cachedStatistics.value
                                        ?: UserStatistics(userId = userId),
                                    pendingRequests = initialPending,
                                )
                            }
                        }
                    }
                }
                launch {
                    repository.observeStatistics(userId).collect { stats ->
                        val resolved = stats ?: UserStatistics(userId = userId)
                        cachedStatistics.value = resolved
                        _uiState.update { state ->
                            when (state) {
                                is ProfileUiState.Ready -> state.copy(statistics = resolved)
                                // Stats arrived before the user document — keep the
                                // current state (Loading) and let the user listener
                                // pick up the cached value when it promotes to Ready.
                                else -> state
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start profile observers", e)
                _uiState.value = ProfileUiState.Error(
                    e.message ?: "Failed to load profile"
                )
            }
        }
    }

    /**
     * Toggle the user's profile between public and private. Optimistically
     * updates UI state then persists; on failure the live user listener will
     * re-emit the original value, so we just clear the in-flight flag.
     */
    fun setProfilePublic(isPublic: Boolean) {
        val current = _uiState.value as? ProfileUiState.Ready ?: return
        if (current.user.isProfilePublic == isPublic || current.visibilityUpdating) return

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
                // The observeUser listener will re-emit the server-authoritative
                // value shortly; just drop the in-flight flag here.
                _uiState.update { state ->
                    (state as? ProfileUiState.Ready)?.copy(visibilityUpdating = false) ?: state
                }
            }
        }
    }

    /**
     * Upload a new profile picture from a content [Uri] (typically returned by
     * the photo picker) and persist its download URL on the user document.
     *
     * Flow:
     *  1) Mark the UI as `pictureUploading` so the avatar shows a spinner.
     *  2) Upload the file to Cloud Storage under `users-content/<uid>/<uuid>.jpg`.
     *  3) Update `profileImageUrl` on the user's Firestore document.
     *  4) Delete the previous blob (if any) so we don't accumulate orphans
     *     in storage when users replace their avatar.
     *  5) On failure of the upload/persist step, delete the freshly uploaded
     *     blob and surface an error message.
     */
    fun uploadProfilePicture(photoUri: Uri) {
        val current = _uiState.value as? ProfileUiState.Ready ?: return
        if (current.pictureUploading) return

        val userId = current.user.id
        val previousImageUrl = current.user.profileImageUrl
        _uiState.value = current.copy(
            pictureUploading = true,
            pictureUploadError = null,
        )

        viewModelScope.launch {
            var uploadResult: StorageRepository.UploadResult? = null
            try {
                uploadResult = storageRepository.uploadUserImage(
                    context = getApplication(),
                    imageUri = photoUri,
                    userId = userId,
                )
                repository.updateProfileImage(userId, uploadResult.downloadUrl)

                // Best-effort cleanup of the previous avatar. We intentionally
                // do NOT roll back the new URL on failure — the user's profile
                // is already correctly pointing at the fresh blob; an orphaned
                // old blob is preferable to a broken avatar.
                if (!previousImageUrl.isNullOrBlank() &&
                    previousImageUrl != uploadResult.downloadUrl
                ) {
                    runCatching {
                        storageRepository.deleteByDownloadUrl(previousImageUrl)
                    }.onFailure { ex ->
                        Log.w(TAG, "Failed to delete previous profile picture", ex)
                    }
                }

                _uiState.update { state ->
                    (state as? ProfileUiState.Ready)?.copy(
                        pictureUploading = false,
                        pictureUploadError = null,
                    ) ?: state
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload profile picture", e)
                // Compensate for a successful upload that failed to persist on
                // the user document — otherwise the blob is orphaned.
                uploadResult?.let { result ->
                    runCatching {
                        storageRepository.deleteUserImage(userId, result.imageId)
                    }.onFailure { ex ->
                        Log.w(TAG, "Failed to delete orphaned profile picture", ex)
                    }
                }
                _uiState.update { state ->
                    (state as? ProfileUiState.Ready)?.copy(
                        pictureUploading = false,
                        pictureUploadError = e.message ?: "Failed to upload picture",
                    ) ?: state
                }
            }
        }
    }

    /**
     * Sign the current user out of Firebase Auth. The caller is responsible
     * for navigating away from authenticated screens once this returns.
     */
    fun signOut() {
        observeJob?.cancel()
        auth.signOut()
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


    // -------------------------------------------------------------------------
    // Change-password dialog
    // -------------------------------------------------------------------------

    fun openPasswordDialog() {
        val current = _uiState.value as? ProfileUiState.Ready ?: return
        _uiState.value = current.copy(passwordDialog = PasswordDialogState())
    }

    fun dismissPasswordDialog() {
        val current = _uiState.value as? ProfileUiState.Ready ?: return
        _uiState.value = current.copy(passwordDialog = null)
    }

    fun onCurrentPasswordChange(value: String) = updateDialog { it.copy(currentPassword = value, error = null) }
    fun onNewPasswordChange(value: String) = updateDialog { it.copy(newPassword = value, error = null) }
    fun onConfirmPasswordChange(value: String) = updateDialog { it.copy(confirmPassword = value, error = null) }

    fun submitPasswordChange() {
        val current = _uiState.value as? ProfileUiState.Ready ?: return
        val dialog = current.passwordDialog ?: return
        if (!dialog.isValid) return

        _uiState.value = current.copy(passwordDialog = dialog.copy(isLoading = true))

        viewModelScope.launch {
            runCatching {
                authRepository.changePassword(dialog.currentPassword, dialog.newPassword)
            }
                .onSuccess {
                    _uiState.update { state ->
                        (state as? ProfileUiState.Ready)?.copy(
                            passwordDialog = null,
                        ) ?: state
                    }
                }
                .onFailure { e ->
                    updateDialog {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to change password. Please try again.",
                        )
                    }
                }
        }
    }

    private fun updateDialog(transform: (PasswordDialogState) -> PasswordDialogState) {
        _uiState.update { state ->
            val loaded = state as? ProfileUiState.Ready ?: return@update state
            val dialog = loaded.passwordDialog ?: return@update state
            loaded.copy(passwordDialog = transform(dialog))
        }
    }

}
