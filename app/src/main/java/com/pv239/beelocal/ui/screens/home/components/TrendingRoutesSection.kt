package com.pv239.beelocal.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.model.Route
import com.pv239.beelocal.ui.screens.routes.components.RouteCard
import com.pv239.beelocal.ui.screens.routes.components.RouteStatus

@Composable
fun TrendingRoutesSection(
    routes: List<Route> = emptyList(),
    routeStatusFor: (String) -> RouteStatus? = { null },
    onRouteClick: (routeId: String) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(
                items = routes,
                key = { it.id },
            ) { route ->
                RouteCard(
                    modifier = Modifier.fillParentMaxWidth(0.85f),
                    route = route,
                    onClick = { onRouteClick(route.id) },
                    routeStatus = routeStatusFor(route.id),
                )
            }
        }
    }
}
