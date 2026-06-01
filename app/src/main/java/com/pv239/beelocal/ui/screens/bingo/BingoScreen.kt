package com.pv239.beelocal.ui.screens.bingo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.pv239.beelocal.R
import com.pv239.beelocal.ui.screens.bingo.components.BingoGrid
import com.pv239.beelocal.ui.screens.bingo.components.BingoShareDialog

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
                        text = stringResource(R.string.bingo_no_card_available),
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
                    onDismissXpAward = viewModel::dismissXpAward,
                    onShowShareDialog = viewModel::showShareDialog,
                    onDismissShareDialog = viewModel::dismissShareDialog,
                    onShareToFeed = { description, photoUrls ->
                        viewModel.shareToFeed(description, photoUrls)
                    },
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
    onDismissXpAward: () -> Unit,
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

    // Bingo celebration dialog (also surfaces "+50 XP" / "+250 XP" feedback)
    AnimatedVisibility(
        visible = state.showBingoCelebration,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        Dialog(onDismissRequest = {
            onDismissCelebration()
            onDismissXpAward()
        }) {
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
                    Text(if (state.cardJustCompleted) "🏆" else "🎉", fontSize = 64.sp)
                    Text(
                        text = if (state.cardJustCompleted) "FULL CARD!" else "BINGO!",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = if (state.cardJustCompleted)
                            "You completed every cell on this week's bingo card!"
                        else
                            "You completed a row, column or diagonal!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    state.lastXpReward?.let { xp ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = "+$xp XP 🍯",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                    Button(onClick = {
                        onDismissCelebration()
                        onDismissXpAward()
                    }) {
                        Text("Awesome!")
                    }
                }
            }
        }
    }

    // Transient "+50 XP" toast for plain task completions (no bingo line yet).
    // Stacks at the top of the screen and auto-dismisses after ~2.5s so it
    // doesn't clobber the rest of the UI.
    val showFloatingXpToast = state.lastXpReward != null && !state.showBingoCelebration
    AnimatedVisibility(
        visible = showFloatingXpToast,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        val xp = state.lastXpReward ?: 0
        LaunchedEffect(xp) {
            kotlinx.coroutines.delay(2_500L)
            onDismissXpAward()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding() + 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shadowElevation = 6.dp,
            ) {
                Text(
                    text = "+$xp XP 🍯",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
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
            text = stringResource(R.string.bingo_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            text = stringResource(R.string.bingo_subtitle),
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
            text = stringResource(R.string.bingo_progress, completedCount, totalCount),
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
                    val completedLinesText = stringResource(
                        if (state.bingoLines.size == 1) {
                            R.string.bingo_lines_completed_single
                        } else {
                            R.string.bingo_lines_completed_plural
                        },
                        state.bingoLines.size,
                    )
                    Text(
                        text = completedLinesText,
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
                            text = stringResource(
                                if (state.sharedToFeed) {
                                    R.string.bingo_share_to_feed_done
                                } else {
                                    R.string.bingo_share_to_feed
                                }
                            ),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

