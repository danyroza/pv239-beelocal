package com.pv239.beelocal.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.R
import kotlin.time.Duration.Companion.seconds

@Composable
fun TimeRemainingBadge(
    modifier: Modifier = Modifier,
    secondsRemaining: Long,
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.daily_challenge_section_time_remaining, secondsRemaining.toHoursMinutes()),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun Long.toHoursMinutes(): String {
    return this.seconds.toComponents { hours, minutes, _, _ ->
        stringResource(R.string.daily_challenge_time_remaining, hours, minutes)
    }
}