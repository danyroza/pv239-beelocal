package com.pv239.beelocal.ui.screens.social.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pv239.beelocal.R
import com.pv239.beelocal.model.FeedEntry
import com.pv239.beelocal.model.types.FeedEntryType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Polymorphic feed card that renders differently based on [FeedEntry.type]:
 *
 * - [FeedEntryType.DAILY_CHALLENGE]      — photo card with a "Daily Challenge" chip
 * - [FeedEntryType.ROUTE_COMPLETED]      — route trophy card with route name + city badge
 * - [FeedEntryType.BINGO_TASK_COMPLETED] — single bingo task card with grid icon
 * - [FeedEntryType.BINGO_COMPLETED]      — full bingo card completion banner
 *
 * All four types share the same header (avatar, username, timestamp) and
 * optionally display [FeedEntry.imageUrl] when present.
 */
@Composable
fun FeedCard(
    entry: FeedEntry,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            FeedCardHeader(entry)

            when (entry.type) {
                FeedEntryType.DAILY_CHALLENGE -> DailyChallengeContent(entry)
                FeedEntryType.ROUTE_COMPLETED -> RouteCompletionContent(entry)
                FeedEntryType.BINGO_TASK_COMPLETED -> BingoTaskContent(entry)
                FeedEntryType.BINGO_COMPLETED -> BingoCardCompletedContent(entry)
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Shared header
// ---------------------------------------------------------------------------

@Composable
private fun FeedCardHeader(entry: FeedEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
                imageUrl = entry.userProfileImageUrl,
                username = entry.username,
                size = 36,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = entry.username,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(entry.timestamp.toDate()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

// ---------------------------------------------------------------------------
// Daily Challenge content
// ---------------------------------------------------------------------------

@Composable
private fun DailyChallengeContent(entry: FeedEntry) {
    Column {
        // Type chip
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            EntryTypeChip(
                label = "Daily Challenge",
                iconRes = R.drawable.outline_photo_camera_24,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }

        if (entry.imageUrl.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            AsyncImage(
                model = entry.imageUrl,
                contentDescription = "Daily challenge photo by ${entry.username}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.medium),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Route Completion content
// ---------------------------------------------------------------------------

@Composable
private fun RouteCompletionContent(entry: FeedEntry) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Type chip
        EntryTypeChip(
            label = "Route Completed",
            iconRes = R.drawable.baseline_map_24,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )

        Spacer(Modifier.height(10.dp))

        // Route completion banner (trophy visual when no photo)
        if (entry.imageUrl.isNotBlank()) {
            AsyncImage(
                model = entry.imageUrl,
                contentDescription = "Route completion photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium),
            )
        } else {
            // Decorative placeholder banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_map_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "Finished a route!",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Route ID reference (in a real app you'd look this up and show the name)
        if (!entry.routeId.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Route: ${entry.routeId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Bingo Task content
// ---------------------------------------------------------------------------

@Composable
private fun BingoTaskContent(entry: FeedEntry) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Type chip
        EntryTypeChip(
            label = "Bingo Task",
            iconRes = R.drawable.baseline_grid_on_24,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )

        if (entry.imageUrl.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            AsyncImage(
                model = entry.imageUrl,
                contentDescription = "Bingo task photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(MaterialTheme.shapes.medium),
            )
        } else {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_grid_on_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Bingo task completed!",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Full Bingo Card Completion content
// ---------------------------------------------------------------------------

@Composable
private fun BingoCardCompletedContent(entry: FeedEntry) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        EntryTypeChip(
            label = "Bingo Card Completed",
            iconRes = R.drawable.baseline_grid_on_24,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )

        Spacer(Modifier.height(10.dp))

        if (entry.imageUrl.isNotBlank()) {
            AsyncImage(
                model = entry.imageUrl,
                contentDescription = "Completed bingo card photo by ${entry.username}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(MaterialTheme.shapes.medium),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_grid_on_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "Completed the whole bingo card!",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared chip
// ---------------------------------------------------------------------------

@Composable
private fun EntryTypeChip(
    label: String,
    iconRes: Int,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
            iconContentColor = contentColor,
        ),
        border = null,
        shape = RoundedCornerShape(percent = 50),
    )
}