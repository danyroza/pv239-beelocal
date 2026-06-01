package com.pv239.beelocal.ui.screens.bingo

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.pv239.beelocal.R
import com.pv239.beelocal.domain.FirestoreRepository
import com.pv239.beelocal.domain.FirestoreRepository.BingoCompletionResult
import com.pv239.beelocal.domain.StorageRepository
import com.pv239.beelocal.model.BingoTaskCompletion
import com.pv239.beelocal.model.FeedEntry
import com.pv239.beelocal.model.UserBingoProgress
import com.pv239.beelocal.model.types.FeedEntryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BingoViewModel @Inject constructor(
    application: Application,
    private val repository: FirestoreRepository,
    private val storageRepository: StorageRepository,
    private val auth: FirebaseAuth,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<BingoUiState>(BingoUiState.Loading)
    val uiState: StateFlow<BingoUiState> = _uiState.asStateFlow()

    init {
        loadBingoCard()
    }

    private fun loadBingoCard() {
        viewModelScope.launch {
            try {
                val card = repository.getCurrentBingoCard() ?: run {
                    _uiState.value = BingoUiState.NoCardAvailable
                    return@launch
                }

                // Pad or truncate to exactly 16 tasks so the grid is always 4×4.
                // If a task has no id set in Firestore (embedded array objects don't
                // get @DocumentId), assign a stable index-based id so each cell is
                // uniquely and consistently trackable across sessions.
                val tasks = card.tasks.take(16).mapIndexed { index, task ->
                    if (task.id.isBlank()) task.copy(id = "task_$index") else task
                }
                if (tasks.size != 16) {
                    _uiState.value =
                        BingoUiState.Error(getString(R.string.bingo_error_card_misconfigured))
                    return@launch
                }
                val normalizedCard = card.copy(tasks = tasks)

                val userId = auth.currentUser?.uid ?: run {
                    _uiState.value =
                        BingoUiState.Error(getString(R.string.bingo_error_sign_in_required))
                    return@launch
                }

                val progress = repository.getUserBingoProgress(userId, card.id)
                val completedIds =
                    progress?.completedTaskIds?.toSet() ?: emptySet()

                val photoUrls =
                    repository.getBingoTaskCompletions(userId, card.id)
                        .mapNotNull { c -> c.photoUrl?.let { c.taskId to it } }
                        .toMap()

                _uiState.value = BingoUiState.Ready(
                    card = normalizedCard,
                    completedTaskIds = completedIds,
                    completedTaskPhotoUrls = photoUrls,
                    bingoLines = detectBingoLines(
                        completedIds,
                        tasks.map { it.id }),
                    sharedToFeed = progress?.sharedToFeed ?: false,
                )
            } catch (e: Exception) {
                Log.e("BingoViewModel", "Failed to load bingo card", e)
                _uiState.value =
                    BingoUiState.Error(e.message ?: getString(R.string.bingo_error_load_failed))
            }
        }
    }

    fun onPhotoTaken(taskId: String, photoUri: Uri) {
        val current = _uiState.value as? BingoUiState.Ready ?: return
        if (current.isCompleted(taskId) || current.submittingTaskId != null) return

        val userId = auth.currentUser?.uid ?: run {
            _uiState.value =
                BingoUiState.Error(getString(R.string.bingo_error_sign_in_required))
            return
        }

        _uiState.update { state ->
            (state as? BingoUiState.Ready)?.copy(submittingTaskId = taskId)
                ?: state
        }

        viewModelScope.launch {
            var uploadResult: StorageRepository.UploadResult? = null
            try {
                uploadResult = storageRepository.uploadUserImage(
                    context = getApplication(),
                    imageUri = photoUri,
                    userId = userId,
                )

                val task = current.card.tasks.find { it.id == taskId }
                val completion = BingoTaskCompletion(
                    userId = userId,
                    bingoCardId = current.card.id,
                    taskId = taskId,
                    taskTitle = task?.title ?: "",
                    photoUrl = uploadResult.downloadUrl,
                    completedAt = Timestamp.now(),
                )

                val progress = UserBingoProgress(
                    userId = userId,
                    bingoCardId = current.card.id,
                    completedTaskIds = current.completedTaskIds.toList(),
                    sharedToFeed = current.sharedToFeed,
                )

                val result = repository.completeBingoTask(progress, completion)
                if (result is BingoCompletionResult.AlreadyDone) {
                    uploadResult.let { stored ->
                        runCatching {
                            storageRepository.deleteUserImage(
                                userId,
                                stored.imageId
                            )
                        }.onFailure { ex ->
                            Log.w(
                                "BingoViewModel",
                                "Failed to delete duplicate upload",
                                ex
                            )
                        }
                    }
                    // Already completed server-side — reload to sync
                    loadBingoCard()
                    return@launch
                }

                val awarded = result as BingoCompletionResult.Awarded

                val newCompletedIds = current.completedTaskIds + taskId
                val taskIds = current.card.tasks.map { it.id }
                val newLines = detectBingoLines(newCompletedIds, taskIds)
                val newBingo = newLines.size > current.bingoLines.size
                val newPhotoUrls =
                    current.completedTaskPhotoUrls + (taskId to uploadResult.downloadUrl)

                _uiState.update { state ->
                    (state as? BingoUiState.Ready)?.copy(
                        completedTaskIds = newCompletedIds,
                        completedTaskPhotoUrls = newPhotoUrls,
                        submittingTaskId = null,
                        bingoLines = newLines,
                        showBingoCelebration = newBingo || awarded.cardCompleted,
                        lastXpReward = awarded.totalXp,
                        cardJustCompleted = awarded.cardCompleted,
                    ) ?: state
                }
            } catch (e: Exception) {
                Log.e(
                    "BingoViewModel",
                    "Failed to complete bingo task $taskId",
                    e
                )
                uploadResult?.let { result ->
                    runCatching {
                        storageRepository.deleteUserImage(
                            userId,
                            result.imageId
                        )
                    }.onFailure { ex ->
                        Log.w(
                            "BingoViewModel",
                            "Failed to delete orphaned image",
                            ex
                        )
                    }
                }
                _uiState.update { state ->
                    (state as? BingoUiState.Ready)?.copy(submittingTaskId = null)
                        ?: state
                }
            } finally {
                cleanUpCameraPhotos()
            }
        }
    }

    fun dismissBingoCelebration() {
        _uiState.update { state ->
            (state as? BingoUiState.Ready)?.copy(
                showBingoCelebration = false,
                cardJustCompleted = false,
            ) ?: state
        }
    }

    /**
     * Clears the most recent XP-award toast so it doesn't re-show across
     * configuration changes / recompositions after the user has acknowledged it.
     */
    fun dismissXpAward() {
        _uiState.update { state ->
            (state as? BingoUiState.Ready)?.copy(lastXpReward = null)
                ?: state
        }
    }

    fun reportCameraError(message: String) {
        Log.e("BingoViewModel", "Camera flow failed: $message")
    }

    fun showShareDialog() {
        _uiState.update { state ->
            (state as? BingoUiState.Ready)?.copy(showShareDialog = true)
                ?: state
        }
    }

    fun dismissShareDialog() {
        _uiState.update { state ->
            (state as? BingoUiState.Ready)?.copy(showShareDialog = false)
                ?: state
        }
    }

    fun shareToFeed(description: String, selectedPhotoUrls: List<String>) {
        val current = _uiState.value as? BingoUiState.Ready ?: return
        if (current.sharedToFeed) return
        val userId = checkNotNull(auth.currentUser?.uid) {
            "shareToFeed called without an authenticated user"
        }
        _uiState.update { state ->
            (state as? BingoUiState.Ready)?.copy(showShareDialog = false)
                ?: state
        }
        viewModelScope.launch {
            try {
                val entry = FeedEntry(
                    userId = userId,
                    type = FeedEntryType.BINGO_COMPLETED,
                    imageId = "",
                    imageUrl = selectedPhotoUrls.firstOrNull() ?: "",
                    imageUrls = selectedPhotoUrls,
                    timestamp = Timestamp.now(),
                    bingoCardId = current.card.id,
                    description = description,
                )
                repository.shareBingoToFeed(userId, current.card.id, entry)
                _uiState.update { state ->
                    (state as? BingoUiState.Ready)?.copy(sharedToFeed = true)
                        ?: state
                }
            } catch (e: Exception) {
                Log.e("BingoViewModel", "Failed to share bingo to feed", e)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Bingo-line detection for a 4×4 grid
    // -------------------------------------------------------------------------

    /**
     * Returns the set of completed line indices:
     *  - 0..3  → rows 0-3
     *  - 4..7  → columns 0-3
     *  - 8     → main diagonal (top-left → bottom-right)
     *  - 9     → anti-diagonal (top-right → bottom-left)
     */
    private fun detectBingoLines(
        completedIds: Set<String>,
        taskIds: List<String>,
    ): Set<Int> {
        if (taskIds.size < 16) return emptySet()
        // If any task still has a blank id, detection would incorrectly fire all
        // lines for a single completion — bail out early as a safety net.
        if (taskIds.any { it.isBlank() }) return emptySet()

        fun id(row: Int, col: Int) = taskIds[row * 4 + col]
        fun done(row: Int, col: Int) = id(row, col) in completedIds

        val lines = mutableSetOf<Int>()

        // Rows
        for (r in 0..3) if ((0..3).all { c -> done(r, c) }) lines += r
        // Columns
        for (c in 0..3) if ((0..3).all { r -> done(r, c) }) lines += 4 + c
        // Main diagonal
        if ((0..3).all { i -> done(i, i) }) lines += 8
        // Anti-diagonal
        if ((0..3).all { i -> done(i, 3 - i) }) lines += 9

        return lines
    }

    /** Deletes all temp files in the camera_photos cache directory. */
    private fun cleanUpCameraPhotos() {
        runCatching {
            val cacheDir =
                File(getApplication<Application>().cacheDir, "camera_photos")
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
}
