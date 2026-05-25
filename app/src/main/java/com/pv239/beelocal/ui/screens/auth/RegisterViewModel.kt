package com.pv239.beelocal.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pv239.beelocal.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(RegisterUiState())
        private set

    private val _events = Channel<RegisterEvent>()
    val events = _events.receiveAsFlow()

    fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            uiState = uiState.copy(errorMessage = "Please fill in all fields.")
            return
        }
        if (password != confirmPassword) {
            uiState = uiState.copy(errorMessage = "Passwords do not match.")
            return
        }
        if (password.length < 6) {
            uiState =
                uiState.copy(errorMessage = "Password must be at least 6 characters.")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            authRepository.register(email, password, username)
                .onSuccess {
                    uiState = uiState.copy(isLoading = false)
                    _events.send(RegisterEvent.Success)
                }
                .onFailure {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = it.message
                    )
                }
        }
    }
}

data class RegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface RegisterEvent {
    data object Success : RegisterEvent
}
