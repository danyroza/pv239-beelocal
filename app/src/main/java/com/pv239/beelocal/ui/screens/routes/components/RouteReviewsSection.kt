package com.pv239.beelocal.ui.screens.routes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pv239.beelocal.R
import com.pv239.beelocal.model.RouteReview

@Composable
fun RouteReviewsSection(
    reviews: List<RouteReview>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.route_reviews),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (reviews.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(
                    text = stringResource(R.string.route_reviews_no_reviews),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                reviews.forEach { review ->
                    ReviewCard(review)
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    review: RouteReview,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = review.username.ifBlank { stringResource(R.string.route_reviews_anonymous) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.weight(1f))

                repeat(review.rating) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_star_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primaryContainer,
                    )
                }
            }

            if (review.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (review.photoUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(review.photoUrls) { photoUrl ->
                        ReviewPhoto(photoUrl)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewPhoto(
    photoUrl: String,
) {
    AsyncImage(
        model = photoUrl,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        contentScale = ContentScale.Crop,
    )
}