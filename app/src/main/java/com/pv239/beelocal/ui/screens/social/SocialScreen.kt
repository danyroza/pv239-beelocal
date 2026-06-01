package com.pv239.beelocal.ui.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pv239.beelocal.ui.screens.social.components.FeedCard
import com.pv239.beelocal.ui.screens.social.components.SocialSearchBar
import com.pv239.beelocal.ui.screens.social.components.UserCard

@Composable
fun SocialScreen(
    innerPadding: PaddingValues,
    startTab: SocialTab? = null,
    viewModel: SocialViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Honor a deep-link start tab the first time we see this destination
    // (e.g. the profile "View all friends" / "Invite" affordances). We key on
    // the value itself so a re-entry with a different requested tab still
    // wins, but tab changes the user makes locally aren't overridden by
    // recompositions.
    LaunchedEffect(startTab) {
        if (startTab != null && uiState.selectedTab != startTab) {
            viewModel.selectTab(startTab)
        }
    }

    // One-shot snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarShown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab row
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                SocialTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.label) },
                    )
                }
            }

            // Tab content
            when (uiState.selectedTab) {
                SocialTab.FEED -> FeedTab(uiState, onRetry = { viewModel.loadFeed() })
                SocialTab.FRIENDS -> FriendsTab(
                    uiState = uiState,
                    onRemoveFriend = viewModel::removeFriend,
                    onRetry = { viewModel.loadFriends() },
                )
                SocialTab.SEARCH -> SearchTab(
                    uiState = uiState,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onClear = viewModel::clearSearch,
                    onAddFriend = viewModel::addFriend,
                    onRemoveFriend = viewModel::removeFriend,
                    isFriend = viewModel::isFriend,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ---------------------------------------------------------------------------
// Feed tab
// ---------------------------------------------------------------------------

@Composable
private fun FeedTab(
    uiState: SocialUiState,
    onRetry: () -> Unit,
) {
    when {
        uiState.isFeedLoading -> CenteredLoader()
        uiState.feedError != null -> ErrorMessage(uiState.feedError, onRetry)
        uiState.feedEntries.isEmpty() -> EmptyMessage("Add friends to see their challenge photos here! 🐝")
        else -> LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.feedEntries, key = { it.id ?: it.hashCode().toString() }) { entry ->
                FeedCard(entry = entry)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Friends tab
// ---------------------------------------------------------------------------

@Composable
private fun FriendsTab(
    uiState: SocialUiState,
    onRemoveFriend: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        uiState.isFriendsLoading -> CenteredLoader()
        uiState.friendsError != null -> ErrorMessage(uiState.friendsError, onRetry)
        uiState.friends.isEmpty() -> EmptyMessage("No friends yet — search for people to add! 🍯")
        else -> LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.friends, key = { it.id }) { friend ->
                UserCard(
                    user = friend,
                    isFriend = true,
                    isLoading = uiState.pendingFriendAction == friend.id,
                    onAddFriend = {},
                    onRemoveFriend = { onRemoveFriend(friend.id) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Search tab
// ---------------------------------------------------------------------------

@Composable
private fun SearchTab(
    uiState: SocialUiState,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onAddFriend: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    isFriend: (String) -> Boolean,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SocialSearchBar(
            query = uiState.searchQuery,
            onQueryChange = onQueryChange,
            onClear = onClear,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        when {
            uiState.isSearching -> CenteredLoader()
            uiState.searchError != null -> ErrorMessage(uiState.searchError, onRetry = {
                onQueryChange(uiState.searchQuery)
            })
            uiState.searchQuery.isNotBlank() && uiState.searchResults.isEmpty() ->
                EmptyMessage("No users found for \"${uiState.searchQuery}\"")
            uiState.searchQuery.isBlank() ->
                EmptyMessage("Type a username to find people 🔍")
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.searchResults, key = { it.id }) { user ->
                    val friend = isFriend(user.id)
                    UserCard(
                        user = user,
                        isFriend = friend,
                        isLoading = uiState.pendingFriendAction == user.id,
                        onAddFriend = { onAddFriend(user.id) },
                        onRemoveFriend = { onRemoveFriend(user.id) },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

@Composable
private fun CenteredLoader() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}