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
import androidx.compose.ui.unit.dp

@Composable
fun TrendingRoutesSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Trending Routes",
            style = MaterialTheme.typography.displaySmall,
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow (
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(3) {
                RouteCard(
                    modifier = Modifier.fillParentMaxWidth(0.85f),
                    title = "Old Town Walk",
                    description = "Discover hidden workshops, secret courtyards, and the best espresso bars in the historic district.",
                    distance = "2.4 km",
                    time = "45 m",
                    rating = "4.6",
                    tags = listOf("History", "Coffee")
                )
            }
        }
    }
}