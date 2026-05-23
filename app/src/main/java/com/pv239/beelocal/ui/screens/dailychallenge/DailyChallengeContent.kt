package com.pv239.beelocal.ui.screens.dailychallenge

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.pv239.beelocal.R
import com.pv239.beelocal.ui.screens.dailychallenge.components.CompletedSection
import com.pv239.beelocal.ui.screens.dailychallenge.components.MapPlaceholder
import com.pv239.beelocal.ui.screens.dailychallenge.components.ProximityCard
import com.pv239.beelocal.ui.screens.dailychallenge.components.ProximityTemperature
import com.pv239.beelocal.ui.screens.dailychallenge.components.TemperatureLegend

@Composable
fun DailyChallengeContent(
    state: DailyChallengeUiState.Ready,
    innerPadding: PaddingValues,
    onPhotoTaken: (Bitmap) -> Unit,
    onShareToFeed: () -> Unit,
) {
    val context = LocalContext.current

    val proximity = state.distanceMeters?.let { ProximityTemperature.fromDistance(it) }
    val isCompleted = state.completion is CompletionState.Completed
    val isSubmitting = state.completion is CompletionState.Submitting

    var showLegend by remember { mutableStateOf(false) }
    var showExpandedPhoto by remember { mutableStateOf(false) }

    val canShowNotifications = remember {
        val initialState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        mutableStateOf(initialState)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) onPhotoTaken(bitmap)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            canShowNotifications.value = true
            cameraLauncher.launch(null)
        }
        // TODO: show a Snackbar when denied
    }

    val canSubmit = proximity != null && proximity.maxMeters <= ProximityTemperature.HOT.maxMeters

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 160.dp,
            ),
        ) {

            // ── Hero photo ───────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clickable { showExpandedPhoto = true }) {
                    AsyncImage(
                        model = state.challenge.imageUrl,
                        contentDescription = stringResource(R.string.daily_challenge_hero_image_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = painterResource(id = R.drawable.outline_map_24),
                        error = painterResource(id = R.drawable.outline_map_24),
                    )

                    // Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.25f),
                                    1f to Color.Black.copy(alpha = 0.55f)
                                )
                            )
                    )

                    // Time remaining badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.daily_challenge_time_remaining_prefix,
                                state.secondsRemaining.toHoursMinutes()
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Title overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.daily_challenge_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = TextUnit(2f, TextUnitType.Sp),
                        )
                        Text(
                            text = if (state.challenge.cityName.isNotBlank())
                                stringResource(R.string.daily_challenge_title_with_city, state.challenge.cityName)
                            else
                                stringResource(R.string.daily_challenge_title_generic),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }

            // ── Body ─────────────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {

                    if (isCompleted) {
                        val completed = state.completion
                        CompletedSection(
                            photoUrl = completed.photoUrl,
                            streakCount = completed.streakCount,
                            sharedToFeed = completed.sharedToFeed,
                            onShareToFeed = onShareToFeed,
                        )
                    } else {
                        if (isSubmitting) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            ProximityCard(
                                proximity = proximity,
                                distanceMeters = state.distanceMeters,
                                onToggleLegend = { showLegend = !showLegend },
                            )
                        }

                        AnimatedVisibility(visible = showLegend && !isSubmitting) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.daily_challenge_proximity_guide_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TemperatureLegend(currentTemperature = proximity)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.daily_challenge_approximate_location_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.daily_challenge_approximate_location_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        MapPlaceholder()
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        // ── FAB ──────────────────────────────────────────────────────────────
        if (!isCompleted && !isSubmitting) {
            val fabContainerColor = if (canSubmit) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer
            val fabContentColor = if (canSubmit) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
            val fabIcon = if (canSubmit) R.drawable.outline_photo_camera_24
            else R.drawable.outline_no_photography_24

            ExtendedFloatingActionButton(
                onClick = {
                    if (canSubmit) {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCameraPermission) cameraLauncher.launch(null)
                        else permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 100.dp),
                containerColor = fabContainerColor,
                contentColor = fabContentColor,
                icon = {
                    Icon(
                        painter = painterResource(id = fabIcon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = {
                    Text(
                        text = if (canSubmit) stringResource(R.string.daily_challenge_fab_take_photo)
                        else stringResource(R.string.daily_challenge_fab_too_far),
                        fontWeight = FontWeight.Bold
                    )
                },
            )
        }
    }

    // ── Expanded photo dialog ─────────────────────────────────────────────
    if (showExpandedPhoto) {
        Dialog(
            onDismissRequest = { showExpandedPhoto = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showExpandedPhoto = false },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = state.challenge.imageUrl,
                    contentDescription = stringResource(R.string.daily_challenge_expanded_image_description),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}