package com.pv239.beelocal.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pv239.beelocal.R

/**
 * Circular avatar that loads [imageUrl] via Coil when available, otherwise
 * falls back to a generic person glyph. Shared by the hero, follow-request
 * rows, and any other profile-adjacent surface that needs a tiny portrait.
 */
@Composable
fun Avatar(
    imageUrl: String?,
    sizeDp: Int,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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
