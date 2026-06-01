package com.pv239.beelocal.ui.screens.bingo.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.R
import com.pv239.beelocal.model.BingoTask
import com.pv239.beelocal.ui.components.rememberCameraLauncher
import com.pv239.beelocal.ui.screens.bingo.BingoUiState


@Composable
fun BingoGrid(
    state: BingoUiState.Ready,
    onPhotoTaken: (taskId: String, uri: android.net.Uri) -> Unit,
    onCameraError: (String) -> Unit,
    onCompletedCellClick: (BingoTask) -> Unit,
) {
    // One camera launcher per active cell tap — we use a single launcher for
    // the whole grid and track which taskId was tapped.
    var pendingTaskId by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberCameraLauncher(
        onPhotoTaken = { uri ->
            pendingTaskId?.let { onPhotoTaken(it, uri) }
            pendingTaskId = null
        },
        onCameraError = onCameraError,
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        state.grid.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEachIndexed { colIndex, task ->
                    val isCompleted = state.isCompleted(task.id)
                    val isSubmitting = state.submittingTaskId == task.id

                    // A cell belongs to a winning line if any of its row/col/diagonal indices
                    // is in bingoLines.
                    val inWinningLine = run {
                        val rowLine = rowIndex
                        val colLine = 4 + colIndex
                        val mainDiag = if (rowIndex == colIndex) 8 else -1
                        val antiDiag = if (rowIndex + colIndex == 3) 9 else -1
                        state.bingoLines.any { it == rowLine || it == colLine || it == mainDiag || it == antiDiag }
                    }

                    BingoCell(
                        task = task,
                        isCompleted = isCompleted,
                        isSubmitting = isSubmitting,
                        inWinningLine = inWinningLine,
                        hasPhoto = state.completedTaskPhotoUrls.containsKey(task.id),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isCompleted) {
                                onCompletedCellClick(task)
                            } else if (!isSubmitting && state.submittingTaskId == null) {
                                pendingTaskId = task.id
                                cameraLauncher.launchCamera()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BingoCell(
    task: BingoTask,
    isCompleted: Boolean,
    isSubmitting: Boolean,
    inWinningLine: Boolean,
    hasPhoto: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 1f,
        label = "cell_scale",
    )

    val backgroundColor = when {
        inWinningLine && isCompleted -> MaterialTheme.colorScheme.primary
        isCompleted -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        inWinningLine && isCompleted -> MaterialTheme.colorScheme.onPrimary
        isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = if (inWinningLine && isCompleted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (inWinningLine && isCompleted) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(6.dp),
            ) {
                if (isCompleted) {
                    Icon(
                        painter = painterResource(if (hasPhoto) R.drawable.local_see_24px else R.drawable.baseline_check_24),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
