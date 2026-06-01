package com.pv239.beelocal.ui.screens.onboarding

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pv239.beelocal.domain.FirestoreRepository
import com.pv239.beelocal.domain.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "OnboardingPicVM"

/**
 * Drives the onboarding "pick your first profile picture" step: stages a
 * locally selected image, uploads it to Cloud Storage on confirm, persists
 * the resulting download URL on the user document, and emits a [Finished]
 * event so the navigation graph can move the user on.
 *
 * Skipping is allowed — onboarding shouldn't gate the rest of the app, so
 * users with no picture yet can simply continue and edit it later from the
 * profile screen (which reuses the same upload plumbing in `ProfileViewModel`).
 */
@HiltViewModel
class OnboardingProfilePictureViewModel @Inject constructor(
    application: Application,
    private val repository: FirestoreRepository,
    private val storageRepository: StorageRepository,
    private val auth: FirebaseAuth,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OnboardingProfilePictureUiState())
    val uiState: StateFlow<OnboardingProfilePictureUiState> = _uiState.asStateFlow()

    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Update the locally previewed image (does not yet upload). */
    fun onImageSelected(uri: Uri) {
        _uiState.update { it.copy(pendingUri = uri, errorMessage = null) }
    }

    /** Continue without picking a picture — leaves `profileImageUrl` null. */
    fun skip() {
        if (_uiState.value.isLoading) return
        if (auth.currentUser == null) return
        viewModelScope.launch { _events.send(OnboardingEvent.Finished) }
    }

    /**
     * Upload the staged image (if any) and persist its URL on the user
     * document. If nothing was staged this behaves like [skip].
     */
    fun confirm() {
        val state = _uiState.value
        if (state.isLoading) return

        val uri = state.pendingUri
        if (uri == null) {
            skip()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.update { it.copy(errorMessage = "Not signed in") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            var uploadResult: StorageRepository.UploadResult? = null
            try {
                uploadResult = storageRepository.uploadUserImage(
                    context = getApplication(),
                    imageUri = uri,
                    userId = userId,
                )
                repository.updateProfileImage(userId, uploadResult.downloadUrl)
                _uiState.update { it.copy(isLoading = false) }
                _events.send(OnboardingEvent.Finished)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload onboarding profile picture", e)
                uploadResult?.let { result ->
                    runCatching {
                        storageRepository.deleteUserImage(userId, result.imageId)
                    }.onFailure { ex ->
                        Log.w(TAG, "Failed to delete orphaned onboarding upload", ex)
                    }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to upload picture",
                    )
                }
            }
        }
    }
}

data class OnboardingProfilePictureUiState(
    val pendingUri: Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface OnboardingEvent {
    data object Finished : OnboardingEvent
}
