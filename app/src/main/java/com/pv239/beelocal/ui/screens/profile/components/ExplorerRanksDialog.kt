package com.pv239.beelocal.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.R

/**
 * Modal that explains the 8-tier XP ladder.
 *
 * Layout:
 *  - Header progress block showing the current rank, XP toward the next
 *    rank, and a linear progress bar tinted in the next rank's colour.
 *  - Scrollable list of every rank with its coloured swatch, name, and XP
 *    threshold. The user's current tier is outlined and tinted so the
 *    dialog doubles as a "where am I" indicator even when scrolled.
 *
 * The list is intentionally height-constrained + scrollable so the dialog
 * stays well-mannered on smaller phones — `AlertDialog`'s text slot would
 * otherwise push past the screen bounds once new ranks are added.
 */
@Composable
fun ExplorerRanksDialog(
    currentXp: Int,
    onDismiss: () -> Unit,
) {
    val currentRank = rankForXp(currentXp)
    val upcoming = nextRank(currentRank)
    // List in ascending order so the player sees their journey from larva
    // upwards rather than from end-game down.
    val ranks = ExplorerRank.entries.sortedBy { it.minXp }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.profile_ranks_dialog_title),
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.profile_ranks_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ProgressHeader(
                    currentXp = currentXp,
                    currentRank = currentRank,
                    nextRank = upcoming,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Constrain the list height so the dialog never overflows
                // on small screens. heightIn(max = ...) lets it shrink to
                // fit short lists (e.g. when fewer ranks are added later).
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ranks.forEach { rank ->
                        RankRow(
                            rank = rank,
                            isCurrent = rank == currentRank,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.profile_ranks_dialog_close),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
    )
}

/**
 * Compact progress card sitting above the rank list. Shows "Current rank
 * → Next rank", the XP delta to the next rank, and a linear progress bar
 * tinted in the next rank's colour. When the user is already at the top
 * tier the bar fills and a max-rank caption is shown instead of XP-to-go.
 */
@Composable
private fun ProgressHeader(
    currentXp: Int,
    currentRank: ExplorerRank,
    nextRank: ExplorerRank?,
) {
    // Compute progress within the [currentRank.minXp, nextRank.minXp]
    // window. If we're already at the top we just fill the bar.
    val progress: Float
    val xpRemaining: Int
    if (nextRank != null) {
        val span = (nextRank.minXp - currentRank.minXp).coerceAtLeast(1)
        val within = (currentXp - currentRank.minXp).coerceAtLeast(0)
        progress = (within.toFloat() / span.toFloat()).coerceIn(0f, 1f)
        xpRemaining = (nextRank.minXp - currentXp).coerceAtLeast(0)
    } else {
        progress = 1f
        xpRemaining = 0
    }

    // When at the max rank we tint the bar in the current rank's colour
    // (no "next" to point to). Otherwise use the next rank's colour as a
    // visual carrot that hints at what the user is climbing toward.
    val accent = nextRank?.color ?: currentRank.color

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RankBadge(rank = currentRank)
                if (nextRank != null) {
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RankBadge(rank = nextRank, faded = true)
                } else {
                    Text(
                        text = stringResource(R.string.profile_ranks_dialog_max_rank),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = currentRank.color,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = accent,
                trackColor = accent.copy(alpha = 0.18f),
            )

            val captionText = if (nextRank != null) {
                stringResource(
                    R.string.profile_ranks_dialog_progress_caption,
                    currentXp,
                    nextRank.minXp,
                    xpRemaining,
                )
            } else {
                stringResource(
                    R.string.profile_ranks_dialog_progress_caption_max,
                    currentXp,
                )
            }
            Text(
                text = captionText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RankBadge(rank: ExplorerRank, faded: Boolean = false) {
    val tint = if (faded) rank.color.copy(alpha = 0.75f) else rank.color
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(tint),
        )
        Text(
            text = stringResource(rank.labelRes),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
    }
}

@Composable
private fun RankRow(
    rank: ExplorerRank,
    isCurrent: Boolean,
) {
    // Highlight the current rank with a tinted background and a thin outline
    // in the rank's own colour so it pops without overwhelming the list.
    val containerColor = if (isCurrent) {
        rank.color.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrent) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = rank.color,
                        shape = RoundedCornerShape(14.dp),
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            ),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(rank.color),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(rank.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = rank.color,
                )
                Text(
                    text = stringResource(
                        R.string.profile_ranks_dialog_threshold,
                        rank.minXp,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isCurrent) {
                Text(
                    text = stringResource(R.string.profile_ranks_dialog_you),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = rank.color,
                )
            }
        }
    }
}
