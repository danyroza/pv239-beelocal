package com.pv239.beelocal.ui.screens.routes.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.R
import androidx.compose.material3.MaterialTheme

/**
 * A row of 5 tappable star icons representing a 1–5 rating.
 *
 * Icons are filled yellow up to [rating], dimmed beyond it.
 */
@Composable
fun HexRatingBar(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        (1..5).forEach { star ->
            Icon(
                painter = painterResource(R.drawable.baseline_star_24),
                contentDescription = stringResource(R.string.hex_rating_bar_star, star),
                tint = if (star <= rating) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onRatingChanged(star) },
            )
        }
    }
}