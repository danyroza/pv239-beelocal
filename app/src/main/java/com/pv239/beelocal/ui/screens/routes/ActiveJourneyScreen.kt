package com.pv239.beelocal.ui.screens.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pv239.beelocal.R
import com.pv239.beelocal.ui.screens.routes.components.AnswerInputSection
import com.pv239.beelocal.ui.screens.routes.components.JourneyProgressBar
import com.pv239.beelocal.ui.screens.routes.components.RiddleCard
import com.pv239.beelocal.ui.screens.routes.components.RouteMapView

/**
 * Active journey screen — shows one checkpoint at a time.
 *
 * Navigation:
 * - The toolbar back arrow exits the journey back to the detail screen
 *   ([onBack]) without losing any progress.
 * - The in-row left arrow navigates to the *previous checkpoint* within the
 *   journey; the answer field is pre-filled with the previously submitted
 *   answer so the user can review it.
 * - After the last point [onRouteCompleted] is called.
 *
 * Progress is persisted to Firestore on every correct answer, so exiting and
 * returning via the detail screen always resumes from where the user left off.
 */
@Composable
fun ActiveJourneyScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onRouteCompleted: () -> Unit,
    viewModel: RouteViewModel = hiltViewModel(),
) {
    val state by viewModel.journeyState.collectAsStateWithLifecycle()
    val completionState by viewModel.completionState.collectAsStateWithLifecycle()

    // Navigate to completion screen once the route is marked done.
    LaunchedEffect(completionState.completion) {
        if (completionState.completion != null && !completionState.isDone) onRouteCompleted()
    }

    val point = state.currentPoint
    val route = state.route

    if (route == null || point == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isCurrentPointAlreadyCompleted = state.currentPointIndex in state.completedPointIndices

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 160.dp,
            ),
        ) {
            // ── Top bar: exit + checkpoint navigation + progress ─────────────
            item {
                JourneyProgressBar(
                    currentIndex = state.currentPointIndex,
                    total = state.totalPoints,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                )
            }

            // ── Live route map ───────────────────────────────────────────────
            item {
                RouteMapView(
                    points = route.points,
                    completedPointIndices = state.completedPointIndices,
                    currentPointIndex = state.currentPointIndex,
                    userLatLng = state.userLatLng,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    height = 200.dp,
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Checkpoint content ───────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = stringResource(R.string.journey_active_mission),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp,
                        ),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.journey_checkpoint_label,
                            state.currentPointIndex + 1,
                            point.name,
                        ),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp,
                        ),
                    )
                    Text(
                        text = point.description,
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    RiddleCard(question = point.quizQuestion.ifBlank { point.description })

                    Spacer(modifier = Modifier.height(20.dp))

                    when {
                        // Checkpoint already done — show "visited" note and let the
                        // user advance forward again.
                        isCurrentPointAlreadyCompleted -> {
                            AlreadyCompletedBanner(
                                isLast = state.isLastPoint,
                                onContinue = viewModel::advanceToNextPoint,
                                isLoading = state.isLoading,
                                onPrevious = viewModel::goToPreviousPoint,
                                previousEnabled = state.currentPointIndex > 0,
                            )
                        }

                        // Awaiting correct answer
                        state.answerResult != true -> {
                            AnswerInputSection(
                                value = state.answerInput,
                                onValueChange = viewModel::onAnswerInputChanged,
                                onSubmit = viewModel::checkAnswer,
                                answerResult = state.answerResult,
                                isLoading = state.isCheckingAnswer,
                            )
                        }

                        // Just answered correctly for the first time
                        else -> {
                            CorrectAnswerBanner(
                                isLast = state.isLastPoint,
                                onContinue = viewModel::advanceToNextPoint,
                                isLoading = state.isLoading,
                                onPrevious = viewModel::goToPreviousPoint,
                                previousEnabled = state.currentPointIndex > 0,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CorrectAnswerBanner(
    isLast: Boolean,
    onContinue: () -> Unit,
    isLoading: Boolean,
    onPrevious: () -> Unit,
    previousEnabled: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.journey_answer_correct),
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))
        CheckpointNavigationRow(
            label = if (isLast) stringResource(R.string.journey_finish)
            else stringResource(R.string.journey_next_checkpoint),
            onPrevious = onPrevious,
            previousEnabled = previousEnabled,
            onContinue = onContinue,
            continueEnabled = true,
            isLoading = isLoading,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
    }
}

@Composable
private fun AlreadyCompletedBanner(
    isLast: Boolean,
    onContinue: () -> Unit,
    isLoading: Boolean,
    onPrevious: () -> Unit,
    previousEnabled: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "✓ ${stringResource(R.string.journey_checkpoint_already_done)}",
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))
        CheckpointNavigationRow(
            label = if (isLast) stringResource(R.string.journey_finish)
            else stringResource(R.string.journey_next_checkpoint),
            onPrevious = onPrevious,
            previousEnabled = previousEnabled,
            onContinue = onContinue,
            continueEnabled = true,
            isLoading = isLoading,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
    }
}

@Composable
private fun AdvanceButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    containerColor: Color,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading && enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.baseline_arrow_forward_24),
                contentDescription = null
            )
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun CheckpointNavigationRow(
    label: String,
    onPrevious: () -> Unit,
    previousEnabled: Boolean,
    onContinue: () -> Unit,
    continueEnabled: Boolean,
    isLoading: Boolean,
    containerColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Button(
            onClick = onPrevious,
            enabled = previousEnabled,
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.outline_arrow_back_24),
                contentDescription = null
            )
        }

        AdvanceButton(
            modifier = Modifier.weight(4f),
            label = label,
            onClick = onContinue,
            enabled = continueEnabled,
            isLoading = isLoading,
            containerColor = containerColor,
        )
    }
}
