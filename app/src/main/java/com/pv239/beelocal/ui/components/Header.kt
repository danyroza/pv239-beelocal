package com.pv239.beelocal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pv239.beelocal.R
import com.pv239.beelocal.ui.theme.BeelocalTheme

@Composable
fun Header(
    streakCount: Int,
    honeyCount: Int,
    modifier: Modifier = Modifier,
    profileImageUrl: String? = null,
    username: String? = null,
    onProfileClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceBright,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderAvatar(profileImageUrl = profileImageUrl, username = username, onProfileClick = onProfileClick)
                Text(
                    text = stringResource(R.string.header_app_name),
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderChip(text = stringResource(R.string.header_chip_streak, streakCount))
                HeaderChip(text = stringResource(R.string.header_chip_honey, honeyCount))
            }
        }
    }
}

/**
 * Circular profile thumbnail rendered on the left side of the header.
 *
 * Renders the remote [profileImageUrl] via Coil when available so the avatar
 * reactively updates as soon as the user uploads a new picture. When no URL
 * is set, falls back to the capitalised first letter of [username] (a "?"
 * placeholder if even that is missing) — no generic person glyph.
 */
@Composable
private fun HeaderAvatar(profileImageUrl: String?, username: String?, onProfileClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onProfileClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (!profileImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = stringResource(R.string.header_profile_picture_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = username?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
            )
        }
    }
}

@Composable
fun HeaderChip(text: String) {
    SuggestionChip(
        onClick = { /* read-only badge */ },
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = null
    )
}

@Preview(showBackground = true)
@Composable
fun HeaderPreview() {
    BeelocalTheme {
        Header(
            streakCount = 5,
            honeyCount = 100,
            username = "honeybee",
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HeaderChipPreview() {
    BeelocalTheme {
        HeaderChip(text = "5 🔥")
    }
}
