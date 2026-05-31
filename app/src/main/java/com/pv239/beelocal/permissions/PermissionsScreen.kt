package com.pv239.beelocal.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun PermissionsScreen(
    modifier: Modifier = Modifier,
    permissionViewModel: PermissionViewModel = hiltViewModel()
) {
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            permissionViewModel.checkLocationPermission()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            permissionViewModel.checkCameraPermission()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            permissionViewModel.checkNotificationPermission()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "To provide you with the best experience, BeeLocal needs access to some features of your device.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        PermissionItem(
            title = "Location",
            description = "Needed to show you local events and routes nearby.",
            icon = Icons.Default.LocationOn,
            isGranted = permissionViewModel.hasLocationPermission,
            onGrantClick = {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        PermissionItem(
            title = "Camera",
            description = "Needed for uploading daily challenge photos and completing bingo tasks.",
            icon = Icons.Default.CameraAlt,
            isGranted = permissionViewModel.hasCameraPermission,
            onGrantClick = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        PermissionItem(
            title = "Notifications",
            description = "Needed to keep you updated on local events and rewards.",
            icon = Icons.Default.Notifications,
            isGranted = permissionViewModel.hasNotificationPermission,
            onGrantClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
    }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        if (isGranted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Granted",
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Button(onClick = onGrantClick) {
                Text("Allow")
            }
        }
    }
}