package com.pv239.beelocal.ui.screens.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pv239.beelocal.R
import com.pv239.beelocal.ui.screens.routes.components.RouteCard
import com.pv239.beelocal.ui.screens.routes.components.RouteStatus
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun RoutesScreen(
    innerPadding: PaddingValues,
    onRouteClick: (routeId: String) -> Unit,
    viewModel: RouteViewModel = hiltViewModel(),
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()
    val authUserId by viewModel.authUserId.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val latestExploreCount by rememberUpdatedState(state.exploreRoutes.size)
    val latestHasMore by rememberUpdatedState(state.hasMore)

    LaunchedEffect(authUserId, state.exploreRoutes.isEmpty(), state.error, state.isLoading) {
        if (authUserId != null &&
            state.exploreRoutes.isEmpty() &&
            state.error == null &&
            !state.isLoading
        ) {
            viewModel.loadRoutes()
        }
    }

    // Infinite scroll: load more when near the end of exploreRoutes.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .map { it ?: 0 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= latestExploreCount - 2 && latestHasMore) {
                    viewModel.loadMoreRoutes()
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        when {
            state.isLoading && state.exploreRoutes.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            state.error != null && state.exploreRoutes.isEmpty() -> {
                ErrorState(
                    message = state.error ?: stringResource(R.string.routes_error_unknown),
                    onRetry = { viewModel.loadRoutes() },
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 100.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    // ── Page header ──────────────────────────────────────────
                    item { PageHeader(city = state.city) }

                    // ── "Continue exploring" section ─────────────────────────
                    if (state.activeRoutes.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.routes_section_continue),
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                            )
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(end = 8.dp),
                            ) {
                                items(
                                    items = state.activeRoutes,
                                    key = { "active_${it.id}" },
                                ) { route ->
                                    RouteCard(
                                        modifier = Modifier.fillParentMaxWidth(0.78f),
                                        route = route,
                                        onClick = { onRouteClick(route.id) },
                                        routeStatus = RouteStatus.IN_PROGRESS,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // ── "Explore" section ────────────────────────────────────
                    item {
                        SectionHeader(
                            title = stringResource(R.string.routes_section_explore, state.city),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    items(items = state.exploreRoutes, key = { it.id }) { route ->
                        val status = when (route.id) {
                            in state.completedRouteIds -> RouteStatus.COMPLETED
                            in state.inProgressRouteIds -> RouteStatus.IN_PROGRESS
                            else -> null
                        }
                        RouteCard(
                            modifier = Modifier.padding(bottom = 12.dp),
                            route = route,
                            onClick = { onRouteClick(route.id) },
                            routeStatus = status,
                        )
                    }

                    // ── Loading-more spinner ─────────────────────────────────
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageHeader(city: String) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(
            text = stringResource(R.string.routes_title),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = stringResource(R.string.routes_subtitle, city),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        modifier = modifier,
    )
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.routes_load_error),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(stringResource(R.string.routes_retry))
        }
    }
}
