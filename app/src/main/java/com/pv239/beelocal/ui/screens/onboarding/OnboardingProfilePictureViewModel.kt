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

/**
 * UI state for the post-registration "pick a profile picture" onboarding screen.
 *
 * The screen is a single-step form: the user can optionally pick an image from
 * their gallery and either upload it (Continue) or skip the step.
 */
data class OnboardingProfilePictureUiState(
    /** Username loaded from Firestore (used to render the initials fallback). */
    val username: String = "",
    /** Locally-selected image URI shown as a preview before upload. */
    val selectedImageUri: Uri? = null,
    /** True while the image is being uploaded to Firebase Storage. */
    val isUploading: Boolean = false,
    /** Error to surface to the user; cleared on retry. */
    val errorMessage: String? = null,
)

sealed interface OnboardingProfilePictureEvent {
    /** Onboarding finished — either the picture was saved or the user skipped. */
    data object Finished : OnboardingProfilePictureEvent
}

@HiltViewModel
class OnboardingProfilePictureViewModel @Inject constructor(
    application: Application,
    private val firestoreRepository: FirestoreRepository,
    private val storageRepository: StorageRepository,
    private val auth: FirebaseAuth,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OnboardingProfilePictureUiState())
    val uiState: StateFlow<OnboardingProfilePictureUiState> = _uiState.asStateFlow()

    private val _events = Channel<OnboardingProfilePictureEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadUsername()
    }

    private fun loadUsername() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching { firestoreRepository.getUser(userId) }
                .onSuccess { user ->
                    if (user != null) {
                        _uiState.update { it.copy(username = user.username) }
                    }
                }
                .onFailure {
                    Log.w("OnboardingProfileVM", "Failed to load user", it)
                }
        }
    }

    fun onImageSelected(uri: Uri?) {
        // null happens when the user dismisses the picker without choosing
        // anything — leave existing selection untouched in that case.
        if (uri == null) return
        _uiState.update { it.copy(selectedImageUri = uri, errorMessage = null) }
    }

    /**
     * Skip uploading a picture. The user document keeps its default null
     * `profileImageUrl`, so the initials avatar fallback will be used app-wide.
     */
    fun skip() {
        viewModelScope.launch { _events.send(OnboardingProfilePictureEvent.Finished) }
    }

    /**
     * Upload the selected image to Firebase Storage under
     * `users-content/<uid>/<uuid>.jpg` and persist the resulting download URL
     * on the user document. Mirrors the pattern used in
     * `DailyChallengeViewModel.submitPhoto` (including cleanup of orphaned
     * blobs on Firestore-write failure).
     *
     * If no image is selected this behaves like [skip].
     */
    fun uploadAndContinue() {
        val state = _uiState.value
        val uri = state.selectedImageUri
        if (uri == null) {
            skip()
            return
        }
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.update { it.copy(errorMessage = "Not signed in.") }
            return
        }

        _uiState.update { it.copy(isUploading = true, errorMessage = null) }

        viewModelScope.launch {
            var uploadResult: StorageRepository.UploadResult? = null
            try {
                uploadResult = storageRepository.uploadUserImage(
                    context = getApplication(),
                    imageUri = uri,
                    userId = userId,
                )

                firestoreRepository.updateUserProfileImage(
                    userId = userId,
                    profileImageUrl = uploadResult.downloadUrl,
                    profileImageId = uploadResult.imageId,
                )

                _uiState.update { it.copy(isUploading = false) }
                _events.send(OnboardingProfilePictureEvent.Finished)
            } catch (e: Exception) {
                Log.e("OnboardingProfileVM", "Failed to upload profile picture", e)
                // Clean up an orphaned blob if the Firestore write failed after
                // the upload itself succeeded.
                uploadResult?.let { result ->
                    runCatching {
                        storageRepository.deleteUserImage(userId, result.imageId)
                    }.onFailure { ex ->
                        Log.w(
                            "OnboardingProfileVM",
                            "Failed to delete orphaned profile image",
                            ex,
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        errorMessage = e.message ?: "Failed to upload picture.",
                    )
                }
            }
        }
    }
}
