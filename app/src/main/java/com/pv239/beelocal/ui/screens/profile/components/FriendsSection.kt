package com.pv239.beelocal.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.R

/**
 * Horizontal friends strip with a heading + "View all" affordance and a row
 * of [FriendChip]s.
 *
 * `User.friends` currently exposes only opaque IDs (no denormalized names or
 * avatars), so by default we render just the "Invite" affordance and let the
 * count headline communicate how many friends the user has. Placeholder
 * "Bee N" chips are only emitted when [isPlaceholder] is explicitly set,
 * e.g. for previews / design teasers.
 */
@Composable
fun FriendsSection(
    friendsCount: Int,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(R.string.profile_friends_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            // Only offer a "View all" entry point when there are real friend
            // identities to navigate to.
            if (friendsCount > 0) {
                Text(
                    text = stringResource(R.string.profile_friends_view_all, friendsCount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            item {
                FriendChip(
                    label = stringResource(R.string.profile_friend_invite),
                    iconRes = R.drawable.baseline_person_24,
                    highlight = true,
                )
            }
            // Synthetic chips are gated behind an explicit placeholder mode so
            // they cannot masquerade as real friend data driven by friendsCount.
            if (isPlaceholder) {
                items(count = friendsCount.coerceAtMost(8)) { index ->
                    FriendChip(
                        label = stringResource(R.string.profile_friend_placeholder, index + 1),
                        iconRes = R.drawable.outline_person_24,
                        highlight = false,
                    )
                }
            }
        }
    }
}

/**
 * Single circular friend tile, used both for the "Invite" affordance (with
 * [highlight] = true) and placeholder friend avatars.
 */
@Composable
fun FriendChip(
    label: String,
    iconRes: Int,
    highlight: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.width(72.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (highlight) MaterialTheme.colorScheme.surfaceContainerHigh
                    else MaterialTheme.colorScheme.surfaceContainerLow
                )
                .border(
                    width = 2.dp,
                    color = if (highlight) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (highlight) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
