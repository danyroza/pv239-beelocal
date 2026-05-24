package com.pv239.beelocal.ui.screens.dailychallenge

import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pv239.beelocal.R

@Composable
fun DailyChallengeScreen(
    innerPadding: PaddingValues,
    viewModel: DailyChallengeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Start receiving GPS fixes and forward them to the ViewModel.
    LocationUpdatesEffect(
        onLocationUpdate = viewModel::onLocationUpdate,
    )

    when (val state = uiState) {
        is DailyChallengeUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is DailyChallengeUiState.NoChallengeToday -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.daily_challenge_no_challenge_today),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }

        is DailyChallengeUiState.Error -> {
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

        is DailyChallengeUiState.Ready -> {
            DailyChallengeContent(
                state = state,
                innerPadding = innerPadding,
                onPhotoTaken = { bitmap ->
                    val streakCount =
                        (state.completion as? CompletionState.Completed)?.streakCount ?: 0
                    viewModel.submitPhoto(bitmap, streakCount)
                },
                onShareToFeed = viewModel::shareToFeed,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Location side-effect — requests GPS updates while the screen is visible
// ---------------------------------------------------------------------------

@Composable
private fun LocationUpdatesEffect(
    onLocationUpdate: (Location) -> Unit,
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5_000L
        ).setMinUpdateDistanceMeters(5f).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(onLocationUpdate)
            }
        }

        try {
            client.requestLocationUpdates(request, callback, null)
        } catch (_: SecurityException) {
            // Fine location permission not granted — the screen degrades gracefully
            // (distanceMeters stays null, proximity card shows "Searching…")
        }

        onDispose { client.removeLocationUpdates(callback) }
    }
}