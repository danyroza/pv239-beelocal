package com.pv239.beelocal.ui.screens.routes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Displays the quiz question / riddle for the current checkpoint in a
 * styled quotation card, matching the design in the mockup.
 */
@Composable
fun RiddleCard(
    question: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        // Large decorative quote mark
        Text(
            text = "\u201D",
            fontSize = 64.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd),
            lineHeight = 40.sp,
        )
        Text(
            text = "\u201C$question\u201D",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        )
    }
}