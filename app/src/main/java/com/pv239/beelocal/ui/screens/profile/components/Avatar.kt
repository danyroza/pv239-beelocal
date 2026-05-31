package com.pv239.beelocal.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pv239.beelocal.R

/**
 * Circular avatar that loads [imageUrl] via Coil when available, otherwise
 * falls back to the capitalised first letter of [username]. Shared by the
 * hero, follow-request rows, and any other profile-adjacent surface that
 * needs a tiny portrait.
 *
 * The fallback text scales with [sizeDp] so the same component looks correct
 * whether it's a 40 dp header thumbnail or a 116 dp hero portrait.
 */
@Composable
fun Avatar(
    imageUrl: String?,
    sizeDp: Int,
    background: Color,
    modifier: Modifier = Modifier,
    username: String? = null,
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
            // Glyph-free fallback: show the user's initial so each avatar
            // remains visually distinct without a placeholder image.
            Text(
                text = username?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Black,
                fontSize = (sizeDp * 0.45f).sp,
            )
        }
    }
}
