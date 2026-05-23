package com.pv239.beelocal.permissions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pv239.beelocal.data.repository.PermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val permissionsRepository: PermissionRepository
) : ViewModel() {

    var hasLocationPermission: Boolean by mutableStateOf<Boolean>(false)
        private set

    var hasCameraPermission: Boolean by mutableStateOf<Boolean>(false)
        private set

    var hasNotificationPermission: Boolean by mutableStateOf<Boolean>(false)
        private set

    init {
        checkAllPermissions()
    }

    fun checkAllPermissions() {
        hasLocationPermission = permissionsRepository.hasLocationPermission()
        hasCameraPermission = permissionsRepository.hasCameraPermission()
        hasNotificationPermission = permissionsRepository.hasNotificationPermission()
    }

    fun checkLocationPermission() {
        hasLocationPermission = permissionsRepository.hasLocationPermission()
    }

    fun checkCameraPermission() {
        hasCameraPermission = permissionsRepository.hasCameraPermission()
    }

    fun checkNotificationPermission() {
        hasNotificationPermission = permissionsRepository.hasNotificationPermission()
    }
}