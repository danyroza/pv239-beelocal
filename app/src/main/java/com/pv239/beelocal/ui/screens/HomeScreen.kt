package com.pv239.beelocal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.R
import com.pv239.beelocal.ui.components.DailyChallengeSection
import com.pv239.beelocal.ui.components.TrendingRoutesSection

@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    onDailyChallengeClick: () -> Unit,
    // TODO: derive from ViewModel
    challengeTimeRemaining: String = stringResource(R.string.home_challenge_time_remaining_default),
    challengeProximityLabel: String? = null,
    challengeIsCompleted: Boolean = false,
) {
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
            DailyChallengeSection(
                timeRemaining = challengeTimeRemaining,
                proximityLabel = challengeProximityLabel,
                isCompleted = challengeIsCompleted,
                onClick = onDailyChallengeClick
            )
        }

        item { TrendingRoutesSection() }
    }
}