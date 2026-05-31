package com.pv239.beelocal.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pv239.beelocal.ui.theme.BeelocalTheme

/**
 * Reusable circular user avatar.
 *
 * If [profileImageUrl] is non-null and non-blank, shows the image. Otherwise
 * falls back to a tinted circle with the first two letters of [username]
 * (uppercased). Used on profile, feed posts, header, etc.
 */
@Composable
fun UserAvatar(
    username: String,
    profileImageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        if (!profileImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = usernameInitials(username),
                    style = textStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/**
 * Returns the first two letters of [username] in uppercase. Falls back to
 * "?" if the username is blank.
 */
fun usernameInitials(username: String): String {
    val trimmed = username.trim()
    if (trimmed.isEmpty()) return "?"
    return trimmed.take(2).uppercase()
}

@Preview(showBackground = true)
@Composable
private fun UserAvatarWithImagePreview() {
    BeelocalTheme {
        UserAvatar(
            username = "honey_bee_23",
            profileImageUrl = null,
            size = 64.dp,
            textStyle = MaterialTheme.typography.titleLarge,
        )
    }
}
