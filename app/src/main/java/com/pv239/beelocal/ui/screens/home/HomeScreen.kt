package com.pv239.beelocal.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pv239.beelocal.R
import com.pv239.beelocal.ui.screens.home.components.DailyChallengeSection
import com.pv239.beelocal.ui.components.TimeRemainingBadge
import com.pv239.beelocal.ui.screens.home.components.TrendingRoutesSection
import com.pv239.beelocal.ui.screens.dailychallenge.CompletionState
import com.pv239.beelocal.ui.screens.dailychallenge.DailyChallengeUiState
import com.pv239.beelocal.ui.screens.dailychallenge.DailyChallengeViewModel

@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    onDailyChallengeClick: () -> Unit,
    viewModel: DailyChallengeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 16.dp,
            bottom = 100.dp,
            start = 20.dp,
            end = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {

            Column(modifier = Modifier.fillMaxWidth()) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.daily_challenge_section_title),
                        style = MaterialTheme.typography.displaySmall
                    )

                    if (uiState is DailyChallengeUiState.Ready) {
                        val state = uiState as DailyChallengeUiState.Ready

                        TimeRemainingBadge(
                            secondsRemaining = state.secondsRemaining
                        )
                    }
                }

                Text(
                    text = when (uiState) {
                        is DailyChallengeUiState.Ready -> {
                            val state = uiState as DailyChallengeUiState.Ready

                            if (state.completion is CompletionState.Completed) {
                                stringResource(R.string.daily_challenge_section_subtitle_completed)
                            } else {
                                stringResource(R.string.daily_challenge_section_subtitle_pending)
                            }
                        }

                        else -> stringResource(R.string.daily_challenge_section_subtitle_pending)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (val state = uiState) {

                    is DailyChallengeUiState.Loading -> {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is DailyChallengeUiState.NoChallengeToday -> {

                        Text(
                            text = stringResource(R.string.daily_challenge_no_challenge_today),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    is DailyChallengeUiState.Error -> {

                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    is DailyChallengeUiState.Ready -> {

                        val isCompleted =
                            state.completion is CompletionState.Completed

                        val completedPhotoUrl =
                            (state.completion as? CompletionState.Completed)?.photoUrl

                        DailyChallengeSection(
                            challenge = state.challenge,
                            completedPhotoUrl = completedPhotoUrl,
                            secondsRemaining = state.secondsRemaining,
                            proximityLabel = null,
                            isCompleted = isCompleted,
                            onClick = onDailyChallengeClick,
                        )
                    }
                }
            }
        }
        item { TrendingRoutesSection() }
    }
}