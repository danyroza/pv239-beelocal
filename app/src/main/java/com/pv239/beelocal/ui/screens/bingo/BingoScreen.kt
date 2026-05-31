package com.pv239.beelocal.ui.screens.bingo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.pv239.beelocal.model.BingoTask
import com.pv239.beelocal.ui.components.rememberCameraLauncher

@Composable
fun BingoScreen(
    innerPadding: PaddingValues,
    viewModel: BingoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is BingoUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is BingoUiState.NoCardAvailable -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No bingo card available this week — check back soon! 🐝",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }

            is BingoUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }

            is BingoUiState.Ready -> {
                BingoContent(
                    state = state,
                    innerPadding = innerPadding,
                    onPhotoTaken = { taskId, uri -> viewModel.onPhotoTaken(taskId, uri) },
                    onCameraError = viewModel::reportCameraError,
                    onDismissCelebration = viewModel::dismissBingoCelebration,
                    onShowShareDialog = viewModel::showShareDialog,
                    onDismissShareDialog = viewModel::dismissShareDialog,
                    onShareToFeed = { description, photoUrls -> viewModel.shareToFeed(description, photoUrls) },
                )
            }
        }
    }
}

@Composable
private fun BingoContent(
    state: BingoUiState.Ready,
    innerPadding: PaddingValues,
    onPhotoTaken: (taskId: String, uri: android.net.Uri) -> Unit,
    onCameraError: (String) -> Unit,
    onDismissCelebration: () -> Unit,
    onShowShareDialog: () -> Unit,
    onDismissShareDialog: () -> Unit,
    onShareToFeed: (description: String, selectedPhotoUrls: List<String>) -> Unit,
) {
    var expandedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var expandedPhotoTitle by remember { mutableStateOf("") }

    // Full-screen photo viewer for completed cells
    expandedPhotoUrl?.let { url ->
        Dialog(
            onDismissRequest = { expandedPhotoUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { expandedPhotoUrl = null },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = expandedPhotoTitle,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = expandedPhotoTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }

    // Bingo share dialog
    if (state.showShareDialog) {
        BingoShareDialog(
            state = state,
            onDismiss = onDismissShareDialog,
            onShare = onShareToFeed,
        )

    }

    // Bingo celebration dialog
    AnimatedVisibility(
        visible = state.showBingoCelebration,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        Dialog(onDismissRequest = onDismissCelebration) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("🎉", fontSize = 64.sp)
                    Text(
                        text = "BINGO!",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "You completed a row, column or diagonal!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Button(onClick = onDismissCelebration) {
                        Text("Awesome!")
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
                start = 12.dp,
                end = 12.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Text(
            text = "Weekly Bingo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            text = "Tap a square to take a photo and complete the task. Get a row, column or diagonal for BINGO!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        // Bingo grid
        BingoGrid(
            state = state,
            onPhotoTaken = onPhotoTaken,
            onCameraError = onCameraError,
            onCompletedCellClick = { task ->
                val url = state.completedTaskPhotoUrls[task.id]
                if (url != null) {
                    expandedPhotoUrl = url
                    expandedPhotoTitle = task.title
                }
            },
        )

        // Progress indicator
        val completedCount = state.completedTaskIds.size
        val totalCount = state.card.tasks.size
        Text(
            text = "Completed: $completedCount / $totalCount",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        if (state.bingoLines.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "🎉 ${state.bingoLines.size} BINGO line${if (state.bingoLines.size > 1) "s" else ""} completed!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onShowShareDialog,
                        enabled = !state.sharedToFeed,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        ),
                    ) {
                        Text(
                            text = if (state.sharedToFeed) "Shared to feed ✓" else "Share to feed",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BingoGrid(
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
                    Text(
                        text = if (hasPhoto) "📷" else "✓",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
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

@Composable
private fun BingoShareDialog(
    state: BingoUiState.Ready,
    onDismiss: () -> Unit,
    onShare: (description: String, selectedPhotoUrls: List<String>) -> Unit,
) {
    var description by remember { mutableStateOf("") }
    val taskPhotos = remember(state.card.tasks, state.completedTaskPhotoUrls) {
        state.card.tasks.mapNotNull { task ->
            val url = state.completedTaskPhotoUrls[task.id]
            if (url != null) Pair(task.title, url) else null
        }
    }
    // Pre-select all completed photos; user can deselect any they don't want.
    var selectedPhotoUrls by remember(taskPhotos) {
        mutableStateOf(taskPhotos.map { it.second }.toSet())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Share your BINGO! 🎉",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Describe your bingo achievement…") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )

                if (taskPhotos.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Select photos to share:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = "${selectedPhotoUrls.size} / ${taskPhotos.size} selected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(taskPhotos) { (title, url) ->
                            val isSelected = url in selectedPhotoUrls
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .clickable {
                                        selectedPhotoUrls = if (isSelected) {
                                            selectedPhotoUrls - url
                                        } else {
                                            selectedPhotoUrls + url
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "✓",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "${state.bingoLines.size} BINGO line${if (state.bingoLines.size > 1) "s" else ""} • ${state.completedTaskIds.size} tasks completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onShare(description.trim(), selectedPhotoUrls.toList()) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Share", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
