package com.pv239.beelocal.ui.screens.profile.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bento-style stat card with a label / large value / caption stack and an
 * oversized decorative icon that bleeds off the top-right corner. Designed to
 * compose well in a two-column grid inside [ProfileHero].
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    caption: String,
    container: Color,
    accent: Color,
    onAccent: Color,
    iconRes: Int,
    modifier: Modifier = Modifier,
    iconAlpha: Float = 0.08f,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = container,
        shadowElevation = 1.dp,
    ) {
        Box {
            // Oversized decorative icon that bleeds off the top-right corner,
            // mirroring the bento-style "ghost" decoration in the design.
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = accent.copy(alpha = iconAlpha),
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .padding(start = 16.dp, bottom = 16.dp)
                    .offset(x = 16.dp, y = (-16).dp),
            )
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = onAccent,
                    letterSpacing = 1.5.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = accent,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = onAccent,
                )
            }
        }
    }
}
