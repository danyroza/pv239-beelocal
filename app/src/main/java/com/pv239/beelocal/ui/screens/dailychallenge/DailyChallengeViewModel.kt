package com.pv239.beelocal.ui.screens.dailychallenge

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.pv239.beelocal.domain.FirestoreRepository
import com.pv239.beelocal.model.DailyChallengeCompletion
import com.pv239.beelocal.model.FeedEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    application: Application,
    private val repository: FirestoreRepository,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
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
                val completion: CompletionState = if (userId != null) {
                    val record = repository.getDailyChallengeCompletion(userId, challenge.id)
                    if (record != null) {
                        val statistics = repository.getStatistics(userId)
                        CompletionState.Completed(
                            photoUrl = record.photoUrl,
                            streakCount = statistics?.streak ?: 1,
                            sharedToFeed = record.sharedToFeed,
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
        _uiState.update { state ->
            (state as? DailyChallengeUiState.Ready)?.copy(distanceMeters = distanceMeters) ?: state
        }
    }

    fun submitPhoto(bitmap: Bitmap, userStreak: Int) {
        val current = _uiState.value as? DailyChallengeUiState.Ready ?: return
        val userId = auth.currentUser?.uid ?: return

        _uiState.update { state ->
            (state as? DailyChallengeUiState.Ready)?.copy(
                completion = CompletionState.Submitting
            ) ?: state
        }

        viewModelScope.launch {
            try {
                Log.d("UPLOAD_DEBUG", "before")

                val photoUrl = uploadPhoto(
                    context = getApplication(),
                    bitmap = bitmap,
                    userId = userId,
                    challengeId = current.challenge.id,
                )

                val newStreak = userStreak + 1
                val completion = DailyChallengeCompletion(
                    challengeId = current.challenge.id,
                    userId = userId,
                    photoUrl = photoUrl,
                    completedAt = Timestamp.now(),
                    sharedToFeed = false,
                )
                repository.submitDailyChallenge(completion, newStreak)

                _uiState.update { state ->
                    (state as? DailyChallengeUiState.Ready)?.copy(
                        completion = CompletionState.Completed(
                            photoUrl = photoUrl,
                            streakCount = newStreak,
                            sharedToFeed = false,
                        )
                    ) ?: state
                }
            } catch (e: Exception) {
                Log.e("DailyChallengeViewModel", "Failed to submit photo", e)
                _uiState.update { state ->
                    (state as? DailyChallengeUiState.Ready)?.copy(
                        completion = CompletionState.NotCompleted
                    ) ?: state
                }
            }
        }
    }

    fun shareToFeed() {
        val current = _uiState.value as? DailyChallengeUiState.Ready ?: return
        val completed = current.completion as? CompletionState.Completed ?: return
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val entry = FeedEntry(
                    userId = userId,
                    challengeId = current.challenge.id,
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

    private suspend fun uploadPhoto(
        context: Context,
        bitmap: Bitmap,
        userId: String,
        challengeId: String,
    ): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val bytes = baos.toByteArray()

        val ref = storage.reference.child("daily_challenge_photos/$userId/$challengeId.jpg")

        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    private fun secondsUntilMidnight(): Long {
        val now = System.currentTimeMillis()
        val midnight =
            LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                .toEpochMilli()
        return (midnight - now) / 1000
    }
}

fun Long.toHoursMinutes(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    return "${h}h ${m}m"
}