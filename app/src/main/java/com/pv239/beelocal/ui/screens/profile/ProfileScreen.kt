package com.pv239.beelocal.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.pv239.beelocal.R
import com.pv239.beelocal.model.FollowRequest

@Composable
fun ProfileScreen(
    innerPadding: PaddingValues,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
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
                    onAccept = viewModel::acceptRequest,
                    onDeny = viewModel::denyRequest,
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState.Ready,
    innerPadding: PaddingValues,
    onVisibilityChange: (Boolean) -> Unit,
    onAccept: (FollowRequest) -> Unit,
    onDeny: (FollowRequest) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 16.dp,
            bottom = 100.dp,
            start = 20.dp,
            end = 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header card ──────────────────────────────────────────────────────
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Avatar(
                        imageUrl = state.user.profileImageUrl,
                        sizeDp = 64,
                        background = MaterialTheme.colorScheme.surfaceContainer,
                    )
                    Column {
                        Text(
                            text = state.user.username.ifBlank {
                                stringResource(R.string.profile_unknown_user)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (state.user.email.isNotBlank()) {
                            Text(
                                text = state.user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }

        // ── Visibility setting ───────────────────────────────────────────────
        item {
            VisibilityCard(
                isPublic = state.user.isProfilePublic,
                updating = state.visibilityUpdating,
                onChange = onVisibilityChange,
            )
        }

        // ── Pending follow requests ──────────────────────────────────────────
        item {
            Text(
                text = stringResource(R.string.profile_follow_requests_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        if (state.pendingRequests.isEmpty()) {
            item {
                Text(
                    text = if (state.user.isProfilePublic) {
                        stringResource(R.string.profile_follow_requests_empty_public)
                    } else {
                        stringResource(R.string.profile_follow_requests_empty_private)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
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
    }
}

@Composable
private fun VisibilityCard(
    isPublic: Boolean,
    updating: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_visibility_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPublic) {
                        stringResource(R.string.profile_visibility_public_description)
                    } else {
                        stringResource(R.string.profile_visibility_private_description)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (updating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Switch(checked = isPublic, onCheckedChange = onChange)
            }
        }
    }
}

@Composable
private fun FollowRequestRow(
    request: FollowRequest,
    processing: Boolean,
    onAccept: () -> Unit,
    onDeny: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(
                imageUrl = request.fromUserProfileImageUrl,
                sizeDp = 40,
                background = MaterialTheme.colorScheme.primaryContainer,
            )
            Text(
                text = request.fromUsername.ifBlank {
                    stringResource(R.string.profile_unknown_user)
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (processing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                OutlinedButton(
                    onClick = onDeny,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.profile_follow_request_deny))
                }
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(stringResource(R.string.profile_follow_request_accept))
                }
            }
        }
    }
}

@Composable
private fun Avatar(
    imageUrl: String?,
    sizeDp: Int,
    background: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.header_profile_picture_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.baseline_person_24),
                contentDescription = stringResource(R.string.header_profile_picture_description),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
