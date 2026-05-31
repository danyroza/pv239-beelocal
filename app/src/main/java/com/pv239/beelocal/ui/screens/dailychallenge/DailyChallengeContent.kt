package com.pv239.beelocal.ui.screens.dailychallenge

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
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
import com.pv239.beelocal.ui.components.TimeRemainingBadge
import com.pv239.beelocal.ui.screens.dailychallenge.components.ChallengeMapView
import com.pv239.beelocal.ui.screens.dailychallenge.components.CompletedSection
import com.pv239.beelocal.ui.screens.dailychallenge.components.ProximityCard
import com.pv239.beelocal.ui.screens.dailychallenge.components.ProximityTemperature
import com.pv239.beelocal.ui.screens.dailychallenge.components.TemperatureLegend

@Composable
fun DailyChallengeContent(
    state: DailyChallengeUiState.Ready,
    innerPadding: PaddingValues,
    onPhotoTaken: (Uri) -> Unit,
    onShareToFeed: () -> Unit,
    onCameraError: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val cameraFileErrorMessage = stringResource(R.string.daily_challenge_camera_file_error)
    val cameraUriErrorMessage = stringResource(R.string.daily_challenge_camera_uri_error)
    val cameraLaunchErrorMessage = stringResource(R.string.daily_challenge_camera_launch_error)

    val proximity = state.distanceMeters?.let { ProximityTemperature.fromDistance(it) }
    val isCompleted = state.completion is CompletionState.Completed
    val isSubmitting = state.completion is CompletionState.Submitting
    val isFailed = state.completion is CompletionState.SubmissionFailed

    var showLegend by remember { mutableStateOf(false) }
    var showExpandedPhoto by remember { mutableStateOf(false) }

    // Create a temp file URI for the camera to write to
    var photoUri by remember { mutableStateOf<Uri>(Uri.EMPTY) }
    // Guard flag to prevent the launcher callback from firing twice on recomposition
    var awaitingCameraResult by remember { mutableStateOf(false) }

    /**
     * Tries to create a temp file in the app cache and wrap it with a FileProvider URI.
     * Either step can throw (IOException from createTempFile, IllegalArgumentException
     * from FileProvider, SecurityException, ...) so we return null on failure and let
     * the caller surface an error UI instead of crashing.
     */
    fun createTempPhotoUri(): Uri? {
        val photoFile = runCatching {
            val photoDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            File.createTempFile("challenge_", ".jpg", photoDir)
        }.getOrElse { e ->
            Log.e("DailyChallengeContent", "Failed to create temp photo file", e)
            onCameraError(cameraFileErrorMessage)
            return null
        }

        return runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile,
            )
        }.getOrElse { e ->
            Log.e("DailyChallengeContent", "Failed to create FileProvider URI", e)
            onCameraError(cameraUriErrorMessage)
            null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && awaitingCameraResult) {
            awaitingCameraResult = false
            onPhotoTaken(photoUri)
        } else {
            // Capture was canceled or failed: clean up the pre-created temp file
            // referenced by photoUri so repeated cancels don't bloat the cache.
            if (photoUri != Uri.EMPTY) {
                runCatching {
                    context.contentResolver.delete(photoUri, null, null)
                }.onFailure { e ->
                    Log.w("DailyChallengeContent", "Failed to delete orphan temp photo", e)
                }
                photoUri = Uri.EMPTY
            }
            awaitingCameraResult = false
        }
    }

    fun launchCameraWithUri() {
        val uri = createTempPhotoUri() ?: run {
            // Failure already reported via onCameraError inside createTempPhotoUri.
            awaitingCameraResult = false
            return
        }
        photoUri = uri
        awaitingCameraResult = true
        runCatching {
            cameraLauncher.launch(uri)
        }.onFailure { e ->
            Log.e("DailyChallengeContent", "Failed to launch camera", e)
            awaitingCameraResult = false
            onCameraError(cameraLaunchErrorMessage)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraWithUri()
        }
    }

    fun launchCamera() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            launchCameraWithUri()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
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
                    TimeRemainingBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        secondsRemaining = state.secondsRemaining,
                    )

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
                            text = if (state.challenge.cityName.isNotBlank()) stringResource(
                                R.string.daily_challenge_title_with_city,
                                state.challenge.cityName
                            )
                            else stringResource(R.string.daily_challenge_title_generic),
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
                        } else if (isFailed) {
                            val failedState = state.completion
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = stringResource(R.string.daily_challenge_submission_failed_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = failedState.errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                    )
                                }
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
                        ChallengeMapView(
                            challengeLocation = state.challenge.location,
                            challengeId = state.challenge.id,
                            userLatLng = state.userLatLng,
                        )
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
                    if (canSubmit) launchCamera()
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
                        text = when {
                            isFailed && canSubmit -> stringResource(R.string.daily_challenge_submission_failed_retry)
                            canSubmit -> stringResource(R.string.daily_challenge_fab_take_photo)
                            else -> stringResource(R.string.daily_challenge_fab_too_far)
                        },
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
