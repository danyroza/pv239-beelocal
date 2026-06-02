package com.pv239.beelocal.ui.screens.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pv239.beelocal.R
import com.pv239.beelocal.ui.screens.profile.components.ProfileHero
import com.pv239.beelocal.ui.screens.social.components.FeedCard

/**
 * Read-only public profile destination for **another** user.
 *
 * Layout:
 *  - Top app bar with a back arrow and the target username.
 *  - Reused [ProfileHero] (avatar, username, streak + XP stats) — without the
 *    avatar-picker affordance.
 *  - Follow / Unfollow / Requested control beneath the hero. Hidden when
 *    `isSelf` is true.
 *  - Posts section listing the user's own [com.pv239.beelocal.model.FeedEntry]
 *    documents, gated behind [UserProfileUiState.Ready.canSeeFeed]. Private
 *    profiles you don't follow show a lock card instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    innerPadding: PaddingValues,
    userId: String,
    onBack: () -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel(),
) {
    // Fire load() once per distinct userId. ViewModel de-dups subsequent calls
    // with the same id so config changes / recompositions don't re-fetch.
    LaunchedEffect(userId) {
        viewModel.load(userId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Surface transient snackbar messages (follow / unfollow / errors). Key
    // the effect on the one-shot message itself so we don't restart it on
    // every unrelated Ready field change (which could re-show a snackbar
    // before the view model has had a chance to clear it).
    val snackbarMessage = (uiState as? UserProfileUiState.Ready)?.snackbarMessage
    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            viewModel.snackbarShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (uiState as? UserProfileUiState.Ready)
                        ?.user
                        ?.username
                        ?.let { "@$it" }
                        ?: stringResource(R.string.profile_unknown_user)
                    Text(text = title, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_arrow_back_24),
                            contentDescription = stringResource(R.string.user_profile_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            when (val state = uiState) {
                is UserProfileUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UserProfileUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                is UserProfileUiState.Ready -> {
                    UserProfileContent(
                        state = state,
                        innerPadding = innerPadding,
                        onFollow = viewModel::follow,
                        onRequestUnfollow = viewModel::requestUnfollow,
                        onCancelPendingRequest = viewModel::cancelPendingRequest,
                    )

                    if (state.showUnfollowDialog) {
                        UnfollowConfirmationDialog(
                            username = state.user.username,
                            onConfirm = viewModel::confirmUnfollow,
                            onDismiss = viewModel::dismissUnfollowDialog,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfileContent(
    state: UserProfileUiState.Ready,
    innerPadding: PaddingValues,
    onFollow: () -> Unit,
    onRequestUnfollow: () -> Unit,
    onCancelPendingRequest: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 24.dp,
            // Reserve bottom space for the parent's bottom nav bar.
            bottom = innerPadding.calculateBottomPadding() + 32.dp,
            start = 24.dp,
            end = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            ProfileHero(
                username = state.user.username,
                isPublic = state.user.profilePublic,
                profileImageUrl = state.user.profileImageUrl,
                streak = state.statistics.streak,
                xp = state.statistics.xp,
                onAvatarClick = {}, // public view — no edit affordance
                pictureUploading = false,
                editable = false,
            )
        }

        if (!state.isSelf) {
            item {
                FollowControl(
                    isFollowing = state.isFollowing,
                    hasPendingRequest = state.hasPendingRequest,
                    inFlight = state.followActionInFlight,
                    onFollow = onFollow,
                    onUnfollow = onRequestUnfollow,
                    onCancelRequest = onCancelPendingRequest,
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.user_profile_posts_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
        }

        when {
            !state.canSeeFeed -> item {
                PrivateProfileCard()
            }

            state.feedLoading -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.feedEntries.isEmpty() -> item {
                EmptyFeedCard(username = state.user.username)
            }

            else -> items(
                items = state.feedEntries,
                key = { it.id.ifBlank { it.hashCode().toString() } },
            ) { entry ->
                FeedCard(entry = entry)
            }
        }
    }
}

@Composable
private fun FollowControl(
    isFollowing: Boolean,
    hasPendingRequest: Boolean,
    inFlight: Boolean,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    onCancelRequest: () -> Unit,
) {
    when {
        isFollowing -> OutlinedButton(
            onClick = onUnfollow,
            enabled = !inFlight,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            FollowButtonContent(
                inFlight = inFlight,
                label = stringResource(R.string.user_profile_unfollow),
            )
        }

        hasPendingRequest -> OutlinedButton(
            onClick = onCancelRequest,
            enabled = !inFlight,
            modifier = Modifier.fillMaxWidth(),
        ) {
            FollowButtonContent(
                inFlight = inFlight,
                label = stringResource(R.string.user_profile_requested),
            )
        }

        else -> Button(
            onClick = onFollow,
            enabled = !inFlight,
            modifier = Modifier.fillMaxWidth(),
        ) {
            FollowButtonContent(
                inFlight = inFlight,
                label = stringResource(R.string.user_profile_follow),
            )
        }
    }
}

@Composable
private fun FollowButtonContent(inFlight: Boolean, label: String) {
    if (inFlight) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    } else {
        Text(text = label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PrivateProfileCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.outline_person_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(R.string.user_profile_private_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyFeedCard(username: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Text(
            text = stringResource(R.string.user_profile_empty_posts, username),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Composable
private fun UnfollowConfirmationDialog(
    username: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.user_profile_unfollow_dialog_title, username),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(text = stringResource(R.string.user_profile_unfollow_dialog_message))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.user_profile_unfollow_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.user_profile_unfollow_dialog_cancel))
            }
        },
        // Slightly tinted scrim so the dialog focus is obvious without going
        // black; matches the rest of the app's modal surfaces.
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        iconContentColor = Color.Unspecified,
    )
}
