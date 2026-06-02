package com.pv239.beelocal.ui.screens.social.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pv239.beelocal.model.User

/**
 * Card that displays a user's avatar, username, and a contextual
 * Add / Remove friend action button.
 *
 * @param user         The user to display.
 * @param isFriend     Whether the current user is already friends with this user.
 * @param isLoading    True while a friend action (add/remove) is in flight.
 * @param onAddFriend  Called when the user taps "Add Friend".
 * @param onRemoveFriend Called when the user taps "Remove".
 */
@Composable
fun UserCard(
    user: User,
    isFriend: Boolean,
    isLoading: Boolean,
    onAddFriend: () -> Unit,
    onRemoveFriend: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Optional handler invoked when the card body (excluding the trailing
     * action button) is tapped — used to navigate to the user's public
     * profile. Defaults to a no-op so existing callers don't need to opt in.
     */
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Avatar + username — only this leading content responds to
            // [onClick] so taps on the trailing action button (or its
            // spinner during a friend-action) don't accidentally trigger
            // navigation to the user's profile.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base },
            ) {
                UserAvatar(
                    imageUrl = user.profileImageUrl,
                    username = user.username,
                )
                Spacer(Modifier.width(12.dp))
                // Only the username shall be shown publicly here
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Action button
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            } else if (isFriend) {
                OutlinedButton(
                    onClick = onRemoveFriend,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Remove")
                }
            } else {
                Button(onClick = onAddFriend) {
                    Text("Add")
                }
            }
        }
    }
}

@Composable
fun UserAvatar(
    imageUrl: String?,
    username: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
) {
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "$username avatar",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape),
        )
    } else {
        // Fallback: coloured circle with initials
        Box(
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = username.take(1).uppercase(),
                style = if (size >= 44) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}