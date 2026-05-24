package com.pv239.beelocal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.R

@Composable
fun TrendingRoutesSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.trending_routes_title),
            style = MaterialTheme.typography.displaySmall,
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(3) {
                RouteCard(
                    modifier = Modifier.fillParentMaxWidth(0.85f),
                    title = stringResource(R.string.trending_route_old_town_title),
                    description = stringResource(R.string.trending_route_old_town_description),
                    distance = stringResource(R.string.trending_route_old_town_distance),
                    time = stringResource(R.string.trending_route_old_town_time),
                    rating = stringResource(R.string.trending_route_old_town_rating),
                    tags = listOf(
                        stringResource(R.string.trending_route_old_town_tag_history),
                        stringResource(R.string.trending_route_old_town_tag_coffee),
                    )
                )
            }
        }
    }
}