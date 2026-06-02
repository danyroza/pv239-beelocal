package com.pv239.beelocal.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.R

/**
 * Top section of the profile screen: gradient-ringed avatar, username, role
 * tagline, and the bento-style stats grid summarising the user's social graph.
 *
 * The avatar is tappable — [onAvatarClick] is invoked when the user wants to
 * pick a new profile picture (the actual photo-picker launcher lives in the
 * screen so it has access to the activity result registry). While an upload
 * is in flight, the avatar shows a circular progress indicator instead of the
 * camera badge.
 */
@Composable
fun ProfileHero(
    username: String,
    isPublic: Boolean,
    profileImageUrl: String?,
    streak: Int,
    xp: Int,
    onAvatarClick: () -> Unit,
    pictureUploading: Boolean = false,
    modifier: Modifier = Modifier,
    /**
     * When `false`, the avatar is rendered as a static portrait — the camera
     * badge and tap-to-pick affordances are hidden. Used by the public
     * user-profile screen where another user's avatar must not be editable.
     */
    editable: Boolean = true,
) {

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Gradient ring + avatar with floating "edit picture" badge.
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                            )
                        )
                    )
                    .clickable(
                        enabled = editable && !pictureUploading,
                        onClick = onAvatarClick,
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Avatar(
                    imageUrl = profileImageUrl,
                    sizeDp = 116,
                    background = MaterialTheme.colorScheme.surfaceContainerLowest,
                    username = username,
                )
                if (pictureUploading) {
                    // Dim the avatar and overlay a spinner so the user knows the
                    // tap was acknowledged and the upload is in progress.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }

            // Floating camera badge in the bottom-right corner of the avatar to
            // signal that the avatar itself is tappable for changing the picture.
            if (editable && !pictureUploading) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onAvatarClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_photo_camera_24),
                        contentDescription = stringResource(R.string.profile_change_picture),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = username.ifBlank { stringResource(R.string.profile_unknown_user) },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_star_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.profile_tagline_master_explorer),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bento stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatTile(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.profile_stat_streak_label),
                value = streak.toString(),
                caption = stringResource(R.string.profile_stat_streak_caption),
                container = MaterialTheme.colorScheme.surfaceContainerLowest,
                accent = MaterialTheme.colorScheme.primary,
                onAccent = MaterialTheme.colorScheme.onSurfaceVariant,
                iconRes = R.drawable.outline_access_time_24,
            )
            StatTile(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.profile_stat_xp_label),
                value = xp.toString(),
                caption = stringResource(R.string.profile_stat_xp_caption),
                container = MaterialTheme.colorScheme.primaryContainer,
                accent = MaterialTheme.colorScheme.onPrimaryContainer,
                onAccent = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                iconRes = R.drawable.baseline_star_24,
                iconAlpha = 0.18f,
            )
        }
    }
}
