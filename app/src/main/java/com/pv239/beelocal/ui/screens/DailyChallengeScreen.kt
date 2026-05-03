package com.pv239.beelocal.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pv239.beelocal.R
import com.pv239.beelocal.ui.viewmodel.CompletionState
import com.pv239.beelocal.ui.viewmodel.DailyChallengeUiState
import com.pv239.beelocal.ui.viewmodel.DailyChallengeViewModel
import com.pv239.beelocal.ui.viewmodel.toHoursMinutes

// ---------------------------------------------------------------------------
// Domain model (unchanged)
// ---------------------------------------------------------------------------

enum class ProximityTemperature(
    val label: String,
    val emoji: String,
    val description: String,
    val color: Color,
    val maxMeters: Int,
) {
    BOILING("Boiling!", "🌋", "< 10 m", Color(0xFFFF3D00), 10), HOT(
        "Hot!",
        "🔥",
        "< 50 m",
        Color(0xFFFF6D00),
        50
    ),
    WARM("Warm", "☀️", "< 150 m", Color(0xFFFFAB00), 150), LUKEWARM(
        "Lukewarm",
        "🌤️",
        "< 300 m",
        Color(0xFFFFD740),
        300
    ),
    COOL("Cool", "💨", "< 500 m", Color(0xFF80DEEA), 500), COLD(
        "Cold",
        "🥶",
        "< 1 000 m",
        Color(0xFF42A5F5),
        1000
    ),
    FREEZING("Freezing!", "🧊", "> 1 000 m", Color(0xFF90CAF9), Int.MAX_VALUE);

    companion object {
        fun fromDistance(meters: Int): ProximityTemperature =
            entries.first { meters <= it.maxMeters }
    }
}

// ---------------------------------------------------------------------------
// Screen entry point
// ---------------------------------------------------------------------------

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
                    text = "No challenge today — check back tomorrow! 🐝",
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
// Main content (only rendered when state is Ready)
// ---------------------------------------------------------------------------

@Composable
private fun DailyChallengeContent(
    state: DailyChallengeUiState.Ready,
    innerPadding: PaddingValues,
    onPhotoTaken: (android.graphics.Bitmap) -> Unit,
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
                        contentDescription = "Challenge location photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        // Fallback while the remote image loads
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
                            text = "⏳ ${state.secondsRemaining.toHoursMinutes()}",
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
                            text = "DAILY CHALLENGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = TextUnit(2f, TextUnitType.Sp),
                        )
                        Text(
                            text = if (state.challenge.cityName.isNotBlank()) "Find this spot in ${state.challenge.cityName}!"
                            else "Find this spot!",
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
                        // Show spinner overlaid on the card while submitting
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
                                    text = "Proximity Guide",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TemperatureLegend(currentTemperature = proximity)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Approximate Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "The spot is somewhere inside the circle.",
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
                        text = if (canSubmit) "Take Photo" else "Too Far Away",
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
                    contentDescription = "Expanded challenge photo",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Location side-effect — requests GPS updates while the screen is visible
// ---------------------------------------------------------------------------

@Composable
private fun LocationUpdatesEffect(
    onLocationUpdate: (android.location.Location) -> Unit,
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

// ---------------------------------------------------------------------------
// Proximity card
// ---------------------------------------------------------------------------

@Composable
private fun ProximityCard(
    proximity: ProximityTemperature?,
    distanceMeters: Int?,
    onToggleLegend: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = proximity?.color?.copy(alpha = 0.12f)
                ?: MaterialTheme.colorScheme.surfaceContainer
        ),
        border = proximity?.let { BorderStroke(1.5.dp, it.color.copy(alpha = 0.4f)) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = proximity?.emoji ?: "📍", style = MaterialTheme.typography.displayMedium
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = proximity?.label ?: "Searching…",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = proximity?.color ?: MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        distanceMeters == null -> "Head outside — GPS is needed to get your proximity hint"
                        else -> "You are about $distanceMeters m from the target"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Surface(
                onClick = onToggleLegend,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "❔", style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Map placeholder
// ---------------------------------------------------------------------------

@Composable
private fun MapPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.outline_map_24),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Map loading…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = "The target is somewhere in the circle",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Temperature legend table
// ---------------------------------------------------------------------------

@Composable
private fun TemperatureLegend(currentTemperature: ProximityTemperature?) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column {
            ProximityTemperature.entries.forEachIndexed { index, temp ->
                val isActive = temp == currentTemperature

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isActive) temp.color.copy(alpha = 0.18f) else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = temp.emoji, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = temp.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                        color = if (isActive) temp.color else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = temp.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.End,
                    )
                }

                if (index < ProximityTemperature.entries.toTypedArray().lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Completed section
// ---------------------------------------------------------------------------

@Composable
private fun CompletedSection(
    photoUrl: String,
    streakCount: Int,
    sharedToFeed: Boolean,
    onShareToFeed: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = "🔥", style = MaterialTheme.typography.displaySmall)
                Column {
                    Text(
                        text = "$streakCount day streak!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Challenge completed — see you tomorrow!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Photo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Your submitted photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_photo_camera_24),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { if (!sharedToFeed) onShareToFeed() },
            enabled = !sharedToFeed,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_group_24),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (sharedToFeed) "Shared ✓" else "Share to Friends' Feed",
                fontWeight = FontWeight.Bold,
            )
        }
    }
}