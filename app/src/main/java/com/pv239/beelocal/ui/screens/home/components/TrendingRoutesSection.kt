package com.pv239.beelocal.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.R
import com.pv239.beelocal.model.Route
import com.pv239.beelocal.ui.screens.routes.components.RouteCard

@Composable
fun TrendingRoutesSection(
    routes: List<Route> = emptyList(),
    onRouteClick: (routeId: String) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.trending_routes_title),
            style = MaterialTheme.typography.displaySmall,
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(
                items = routes,
                key = { it.id },
            ) { route ->
                RouteCard(
                    modifier = Modifier.fillParentMaxWidth(0.85f),
                    route = route,
                    onClick = { onRouteClick(route.id) },
                )
            }
        }
    }
}