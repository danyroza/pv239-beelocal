package com.pv239.beelocal.ui.screens.social.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    /**
     * Invoked when the user taps the header (avatar / username) to open the
     * author's public profile. Defaults to a no-op so the card is still safe
     * to render in previews or contexts that don't support navigation.
     */
    onAuthorClick: () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            FeedCardHeader(entry = entry, onAuthorClick = onAuthorClick)

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
private fun FeedCardHeader(entry: FeedEntry, onAuthorClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Wrap the avatar + username in a clickable row so taps anywhere on
        // the author identity open the public profile screen.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onAuthorClick),
        ) {
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
            text = SimpleDateFormat(
                "dd MMM",
                Locale.getDefault()
            ).format(entry.timestamp.toDate()),
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
    val imageUrls = entry.feedImageUrls()

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

        if (imageUrls.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FeedImageGallery(
                imageUrls = imageUrls,
                contentDescription = "Daily challenge photo by ${entry.username}",
                modifier = Modifier.padding(horizontal = 16.dp),
                aspectRatio = 4f / 3f,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Route Completion content
// ---------------------------------------------------------------------------

@Composable
private fun RouteCompletionContent(entry: FeedEntry) {
    val imageUrls = entry.feedImageUrls()

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
        if (imageUrls.isNotEmpty()) {
            FeedImageGallery(
                imageUrls = imageUrls,
                contentDescription = "Route completion photo",
                aspectRatio = 16f / 9f,
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
    val imageUrls = entry.feedImageUrls()

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Type chip
        EntryTypeChip(
            label = "Bingo Task",
            iconRes = R.drawable.baseline_grid_on_24,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )

        if (imageUrls.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FeedImageGallery(
                imageUrls = imageUrls,
                contentDescription = "Bingo task photo",
                aspectRatio = 4f / 3f,
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
    val imageUrls = entry.feedImageUrls()

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        EntryTypeChip(
            label = "Bingo Card Completed",
            iconRes = R.drawable.baseline_grid_on_24,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )

        Spacer(Modifier.height(10.dp))

        if (imageUrls.isNotEmpty()) {
            FeedImageGallery(
                imageUrls = imageUrls,
                contentDescription = "Completed bingo card photo by ${entry.username}",
                aspectRatio = 4f / 3f,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedImageGallery(
    imageUrls: List<String>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    aspectRatio: Float,
) {
    var expandedImageIndex by rememberSaveable(imageUrls) { mutableIntStateOf(-1) }
    var showExpandedPhoto by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })

    Column(modifier = modifier.fillMaxWidth()) {
        Box {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(MaterialTheme.shapes.medium),
            ) { page ->
                AsyncImage(
                    model = imageUrls[page],
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            expandedImageIndex = page
                            showExpandedPhoto = true
                        },
                )
            }

            if (imageUrls.size > 1) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${imageUrls.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                    )
                }
            }
        }

        if (imageUrls.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                imageUrls.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (index == pagerState.currentPage) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                }
                            ),
                    )
                }
            }
        }
    }

    if (showExpandedPhoto) {
        ExpandedFeedImageGallery(
            imageUrls = imageUrls,
            initialPage = expandedImageIndex,
            contentDescription = contentDescription,
            onDismiss = { showExpandedPhoto = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpandedFeedImageGallery(
    imageUrls: List<String>,
    initialPage: Int,
    contentDescription: String,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { imageUrls.size },
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = imageUrls[page],
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDismiss() },
                    )
                }
            }

            Text(
                text = "${pagerState.currentPage + 1}/${imageUrls.size}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
            )

            Text(
                text = "Close",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .clickable(onClick = onDismiss),
            )
        }
    }
}

private fun FeedEntry.feedImageUrls(): List<String> =
    rememberFeedImageUrls(imageUrls, imageUrl)

private fun rememberFeedImageUrls(
    imageUrls: List<String>,
    fallbackImageUrl: String
): List<String> {
    val combined = buildList {
        addAll(imageUrls.filter { it.isNotBlank() })
        if (fallbackImageUrl.isNotBlank() && fallbackImageUrl !in this) {
            add(fallbackImageUrl)
        }
    }
    return combined
}

// ---------------------------------------------------------------------------
// Shared chip
// ---------------------------------------------------------------------------

@Composable
private fun EntryTypeChip(
    label: String,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
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
