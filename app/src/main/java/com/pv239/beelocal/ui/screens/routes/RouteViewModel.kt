package com.pv239.beelocal.ui.screens.routes

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pv239.beelocal.data.repository.RouteRepository
import com.pv239.beelocal.domain.FirestoreRepository
import com.pv239.beelocal.domain.StorageRepository
import com.pv239.beelocal.domain.XpRewards
import com.pv239.beelocal.model.FeedEntry
import com.pv239.beelocal.model.Route
import com.pv239.beelocal.model.RouteReview
import com.pv239.beelocal.model.types.FeedEntryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val storageRepository: StorageRepository,
    private val repository: FirestoreRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val newUserId = firebaseAuth.currentUser?.uid
        val previousUserId = _authUserId.value
        if (newUserId == previousUserId) return@AuthStateListener

        _authUserId.value = newUserId

        if (newUserId == null) {
            clearRouteListState()
        } else {
            loadRoutes()
        }
    }

    private val _authUserId = MutableStateFlow(auth.currentUser?.uid)
    val authUserId: StateFlow<String?> = _authUserId.asStateFlow()

    // ---------------------------------------------------------------------------
    // Routes list
    // ---------------------------------------------------------------------------

    private val _listState = MutableStateFlow(RoutesListUiState())
    val listState: StateFlow<RoutesListUiState> = _listState.asStateFlow()

    // ---------------------------------------------------------------------------
    // Route detail
    // ---------------------------------------------------------------------------

    private val _detailState = MutableStateFlow(RouteDetailUiState())
    val detailState: StateFlow<RouteDetailUiState> = _detailState.asStateFlow()

    // ---------------------------------------------------------------------------
    // Active journey
    // ---------------------------------------------------------------------------

    private val _journeyState = MutableStateFlow(ActiveJourneyUiState())
    val journeyState: StateFlow<ActiveJourneyUiState> = _journeyState.asStateFlow()

    // ---------------------------------------------------------------------------
    // Route completion
    // ---------------------------------------------------------------------------

    private val _completionState = MutableStateFlow(RouteCompletionUiState())
    val completionState: StateFlow<RouteCompletionUiState> = _completionState.asStateFlow()

    // ---------------------------------------------------------------------------
    // List screen actions
    // ---------------------------------------------------------------------------

    init {
        auth.addAuthStateListener(authStateListener)
        loadRoutes()
    }

    fun loadRoutes(city: String = _listState.value.city) {
        val userId = currentUserId ?: return
        val state = _listState.value
        if (state.isLoading && state.city == city) return

        _listState.update { it.copy(isLoading = true, error = null, city = city) }
        viewModelScope.launch {
            try {
                val page = routeRepository.getRoutesByCity(city)
                val progressMap = routeRepository.getRouteProgressSummary(
                    userId,
                    page.items.map { it.id },
                )
                val completedIds = progressMap.filter { it.value.isCompleted }.keys
                val inProgressIds = progressMap.filter { !it.value.isCompleted }.keys

                // Routes the user has started but not finished — shown at top.
                // All routes for browsing (including completed ones with badge).

                _listState.update {
                    it.copy(
                        exploreRoutes = page.items,
                        cursor = page.cursor,
                        hasMore = page.hasMore,
                        isLoading = false,
                        completedRouteIds = completedIds,
                        inProgressRouteIds = inProgressIds,
                    )
                }
            } catch (e: Exception) {
                _listState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadMoreRoutes() {
        val state = _listState.value
        if (!state.hasMore || state.isLoadingMore) return
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _listState.update { it.copy(isLoadingMore = true) }
            try {
                val page = routeRepository.getRoutesByCity(state.city, state.cursor)
                val progressMap = routeRepository.getRouteProgressSummary(
                    userId,
                    page.items.map { it.id },
                )
                val newCompleted = progressMap.filter { it.value.isCompleted }.keys
                val newInProgress = progressMap.filter { !it.value.isCompleted }.keys

                _listState.update {
                    it.copy(
                        exploreRoutes = it.exploreRoutes + page.items,
                        cursor = page.cursor,
                        hasMore = page.hasMore,
                        isLoadingMore = false,
                        completedRouteIds = it.completedRouteIds + newCompleted,
                        inProgressRouteIds = it.inProgressRouteIds + newInProgress,
                    )
                }
            } catch (e: Exception) {
                _listState.update { it.copy(isLoadingMore = false, error = e.message) }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Detail screen actions
    // ---------------------------------------------------------------------------

    fun loadRoute(routeId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _detailState.value = RouteDetailUiState(isLoading = true)
            try {
                val route = routeRepository.getRoute(routeId)
                val progress = routeRepository.getRouteProgress(userId, routeId)
                val reviews = routeRepository.getRouteReviews(routeId)
                val completedPointIds = when {
                    progress?.isCompleted == true && route != null -> route.points.indices.map(Int::toString)
                    else -> progress?.completedPointIds ?: emptyList()
                }
                _detailState.update {
                    it.copy(
                        route = route,
                        isLoading = false,
                        completedPointIds = completedPointIds,
                        isAlreadyCompleted = progress?.isCompleted == true,
                        startedAt = progress?.startedAt,
                        routeReviews = reviews,
                    )
                }

                if (progress?.isCompleted == true && _journeyState.value.route?.id == routeId) {
                    _journeyState.value = ActiveJourneyUiState()
                }

                // Restore an in-progress journey into memory so the Resume button works.
                // Do NOT auto-navigate — the user should tap "Resume" explicitly.
                if (progress != null && !progress.isCompleted && route != null && _journeyState.value.route == null) {
                    val completedIndices =
                        progress.completedPointIds.mapNotNull { it.toIntOrNull() }.toSet()
                    val resumeIndex =
                        (0 until route.points.size).firstOrNull { it !in completedIndices } ?: 0
                    // Restore persisted answers so they can be pre-filled.
                    val savedAnswers =
                        progress.lastAnswers.mapKeys { (k, _) -> k.toIntOrNull() ?: -1 }
                            .filterKeys { it >= 0 }
                    _journeyState.value = ActiveJourneyUiState(
                        route = route,
                        currentPointIndex = resumeIndex,
                        completedPointIndices = completedIndices,
                        isRouteCompleted = false,
                        startedAt = progress.startedAt,
                        savedAnswers = savedAnswers,
                        // Pre-fill the answer for the resume checkpoint if already saved.
                        answerInput = savedAnswers[resumeIndex] ?: "",
                    )
                }
            } catch (e: Exception) {
                _detailState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Starts (or resumes) the journey for [route].
     *
     * - If the user has in-progress state the journey is already loaded in
     *   [_journeyState]; we simply signal navigation without re-starting.
     * - For a fresh start we write the progress doc to Firestore, then load.
     */
    @SuppressLint("RestrictedApi")
    fun startJourney(route: Route) {
        if (_detailState.value.isAlreadyCompleted) return
        val userId = currentUserId ?: return
        resetCompletionState()

        // If a journey is already in memory (resumed), just trigger navigation.
        if (_journeyState.value.route?.id == route.id) {
            _journeyState.update { it.copy(navigationTrigger = it.navigationTrigger + 1) }
            return
        }

        viewModelScope.launch {
            _detailState.update { it.copy(isStarting = true) }
            try {
                val progress = routeRepository.startRoute(userId, route.id)
                if (progress.isCompleted) {
                    markRouteCompleted(route)
                    _detailState.update { it.copy(isStarting = false) }
                    return@launch
                }

                val completedIndices = progress.completedPointIds.mapNotNull { it.toIntOrNull() }.toSet()
                val resumeIndex =
                    (0 until route.points.size).firstOrNull { it !in completedIndices } ?: 0
                val savedAnswers = progress.lastAnswers
                    .mapKeys { (k, _) -> k.toIntOrNull() ?: -1 }
                    .filterKeys { it >= 0 }

                _journeyState.value = ActiveJourneyUiState(
                    route = route,
                    currentPointIndex = resumeIndex,
                    completedPointIndices = completedIndices,
                    isRouteCompleted = false,
                    startedAt = progress.startedAt,
                    savedAnswers = savedAnswers,
                    answerInput = savedAnswers[resumeIndex] ?: "",
                    navigationTrigger = 1,
                )
                _detailState.update {
                    it.copy(
                        isStarting = false,
                        completedPointIds = progress.completedPointIds,
                        isAlreadyCompleted = false,
                        startedAt = progress.startedAt,
                    )
                }
                markRouteInProgress(route)
            } catch (e: Exception) {
                Log.e("RouteViewModel", "Error starting journey: ${e.message}")
                _detailState.update { it.copy(isStarting = false, error = e.message) }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Journey screen actions
    // ---------------------------------------------------------------------------

    fun onAnswerInputChanged(value: String) {
        _journeyState.update { it.copy(answerInput = value, answerResult = null) }
    }

    /**
     * Navigates back to the previous checkpoint.
     *
     * Completed state is preserved. The answer field is pre-filled with the
     * answer the user previously submitted for that checkpoint (if saved).
     */
    fun goToPreviousPoint() {
        _journeyState.update { state ->
            if (state.currentPointIndex <= 0) return@update state
            val prevIndex = state.currentPointIndex - 1
            state.copy(
                currentPointIndex = prevIndex,
                // Pre-fill with the saved answer for that checkpoint.
                answerInput = state.savedAnswers[prevIndex] ?: "",
                answerResult = if (prevIndex in state.completedPointIndices) true else null,
            )
        }
    }

    /**
     * Checks the user's answer against the current point's quiz answer.
     *
     * On a correct answer the point is marked complete in Firestore and the
     * answer is stored in [savedAnswers] for later pre-fill.
     * If it was the last point, [triggerRouteCompletion] is called automatically.
     */
    fun checkAnswer() {
        val state = _journeyState.value
        val point = state.currentPoint ?: return
        val route = state.route ?: return
        val userId = currentUserId ?: return

        val expected = point.quizAnswer.trim()
        val isCorrect =
            expected.isNotEmpty() && state.answerInput.trim().equals(expected, ignoreCase = true)

        _journeyState.update { it.copy(isCheckingAnswer = true) }

        if (!isCorrect) {
            _journeyState.update { it.copy(isCheckingAnswer = false, answerResult = false) }
            return
        }

        val pointId = "${state.currentPointIndex}"
        val answeredText = state.answerInput.trim()
        viewModelScope.launch {
            try {
                routeRepository.completeRoutePoint(userId, route.id, pointId, answeredText)
                val newCompleted = state.completedPointIndices + state.currentPointIndex
                val newSaved = state.savedAnswers + (state.currentPointIndex to answeredText)
                _journeyState.update {
                    it.copy(
                        isCheckingAnswer = false,
                        answerResult = true,
                        completedPointIndices = newCompleted,
                        savedAnswers = newSaved,
                    )
                }
            } catch (e: Exception) {
                _journeyState.update { it.copy(isCheckingAnswer = false, error = e.message) }
            }
        }
    }

    /** Advances to the next checkpoint, or triggers route completion. */
    fun advanceToNextPoint() {
        val state = _journeyState.value
        val route = state.route ?: return

        if (state.isLastPoint) {
            triggerRouteCompletion(route)
        } else {
            val nextIndex = state.currentPointIndex + 1
            _journeyState.update {
                it.copy(
                    currentPointIndex = nextIndex,
                    // Pre-fill if already visited.
                    answerInput = it.savedAnswers[nextIndex] ?: "",
                    answerResult = if (nextIndex in it.completedPointIndices) true else null,
                )
            }
        }
    }

    private fun triggerRouteCompletion(route: Route) {
        val userId = currentUserId ?: return
        if (_journeyState.value.isRouteCompleted) return
        viewModelScope.launch {
            _journeyState.update { it.copy(isLoading = true) }
            try {
                val user = auth.currentUser
                val result = routeRepository.completeRoute(
                    userId = userId,
                    username = user?.displayName ?: "Traveller",
                    userProfileImageUrl = user?.photoUrl?.toString(),
                    route = route,
                    startedAt = _journeyState.value.startedAt,
                )
                _completionState.value = RouteCompletionUiState(
                    completion = result.completion,
                    xpAwarded = XpRewards.ROUTE_COMPLETION,
                )
                markRouteCompleted(route)
                _journeyState.update { it.copy(isLoading = false, isRouteCompleted = true) }
            } catch (e: Exception) {
                _journeyState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Completion screen actions
    // ---------------------------------------------------------------------------

    fun onRatingChanged(rating: Int) {
        _completionState.update { it.copy(rating = rating) }
    }

    fun onReviewTextChanged(text: String) {
        _completionState.update { it.copy(reviewText = text) }
    }

    fun addPhotoUri(uri: String) {
        _completionState.update { it.copy(selectedPhotoUris = it.selectedPhotoUris + uri) }
    }

    fun removePhotoUri(uri: String) {
        _completionState.update { it.copy(selectedPhotoUris = it.selectedPhotoUris - uri) }
    }

    /**
     * Uploads photos, submits the review, and shares the completion to the feed.
     * Marks [isDone] = true so the screen cannot be re-entered.
     */
    fun submitAndShare(context: Context, onDone: () -> Unit) {
        val state = _completionState.value
        val completion = state.completion ?: return
        val userId = currentUserId ?: return
        if (state.isSubmitting || state.isDone) return

        viewModelScope.launch {
            _completionState.update { it.copy(isSubmitting = true, error = null) }
            try {
                val uploadedUrls = state.selectedPhotoUris.map { uriString ->
                    val result = storageRepository.uploadUserImage(
                        context, uriString.toUri(), userId
                    )
                    result.downloadUrl
                }
                val userProfile = repository.getUser(userId)
                val username = userProfile?.username
                    ?.takeIf { it.isNotBlank() }
                    ?: auth.currentUser?.displayName
                    ?: "Traveller"

                if (state.rating > 0) {
                    val review = RouteReview(
                        userId = userId,
                        username = username,
                        rating = state.rating,
                        comment = state.reviewText,
                        photoUrls = uploadedUrls,
                    )
                    routeRepository.addRouteReview(completion.routeId, review)
                }

                val feedEntry = FeedEntry(
                    userId = userId,
                    username = username,
                    userProfileImageUrl = userProfile?.profileImageUrl
                        ?: auth.currentUser?.photoUrl?.toString(),
                    type = FeedEntryType.ROUTE_COMPLETED,
                    imageUrl = uploadedUrls.firstOrNull() ?: "",
                    imageUrls = uploadedUrls,
                    timestamp = completion.completedAt,
                    routeId = completion.routeId,
                    description = completion.caption
                )
                routeRepository.shareRouteCompletionToFeed(completion, feedEntry)

                _completionState.update { it.copy(isSubmitting = false, isDone = true) }
                // Clear journey state so back-navigation cannot re-trigger completion.
                _journeyState.value = ActiveJourneyUiState()
                onDone()
            } catch (e: Exception) {
                _completionState.update { it.copy(isSubmitting = false, error = e.message) }
            }
        }
    }

    /** Skips sharing and marks the completion screen as done. */
    fun skipSharing(onDone: () -> Unit) {
        if (_completionState.value.isDone) return
        _completionState.update { it.copy(isDone = true) }
        _journeyState.value = ActiveJourneyUiState()
        onDone()
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun clearRouteListState() {
        _listState.update {
            it.copy(
                exploreRoutes = emptyList(),
                isLoading = false,
                isLoadingMore = false,
                hasMore = false,
                cursor = null,
                error = null,
                completedRouteIds = emptySet(),
                inProgressRouteIds = emptySet(),
            )
        }
    }

    private fun markRouteInProgress(route: Route) {
        _listState.update { state ->
            state.copy(
                completedRouteIds = state.completedRouteIds - route.id,
                inProgressRouteIds = state.inProgressRouteIds + route.id,
            )
        }
    }

    private fun markRouteCompleted(route: Route) {
        _detailState.update {
            it.copy(
                completedPointIds = route.points.indices.map(Int::toString),
                isAlreadyCompleted = true,
            )
        }
        if (_journeyState.value.route?.id == route.id) {
            _journeyState.update { it.copy(isRouteCompleted = true) }
        }
        _listState.update { state ->
            state.copy(
                completedRouteIds = state.completedRouteIds + route.id,
                inProgressRouteIds = state.inProgressRouteIds - route.id,
            )
        }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }

    private fun resetCompletionState() {
        _completionState.value = RouteCompletionUiState()
    }

    private val currentUserId: String?
        get() = _authUserId.value
}
