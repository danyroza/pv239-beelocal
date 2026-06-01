package com.pv239.beelocal.ui.screens.routes

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.pv239.beelocal.R
import com.pv239.beelocal.ui.screens.routes.components.CheckpointListItem
import com.pv239.beelocal.ui.screens.routes.components.JourneyProgressBar
import com.pv239.beelocal.ui.screens.routes.components.RouteCard
import com.pv239.beelocal.ui.screens.routes.components.RouteMapView
import com.pv239.beelocal.ui.screens.routes.components.RouteReviewsSection

@Composable
fun RouteDetailScreen(
    innerPadding: PaddingValues,
    routeId: String,
    onJourneyStart: () -> Unit,
    viewModel: RouteViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()
    val journeyState by viewModel.journeyState.collectAsStateWithLifecycle()
    val bottomInset = innerPadding.calculateBottomPadding()


    LaunchedEffect(routeId) { viewModel.loadRoute(routeId) }

    // Track the last navigationTrigger value we acted on so a re-composition
    // (e.g. screen coming back from the stack) doesn't fire navigation again.
    var lastSeenTrigger by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(journeyState.navigationTrigger) {
        if (journeyState.navigationTrigger > lastSeenTrigger) {
            lastSeenTrigger = journeyState.navigationTrigger
            onJourneyStart()
        }
    }

    var showExpandedPhoto by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )

            state.route == null -> Text(
                text = stringResource(R.string.route_detail_not_found),
                modifier = Modifier.align(Alignment.Center),
            )

            else -> {
                val route = state.route!!
                val completedIndices =
                    state.completedPointIds.mapNotNull { it.toIntOrNull() }.toSet()

                LazyColumn(
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 160.dp,
                    ),
                ) {
                    // Resumption progress bar
                    item {
                        JourneyProgressBar(
                            currentIndex = completedIndices.size - 1,
                            total = route.points.size,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        )
                    }

                    // Route info card
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.route_detail_route),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }

                        RouteCard(
                            route = route,
                            onClick = {},
                            onImageClick = { showExpandedPhoto = true },
                            routeStatus = null,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp),
                        )
                    }

                    // Already-completed banner
                    if (state.isAlreadyCompleted) {
                        item {
                            CompletedBanner(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 16.dp),
                            )
                        }
                    }

                    // Journey section header
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.route_detail_the_journey),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }

                    // ── Route map ─────────────────────────────────────────────
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        RouteMapView(
                            points = route.points,
                            completedPointIndices = completedIndices,
                            currentPointIndex = -1,
                            userLatLng = null,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            height = 200.dp,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Checkpoint list
                    items(route.points) { point ->
                        val index = route.points.indexOf(point)
                        CheckpointListItem(
                            index = index,
                            point = point,
                            isCompleted = index in completedIndices,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp),
                        )
                    }

                    // Reviews
                    item {
                        Spacer(modifier = Modifier.height(24.dp))

                        RouteReviewsSection(
                            reviews = state.routeReviews,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Start / Resume FAB (hidden once fully completed)
                if (!state.isAlreadyCompleted) {
                    val fabLabel = when {
                        completedIndices.isNotEmpty() -> stringResource(R.string.route_detail_resume_journey)
                        else -> stringResource(R.string.route_detail_start_journey)
                    }
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.startJourney(route) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = bottomInset + 16.dp),
                        shape = RoundedCornerShape(28.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_play_arrow_24),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        text = {
                            Text(text = fabLabel, fontWeight = FontWeight.Bold)
                        },
                    )
                }

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
                                model = route.imageUrl,
                                contentDescription = route.name,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "🏆", fontSize = 22.sp)
            Column {
                Text(
                    text = "Route completed!",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                Text(
                    text = "You've already finished this route.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    ),
                )
            }
        }
    }
}
