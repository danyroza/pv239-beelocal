package com.pv239.beelocal.ui.screens.dailychallenge.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProximityCard(
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
