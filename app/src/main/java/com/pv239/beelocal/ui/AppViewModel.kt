package com.pv239.beelocal.ui

import androidx.lifecycle.ViewModel
import com.pv239.beelocal.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    val isLoggedIn: Boolean get() = authRepository.currentUser != null
}
