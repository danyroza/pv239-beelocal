package com.pv239.beelocal.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Holds state and callbacks for launching the system camera and receiving a photo URI.
 */
class CameraLauncherState internal constructor(
    val launchCamera: () -> Unit,
)

/**
 * Returns a [CameraLauncherState] that manages temp-file creation, permission
 * requests, and camera intent launching. Extracted here so both
 * [DailyChallengeContent] and [BingoScreen] can reuse the same logic without
 * duplication.
 *
 * @param onPhotoTaken  Called with the [Uri] of the captured photo on success.
 * @param onCameraError Called with a human-readable error message on failure.
 */
@Composable
fun rememberCameraLauncher(
    onPhotoTaken: (Uri) -> Unit,
    onCameraError: (String) -> Unit = {},
): CameraLauncherState {
    val context = LocalContext.current

    var photoUri by remember { mutableStateOf<Uri>(Uri.EMPTY) }
    var awaitingCameraResult by remember { mutableStateOf(false) }

    fun createTempPhotoUri(prefix: String): Uri? {
        val photoFile = runCatching {
            val photoDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            File.createTempFile("${prefix}_", ".jpg", photoDir)
        }.getOrElse { e ->
            Log.e("CameraLauncher", "Failed to create temp photo file", e)
            onCameraError("Failed to create temp file for photo")
            return null
        }

        return runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile,
            )
        }.getOrElse { e ->
            Log.e("CameraLauncher", "Failed to create FileProvider URI", e)
            onCameraError("Failed to generate photo URI")
            null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && awaitingCameraResult) {
            awaitingCameraResult = false
            onPhotoTaken(photoUri)
        } else {
            if (photoUri != Uri.EMPTY) {
                runCatching {
                    context.contentResolver.delete(photoUri, null, null)
                }.onFailure { e ->
                    Log.w("CameraLauncher", "Failed to delete orphan temp photo", e)
                }
                photoUri = Uri.EMPTY
            }
            awaitingCameraResult = false
        }
    }

    fun launchCameraWithUri(prefix: String) {
        val uri = createTempPhotoUri(prefix) ?: run {
            awaitingCameraResult = false
            return
        }
        photoUri = uri
        awaitingCameraResult = true
        runCatching {
            cameraLauncher.launch(uri)
        }.onFailure { e ->
            Log.e("CameraLauncher", "Failed to launch camera", e)
            awaitingCameraResult = false
            onCameraError("Failed to launch camera")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraWithUri("photo")
        }
    }

    fun launchCamera(prefix: String = "photo") {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            launchCameraWithUri(prefix)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    return remember {
        CameraLauncherState(launchCamera = { launchCamera() })
    }
}
