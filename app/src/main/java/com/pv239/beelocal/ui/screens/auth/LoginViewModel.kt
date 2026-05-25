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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    private val _events = Channel<LoginEvent>()
    val events = _events.receiveAsFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            uiState = uiState.copy(errorMessage = "Please fill in all fields.")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            authRepository.login(email, password)
                .onSuccess {
                    uiState = uiState.copy(isLoading = false)
                    _events.send(LoginEvent.Success)
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

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface LoginEvent {
    data object Success : LoginEvent
}
