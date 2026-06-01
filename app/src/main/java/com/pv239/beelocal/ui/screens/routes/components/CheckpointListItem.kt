package com.pv239.beelocal.ui.screens.routes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pv239.beelocal.R
import com.pv239.beelocal.model.RoutePoint

/**
 * A single checkpoint row displayed on the route detail screen.
 *
 * When [isCompleted] is true, the icon circle turns primary-green and a
 * checkmark icon is shown to indicate the user has already passed this point.
 */
@Composable
fun CheckpointListItem(
    index: Int,
    point: RoutePoint,
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false,
) {
    val iconBg = if (isCompleted)
        MaterialTheme.colorScheme.tertiaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer

    val iconTint = if (isCompleted)
        MaterialTheme.colorScheme.onTertiaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    val cardAlpha = if (isCompleted) 0.7f else 0.5f

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = cardAlpha),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBg, CircleShape),
            ) {
                Icon(
                    painter = painterResource(
                        if (isCompleted) R.drawable.where_to_vote_24px
                        else R.drawable.not_listed_location_24px,
                    ),
                    contentDescription = if (isCompleted)
                        stringResource(R.string.checkpoint_completed)
                    else
                        stringResource(R.string.checkpoint),
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = buildString {
                        append(stringResource(R.string.checkpoint).uppercase())
                        append(" ")
                        append(index + 1)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp,
                    ),
                )
                Text(
                    text = point.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                Text(
                    text = point.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    maxLines = 2,
                )
            }
        }
    }
}