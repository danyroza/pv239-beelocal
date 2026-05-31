package com.pv239.beelocal.ui.screens.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.R
import com.pv239.beelocal.model.FollowRequest

/**
 * Card-style row representing an incoming follow request. Shows the
 * requester's avatar + username with accept/deny buttons, swapping the action
 * area for a spinner while a Firestore mutation is in flight.
 */
@Composable
fun FollowRequestRow(
    request: FollowRequest,
    processing: Boolean,
    onAccept: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(
                imageUrl = request.fromUserProfileImageUrl,
                sizeDp = 44,
                background = MaterialTheme.colorScheme.primaryContainer,
                username = request.fromUsername,
            )
            Text(
                text = request.fromUsername.ifBlank {
                    stringResource(R.string.profile_unknown_user)
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (processing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                OutlinedButton(
                    onClick = onDeny,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(stringResource(R.string.profile_follow_request_deny))
                }
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(stringResource(R.string.profile_follow_request_accept))
                }
            }
        }
    }
}
