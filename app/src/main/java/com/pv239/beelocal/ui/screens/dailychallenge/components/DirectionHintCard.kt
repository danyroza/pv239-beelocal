package com.pv239.beelocal.ui.screens.dailychallenge.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Unlocked direction hint. Shows a large compass-style arrow pointing from
 * the user toward the (real) challenge location, plus a textual bearing
 * label ("≈ 247° • W").
 *
 * The bearing is **bearing-from-north** — i.e. it tells you where the target
 * is relative to the globe, not relative to whichever way the phone is
 * currently pointing. A true compass would require the magnetometer, which
 * this v1 deliberately skips.
 *
 * If [bearingDegrees] is null (no GPS fix yet) the card renders a placeholder
 * "Waiting for GPS…" message instead of a misleading 0° arrow.
 */
@Composable
fun DirectionHintCard(
    bearingDegrees: Float?,
    distanceMeters: Int?,
    modifier: Modifier = Modifier,
) {
    // Smoothly animate the arrow when bearing updates so it doesn't jitter
    // from GPS noise. animateFloatAsState handles null→0f gracefully because
    // we only render the arrow when bearing != null below.
    val animatedBearing by animateFloatAsState(
        targetValue = bearingDegrees ?: 0f,
        label = "compass_rotation",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Arrow puck ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(72.dp),
                ) {}
                if (bearingDegrees != null) {
                    val arrowColor = MaterialTheme.colorScheme.primary
                    Canvas(
                        modifier = Modifier
                            .size(56.dp)
                            .rotate(animatedBearing),
                    ) {
                        // A simple chevron arrow pointing "up" before rotation,
                        // so after rotating by `bearing` degrees it points in
                        // the right cardinal direction (0° = north → up).
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.5f, h * 0.10f)              // tip
                            lineTo(w * 0.85f, h * 0.80f)             // bottom-right
                            lineTo(w * 0.5f, h * 0.60f)              // notch
                            lineTo(w * 0.15f, h * 0.80f)             // bottom-left
                            close()
                        }
                        drawPath(path = path, color = arrowColor)
                        // Small centre dot
                        drawCircle(
                            color = arrowColor,
                            radius = 4.dp.toPx(),
                            center = Offset(w * 0.5f, h * 0.60f),
                        )
                    }
                } else {
                    Text("🧭", style = MaterialTheme.typography.headlineMedium)
                }
            }

            // ── Labels ───────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Direction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = bearingLabel(bearingDegrees),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                if (distanceMeters != null) {
                    Text(
                        text = "≈ $distanceMeters m away",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

/** Formats e.g. `247.3f` as `"≈ 247° • W"`. */
private fun bearingLabel(bearingDegrees: Float?): String {
    if (bearingDegrees == null) return "Waiting for GPS…"
    val normalized = ((bearingDegrees % 360f) + 360f) % 360f
    // Round, then normalise the rounded value back into [0,359]
    val rounded = ((normalized.roundToInt() % 360) + 360) % 360
    return "≈ $rounded° • ${cardinal(normalized)}"
}

/** 8-point compass label (N/NE/E/SE/S/SW/W/NW) for a bearing in [0, 360). */
private fun cardinal(bearing: Float): String {
    // Center each 45°-wide bucket on its cardinal so e.g. 22.5° is the boundary
    // between N and NE rather than 0° being NE-leaning. The +22.5 shift moves
    // the bucket boundary so 0° falls cleanly in the middle of the N range.
    val index = (((bearing + 22.5f) % 360f) / 45f).toInt()
    return arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[index]
}
