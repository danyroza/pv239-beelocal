package com.pv239.beelocal.ui.screens.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.pv239.beelocal.R
import com.pv239.beelocal.ui.components.UserAvatar
import com.pv239.beelocal.ui.screens.auth.AuthCard
import com.pv239.beelocal.ui.screens.auth.AuthScreenScaffold

/**
 * Post-registration onboarding screen that lets the user pick a profile
 * picture. The picture is optional — skipping leaves the user with the
 * first-two-letters fallback avatar.
 */
@Composable
fun OnboardingProfilePictureScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingProfilePictureViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OnboardingProfilePictureEvent.Finished -> onFinished()
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onImageSelected(uri) }

    AuthScreenScaffold {
        AuthCard(
            title = stringResource(R.string.onboarding_profile_picture_title),
            subtitle = stringResource(R.string.onboarding_profile_picture_subtitle),
        ) {
            ProfilePicturePreview(
                username = state.username,
                selectedImageUri = state.selectedImageUri?.toString(),
                enabled = !state.isUploading,
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                enabled = !state.isUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = stringResource(
                        if (state.selectedImageUri == null) {
                            R.string.onboarding_profile_picture_pick
                        } else {
                            R.string.onboarding_profile_picture_change
                        }
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            state.errorMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- Primary CTA: Continue (uploads if a picture was picked) ---
            Button(
                onClick = { viewModel.uploadAndContinue() },
                enabled = !state.isUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                if (state.isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.onboarding_profile_picture_continue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // --- Secondary CTA: Skip ---
            TextButton(
                onClick = { viewModel.skip() },
                enabled = !state.isUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_profile_picture_skip),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

/**
 * Large circular preview. Shows the picked image, or the username-initials
 * avatar fallback when nothing is selected yet.
 */
@Composable
private fun ProfilePicturePreview(
    username: String,
    selectedImageUri: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (selectedImageUri != null) {
            Surface(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled, onClick = onClick),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            UserAvatar(
                username = username,
                profileImageUrl = null,
                size = 140.dp,
                textStyle = MaterialTheme.typography.displaySmall,
                modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
            )
        }
    }
}
