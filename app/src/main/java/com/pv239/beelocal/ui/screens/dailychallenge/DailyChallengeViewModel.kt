package com.pv239.beelocal.ui.screens.dailychallenge

import android.app.Application
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.pv239.beelocal.domain.FirestoreRepository
import com.pv239.beelocal.domain.StorageRepository
import com.pv239.beelocal.domain.XpRewards
import com.pv239.beelocal.model.DailyChallengeCompletion
import com.pv239.beelocal.model.DailyChallengeHints
import com.pv239.beelocal.model.FeedEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    application: Application,
    private val repository: FirestoreRepository,
    private val storageRepository: StorageRepository,
    private val auth: FirebaseAuth,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<DailyChallengeUiState>(DailyChallengeUiState.Loading)
    val uiState: StateFlow<DailyChallengeUiState> = _uiState.asStateFlow()

    init {
        loadChallenge()
    }

    // ---------------------------------------------------------------------------
    // Load
    // ---------------------------------------------------------------------------

    private fun loadChallenge() {
        viewModelScope.launch {
            try {
                // Build a Timestamp for the start of today (UTC midnight)
                val todayMidnight =
                    LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant()
                val todayTimestamp = Timestamp(Date.from(todayMidnight))

                val challenge = repository.getDailyChallenge(todayTimestamp) ?: run {
                    _uiState.value = DailyChallengeUiState.NoChallengeToday
                    return@launch
                }

                val userId = auth.currentUser?.uid

                // Hints + completion are independent fetches; both can be absent.
                val hints: DailyChallengeHints = if (userId != null) {
                    repository.getDailyChallengeHints(userId, challenge.id)
                        ?: DailyChallengeHints(id = challenge.id)
                } else {
                    DailyChallengeHints(id = challenge.id)
                }

                val statistics = userId?.let { repository.getStatistics(it) }
                val currentXp = statistics?.xp ?: 0

                val completion: CompletionState = if (userId != null) {
                    val record = repository.getDailyChallengeCompletion(userId, challenge.id)
                    if (record != null) {
                        CompletionState.Completed(
                            imageId = record.imageId,
                            photoUrl = record.photoUrl,
                            streakCount = statistics?.streak ?: 1,
                            sharedToFeed = record.sharedToFeed,
                            // The XP for past submissions isn't stored anywhere;
                            // we only show the awarded amount for in-session submits.
                            xpAwarded = 0,
                        )
                    } else {
                        CompletionState.NotCompleted
                    }
                } else {
                    CompletionState.NotCompleted
                }

                _uiState.value = DailyChallengeUiState.Ready(
                    challenge = challenge,
                    secondsRemaining = secondsUntilMidnight(),
                    distanceMeters = null,
                    completion = completion,
                    directionUnlocked = hints.directionUnlocked,
                    mapUnlocked = hints.mapUnlocked,
                    currentXp = currentXp,
                )
            } catch (e: Exception) {
                _uiState.value = DailyChallengeUiState.Error(
                    e.message ?: "Failed to load today's challenge"
                )
            }
        }
    }

    fun onLocationUpdate(location: Location) {
        val current = _uiState.value as? DailyChallengeUiState.Ready ?: return
        val target = current.challenge.location
        val targetLocation = Location("").apply {
            latitude = target.latitude
            longitude = target.longitude
        }
        val distanceMeters = location.distanceTo(targetLocation).roundToInt()
        // bearingTo returns degrees in [-180, 180]; normalise to [0, 360) so the
        // UI can rotate a compass arrow without sign handling.
        val rawBearing = location.bearingTo(targetLocation)
        val bearing = ((rawBearing % 360f) + 360f) % 360f
        _uiState.update { state ->
            (state as? DailyChallengeUiState.Ready)?.copy(
                distanceMeters = distanceMeters,
                userLatLng = Pair(location.latitude, location.longitude),
                bearingDegrees = bearing,
            ) ?: state
        }
    }

    // ---------------------------------------------------------------------------
    // Hint unlocks
    // ---------------------------------------------------------------------------

    fun unlockDirectionHint() = unlockHint(HintKind.DIRECTION)
    fun unlockMapHint() = unlockHint(HintKind.MAP)

    private fun unlockHint(kind: HintKind) {
        val current = _uiState.value as? DailyChallengeUiState.Ready ?: return
        if (current.hintUnlockInFlight != null) return
        // The hint cost is paid out of the eventual completion reward, not
        // out of the user's current XP balance — so no affordability check
        // is required. We still bail out client-side on already-unlocked to
        // avoid a pointless round-trip.
        val alreadyUnlocked = when (kind) {
            HintKind.DIRECTION -> current.directionUnlocked
            HintKind.MAP -> current.mapUnlocked
        }
        if (alreadyUnlocked) return

        val userId: String = checkNotNull(auth.currentUser?.uid) {
            "unlockHint called without an authenticated user"
        }
        val hintField = when (kind) {
            HintKind.DIRECTION -> "directionUnlocked"
            HintKind.MAP -> "mapUnlocked"
        }

        _uiState.update { state ->
            (state as? DailyChallengeUiState.Ready)?.copy(
                hintUnlockInFlight = kind,
                hintUnlockError = null,
            ) ?: state
        }

        viewModelScope.launch {
            try {
                val result = repository.unlockDailyChallengeHint(
                    userId = userId,
                    challengeId = current.challenge.id,
                    hintField = hintField,
                )
                _uiState.update { state ->
                    val ready = state as? DailyChallengeUiState.Ready ?: return@update state
                    when (result) {
                        is FirestoreRepository.HintUnlockResult.Unlocked -> ready.copy(
                            directionUnlocked = result.hints.directionUnlocked,
                            mapUnlocked = result.hints.mapUnlocked,
                            hintUnlockInFlight = null,
                            hintUnlockError = null,
                        )

                        is FirestoreRepository.HintUnlockResult.AlreadyUnlocked -> ready.copy(
                            directionUnlocked = result.hints.directionUnlocked,
                            mapUnlocked = result.hints.mapUnlocked,
                            hintUnlockInFlight = null,
                            hintUnlockError = null,
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DailyChallengeViewModel", "Failed to unlock hint $kind", e)
                _uiState.update { state ->
                    (state as? DailyChallengeUiState.Ready)?.copy(
                        hintUnlockInFlight = null,
                        hintUnlockError = e.message ?: "Failed to unlock hint",
                    ) ?: state
                }
            }
        }
    }

    fun submitPhoto(photoUri: Uri, userStreak: Int) {
        val current = _uiState.value as? DailyChallengeUiState.Ready ?: return
        // Only allow submission while we're in the NotCompleted state so retries
        // don't run on top of an in-flight or already-completed submission.
        if (current.completion !is CompletionState.NotCompleted &&
            current.completion !is CompletionState.SubmissionFailed
        ) return
        // submitPhoto is only reachable from authenticated UI (the FAB is
        // hidden/disabled otherwise), so a missing uid here is a programmer
        // error rather than a recoverable runtime state.
        val userId: String = checkNotNull(auth.currentUser?.uid) {
            "submitPhoto called without an authenticated user"
        }

        _uiState.update { state ->
            (state as? DailyChallengeUiState.Ready)?.copy(
                completion = CompletionState.Submitting
            ) ?: state
        }

        viewModelScope.launch {
            var uploadResult: StorageRepository.UploadResult? = null
            try {
                Log.d("UPLOAD_DEBUG", "Uploading photo from URI: $photoUri")

                // 1) Upload the photo to users-content/<userId>/<uuid>.jpg
                uploadResult = storageRepository.uploadUserImage(
                    context = getApplication(),
                    imageUri = photoUri,
                    userId = userId,
                )

                val newStreak = userStreak + 1
                val completion = DailyChallengeCompletion(
                    challengeId = current.challenge.id,
                    userId = userId,
                    imageId = uploadResult.imageId,
                    photoUrl = uploadResult.downloadUrl,
                    completedAt = Timestamp.now(),
                    sharedToFeed = false,
                )

                // XP reward is computed from the count of paid hints the user
                // unlocked — read from the freshest UI state we have.
                val hintsUnlocked = (_uiState.value as? DailyChallengeUiState.Ready)
                    ?.hintsUnlockedCount ?: current.hintsUnlockedCount
                val xpReward = XpRewards.dailyChallengeReward(hintsUnlocked)

                // 2) Atomically write the completion + streak + XP. If a
                //    completion already exists, `created` will be false and
                //    nothing was written — in that case we must clean up the
                //    orphaned blob and refresh from server-state instead of
                //    pretending we succeeded.
                val created = repository.submitDailyChallenge(completion, newStreak, xpReward)
                if (!created) {
                    runCatching {
                        storageRepository.deleteUserImage(userId, uploadResult.imageId)
                    }.onFailure { ex ->
                        Log.w(
                            "DailyChallengeViewModel",
                            "Failed to delete orphaned image after no-op submit",
                            ex,
                        )
                    }
                    loadChallenge()
                    return@launch
                }

                _uiState.update { state ->
                    (state as? DailyChallengeUiState.Ready)?.copy(
                        completion = CompletionState.Completed(
                            imageId = uploadResult.imageId,
                            photoUrl = uploadResult.downloadUrl,
                            streakCount = newStreak,
                            sharedToFeed = false,
                            xpAwarded = xpReward,
                        ),
                        currentXp = state.currentXp + xpReward,
                    ) ?: state
                }
            } catch (e: Exception) {
                Log.e("DailyChallengeViewModel", "Failed to submit photo", e)
                // Compensate for a successful upload that wasn't followed by a
                // successful Firestore write — otherwise the blob is orphaned.
                uploadResult?.let { result ->
                    runCatching {
                        storageRepository.deleteUserImage(userId, result.imageId)
                    }.onFailure { ex ->
                        Log.w(
                            "DailyChallengeViewModel",
                            "Failed to delete orphaned image after submit failure",
                            ex,
                        )
                    }
                }
                _uiState.update { state ->
                    (state as? DailyChallengeUiState.Ready)?.copy(
                        completion = CompletionState.SubmissionFailed(
                            errorMessage = e.message ?: "Failed to submit photo"
                        )
                    ) ?: state
                }
            } finally {
                // Clean up temp camera files
                cleanUpCameraPhotos()
            }
        }
    }

    /**
     * Report a failure that happened before the photo was even submitted
     * (e.g. temp-file creation or camera intent launch threw). Surfaces the
     * error using the existing `SubmissionFailed` state so the UI shows an
     * error message and the FAB switches to "Retry".
     */
    fun reportCameraError(message: String) {
        Log.e("DailyChallengeViewModel", "Camera flow failed: $message")
        _uiState.update { state ->
            (state as? DailyChallengeUiState.Ready)?.let { ready ->
                // Don't clobber a successful submission with a transient camera error.
                if (ready.completion is CompletionState.Completed) ready
                else ready.copy(
                    completion = CompletionState.SubmissionFailed(errorMessage = message)
                )
            } ?: state
        }
    }

    fun shareToFeed() {
        val current = _uiState.value as? DailyChallengeUiState.Ready ?: return
        val completed = current.completion as? CompletionState.Completed ?: return
        // shareToFeed is only reachable after a successful submission, so the
        // user must already be authenticated by the time we get here.
        val userId: String = checkNotNull(auth.currentUser?.uid) {
            "shareToFeed called without an authenticated user"
        }

        viewModelScope.launch {
            try {
                val entry = FeedEntry(
                    userId = userId,
                    challengeId = current.challenge.id,
                    imageId = completed.imageId,
                    imageUrl = completed.photoUrl,
                    timestamp = Timestamp.now(),
                )
                repository.shareChallengeToFeed(userId, current.challenge.id, entry)

                _uiState.update { state ->
                    (state as? DailyChallengeUiState.Ready)?.copy(
                        completion = completed.copy(sharedToFeed = true)
                    ) ?: state
                }
            } catch (_: Exception) {
                Log.e("DailyChallengeViewModel", "Failed to share to feed")
            }
        }
    }

    /** Deletes all temp files in the camera_photos cache directory. */
    private fun cleanUpCameraPhotos() {
        try {
            val cameraDir = java.io.File(getApplication<Application>().cacheDir, "camera_photos")
            cameraDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.w("DailyChallengeViewModel", "Failed to clean up temp camera files", e)
        }
    }

    private fun secondsUntilMidnight(): Long {
        val now = System.currentTimeMillis()
        val midnight =
            LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                .toEpochMilli()
        return (midnight - now) / 1000
    }
}
