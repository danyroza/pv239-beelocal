package com.pv239.beelocal.ui.screens.routes.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pv239.beelocal.R
import com.pv239.beelocal.model.Route

/**
 * Route card used on the routes list screen.
 *
 * Layout mirrors the home-screen RouteCard:
 * - Hero image at the top (with status badge overlay when present).
 * - Title, distance / time / rating label row, description, tags below.
 *
 * [routeStatus] drives the status badge (null = no badge).
 */
@Composable
fun RouteCard(
    modifier: Modifier = Modifier,
    route: Route,
    onClick: () -> Unit,
    onImageClick: (() -> Unit)? = null,
    routeStatus: RouteStatus? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column {
            // ── Hero image ───────────────────────────────────────────────────
            Box {
                AsyncImage(
                    model = route.imageUrl.takeUnless { it.isNullOrBlank() }
                        ?: R.drawable.kyoto, // fallback placeholder
                    contentDescription = route.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .then(
                            if (onImageClick != null) {
                                Modifier.clickable { onImageClick() }
                            } else {
                                Modifier
                            }
                        ),
                    contentScale = ContentScale.Crop,
                )
                // Status badge overlaid on top-right of the image
                routeStatus?.let {
                    StatusBadge(
                        status = it,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                // ── Title ─────────────────────────────────────────────────────
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // ── Distance / time / rating row ──────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val distanceLabel = when {
                        route.distanceMeters > 0 -> formatDistance(route.distanceMeters)
                        else                     -> "${route.points.size * 700}m"
                    }
                    val durationLabel = when {
                        route.estimatedDurationMinutes > 0 ->
                            formatDuration(route.estimatedDurationMinutes)
                        else -> "${route.points.size * 10} min"
                    }
                    val checkpointsLabel = "${route.points.size}"

                    Label(
                        icon = R.drawable.outline_route_24,
                        text = distanceLabel,
                    )
                    Label(
                        icon = R.drawable.outline_access_time_24,
                        text = durationLabel,
                    )
                    Label(
                        icon = R.drawable.not_listed_location_24px,
                        text = checkpointsLabel,
                    )
                    if (route.averageRating > 0f) {
                        Label(
                            icon = R.drawable.baseline_star_24,
                            text = "%.1f".format(route.averageRating),
                            tint = Color(0xFFFFB800),
                        )
                    } else {
                        Label(
                            icon = R.drawable.baseline_star_24,
                            text = stringResource(R.string.routes_rating_new),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Description ───────────────────────────────────────────────
                Text(
                    text = route.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // ── Tags ──────────────────────────────────────────────────────
                if (route.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        route.tags.take(3).forEach { tag -> TagChip(tag) }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Supporting composables (mirrors home RouteCard helpers)
// ─────────────────────────────────────────────────────────────────────────────

enum class RouteStatus { IN_PROGRESS, COMPLETED }

@Composable
private fun StatusBadge(status: RouteStatus, modifier: Modifier = Modifier) {
    val (label, containerColor, contentColor) = when (status) {
        RouteStatus.IN_PROGRESS -> Triple(
            stringResource(R.string.routes_in_progress),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        RouteStatus.COMPLETED -> Triple(
            stringResource(R.string.routes_completed),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = contentColor,
            ),
        )
    }
}

@Composable
private fun Label(
    icon: Int,
    text: String,
    tint: Color = LocalContentColor.current,
) {
    Row(
        modifier = Modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tint,
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun TagChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Formatting helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatDistance(metres: Int): String = when {
    metres >= 1000 -> "%.1f km".format(metres / 1000f)
    else           -> "${metres}m"
}

private fun formatDuration(minutes: Int): String = when {
    minutes >= 60 -> {
        val h = minutes / 60
        val m = minutes % 60
        if (m == 0) "${h}h" else "${h}h ${m}min"
    }
    else -> "${minutes} min"
}