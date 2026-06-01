package com.pv239.beelocal.ui.screens.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pv239.beelocal.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SocialViewModel @Inject constructor(
    private val repository: SocialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    // Debounce live search input
    private val searchQueryFlow = MutableStateFlow("")

    init {
        loadFriendsAndFeed()
        observeSearchQuery()
    }

    // -------------------------------------------------------------------------
    // Tabs
    // -------------------------------------------------------------------------

    fun selectTab(tab: SocialTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        when (tab) {
            SocialTab.FEED -> if (_uiState.value.feedEntries.isEmpty()) loadFeed()
            SocialTab.FRIENDS -> if (_uiState.value.friends.isEmpty()) loadFriends()
            SocialTab.SEARCH -> Unit // triggered by user typing
        }
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    private fun loadFriendsAndFeed() {
        loadFriends()
        loadFeed()
    }

    // -------------------------------------------------------------------------
    // Friends
    // -------------------------------------------------------------------------

    fun loadFriends() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFriendsLoading = true, friendsError = null) }
            runCatching { repository.getFriends() }
                .onSuccess { friends ->
                    _uiState.update { it.copy(friends = friends, isFriendsLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isFriendsLoading = false,
                            friendsError = err.message ?: "Failed to load friends",
                        )
                    }
                }
        }
    }

    /**
     * Follow [userId], respecting the target's profile visibility:
     *  - **Public** target → instantly added to the current user's friends list
     *    (feed gets the new entries on the next refresh).
     *  - **Private** target → a follow request is created and must be accepted
     *    by the target; nothing is added to the local friends list yet.
     */
    fun addFriend(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(pendingFriendAction = userId) }
            runCatching { repository.requestFollow(userId) }
                .onSuccess { accepted ->
                    if (accepted) {
                        _uiState.update {
                            it.copy(
                                pendingFriendAction = null,
                                snackbarMessage = "Friend added!",
                            )
                        }
                        loadFriends()
                        loadFeed() // feed may have new entries now
                    } else {
                        // Target is private — request is pending approval, so we
                        // intentionally do not refresh friends / feed yet.
                        _uiState.update {
                            it.copy(
                                pendingFriendAction = null,
                                snackbarMessage = "Follow request sent",
                            )
                        }
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            pendingFriendAction = null,
                            snackbarMessage = err.message ?: "Could not add friend",
                        )
                    }
                }
        }
    }

    fun removeFriend(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(pendingFriendAction = userId) }
            runCatching { repository.removeFriend(userId) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            pendingFriendAction = null,
                            friends = state.friends.filter { it.id != userId },
                            snackbarMessage = "Friend removed",
                        )
                    }
                    loadFeed()
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            pendingFriendAction = null,
                            snackbarMessage = err.message ?: "Could not remove friend",
                        )
                    }
                }
        }
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
        searchQueryFlow.value = ""
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query -> performSearch(query) }
        }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        _uiState.update { it.copy(isSearching = true, searchError = null) }
        runCatching { repository.searchUsers(query) }
            .onSuccess { page ->
                _uiState.update {
                    it.copy(
                        searchResults = page.items,
                        hasMoreSearchResults = page.hasMore,
                        isSearching = false,
                    )
                }
            }
            .onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchError = err.message ?: "Search failed",
                    )
                }
            }
    }

    // -------------------------------------------------------------------------
    // Feed
    // -------------------------------------------------------------------------

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFeedLoading = true, feedError = null) }
            runCatching { repository.getFriendsFeed() }
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            feedEntries = page.items,
                            hasMoreFeedEntries = page.hasMore,
                            isFeedLoading = false,
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isFeedLoading = false,
                            feedError = err.message ?: "Failed to load feed",
                        )
                    }
                }
        }
    }

    // -------------------------------------------------------------------------
    // Snackbar
    // -------------------------------------------------------------------------

    fun snackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    fun isFriend(userId: String): Boolean =
        _uiState.value.friends.any { it.id == userId }
}