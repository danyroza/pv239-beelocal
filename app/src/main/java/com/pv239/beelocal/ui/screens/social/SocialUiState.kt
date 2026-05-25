package com.pv239.beelocal.ui.screens.social

import com.pv239.beelocal.model.FeedEntry
import com.pv239.beelocal.model.User

data class SocialUiState(
    // Search
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val hasMoreSearchResults: Boolean = false,

    // Friends
    val friends: List<User> = emptyList(),
    val isFriendsLoading: Boolean = false,
    val friendsError: String? = null,

    // Friend actions
    val pendingFriendAction: String? = null, // userId currently being added/removed

    // Feed
    val feedEntries: List<FeedEntry> = emptyList(),
    val isFeedLoading: Boolean = false,
    val feedError: String? = null,
    val hasMoreFeedEntries: Boolean = false,

    // Currently selected tab
    val selectedTab: SocialTab = SocialTab.FEED,

    // Snackbar / one-shot messages
    val snackbarMessage: String? = null,
)

enum class SocialTab(val label: String) {
    FEED("Feed"),
    FRIENDS("Friends"),
    SEARCH("Find People"),
}