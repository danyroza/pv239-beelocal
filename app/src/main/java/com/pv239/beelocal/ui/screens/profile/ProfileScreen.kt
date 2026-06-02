package com.pv239.beelocal.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pv239.beelocal.R
import com.pv239.beelocal.model.FollowRequest
import com.pv239.beelocal.ui.screens.profile.components.ChangePasswordDialog
import com.pv239.beelocal.ui.screens.profile.components.ChangePasswordRow
import com.pv239.beelocal.ui.screens.profile.components.FollowRequestRow
import com.pv239.beelocal.ui.screens.profile.components.FriendsSection
import com.pv239.beelocal.ui.screens.profile.components.PreferenceCard
import com.pv239.beelocal.ui.screens.profile.components.ProfileHero
import com.pv239.beelocal.ui.screens.profile.components.VisibilityRow

/**
 * Top-level profile destination. Owns nothing beyond the [ProfileViewModel]
 * subscription and the dispatch between the three UI states; the actual
 * layout is composed out of the smaller pieces in
 * `ui.screens.profile.components`.
 */
@Composable
fun ProfileScreen(
    innerPadding: PaddingValues,
    onLogout: () -> Unit,
    onViewAllFriends: () -> Unit = {},
    onInviteFriend: () -> Unit = {},
    /**
     * Invoked when the user taps a friend chip in the friends cluster to
     * open that friend's public profile.
     */
    onOpenUserProfile: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Photo picker for changing the profile picture. Lives at the screen level
    // so the underlying activity result registry is in scope; we just hand the
    // returned URI off to the view model for upload.
    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.uploadProfilePicture(uri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ProfileUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }

            is ProfileUiState.Ready -> {
                ProfileContent(
                    state = state,
                    innerPadding = innerPadding,
                    onVisibilityChange = viewModel::setProfilePublic,
                    onAvatarClick = {
                        pickPhotoLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onAccept = viewModel::acceptRequest,
                    onDeny = viewModel::denyRequest,
                    onChangePasswordClick = viewModel::openPasswordDialog,
                    onViewAllFriends = onViewAllFriends,
                    onInviteFriend = onInviteFriend,
                    onOpenUserProfile = onOpenUserProfile,
                    onLogout = {
                        viewModel.signOut()
                        onLogout()
                    },
                )

                state.passwordDialog?.let { dialogState ->
                    ChangePasswordDialog(
                        state = dialogState,
                        onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
                        onNewPasswordChange = viewModel::onNewPasswordChange,
                        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                        onDismiss = viewModel::dismissPasswordDialog,
                        onConfirm = viewModel::submitPasswordChange
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState.Ready,
    innerPadding: PaddingValues,
    onVisibilityChange: (Boolean) -> Unit,
    onAvatarClick: () -> Unit,
    onAccept: (FollowRequest) -> Unit,
    onDeny: (FollowRequest) -> Unit,
    onChangePasswordClick: () -> Unit,
    onViewAllFriends: () -> Unit,
    onInviteFriend: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onLogout: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 24.dp,
            bottom = 120.dp,
            start = 24.dp,
            end = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        item {
            ProfileHero(
                username = state.user.username,
                isPublic = state.user.profilePublic,
                profileImageUrl = state.user.profileImageUrl,
                streak = state.statistics.streak,
                xp = state.statistics.xp,
                onAvatarClick = onAvatarClick,
                pictureUploading = state.pictureUploading,
            )
        }

        state.pictureUploadError?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            FriendsSection(
                friendsCount = state.user.friends.size,
                friends = state.friendPreviews,
                onInviteClick = onInviteFriend,
                onViewAllClick = onViewAllFriends,
                // Tapping an individual friend chip now drills into that
                // friend's public profile via the host navigator.
                onFriendClick = { friend -> onOpenUserProfile(friend.id) },
            )
        }


        item {
            Text(
                text = stringResource(R.string.profile_preferences_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
        }

        item {
            PreferenceCard {
                VisibilityRow(
                    isPublic = state.user.profilePublic,
                    updating = state.visibilityUpdating,
                    onChange = onVisibilityChange,
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.profile_follow_requests_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }

        if (state.pendingRequests.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Text(
                        text = if (state.user.profilePublic) {
                            stringResource(R.string.profile_follow_requests_empty_public)
                        } else {
                            stringResource(R.string.profile_follow_requests_empty_private)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        } else {
            items(state.pendingRequests, key = { it.id }) { request ->
                FollowRequestRow(
                    request = request,
                    processing = request.id in state.processingRequestIds,
                    onAccept = { onAccept(request) },
                    onDeny = { onDeny(request) },
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.profile_credentials_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 1.dp,
            ) {
                ChangePasswordRow(
                    onClick = onChangePasswordClick
                )
            }
        }

        item {
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.profile_log_out),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
