package com.pv239.beelocal.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pv239.beelocal.data.repository.AuthRepository
import com.pv239.beelocal.domain.FirestoreRepository
import com.pv239.beelocal.model.User
import com.pv239.beelocal.model.UserStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BeeLocalAppViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    auth: FirebaseAuth,
    private val authRepository: AuthRepository
) : ViewModel() {

    val isLoggedIn: Boolean get() = authRepository.currentUser != null

    /**
     * Both flows are only reachable from the authenticated `MainGraph` (the
     * NavHost gates `BeelocalApp` behind a logged-in user), so a missing uid
     * here would be a programmer error — fail fast rather than silently
     * falling back to a stub account.
     */
    private val userId: String = checkNotNull(auth.currentUser?.uid) {
        "BeeLocalAppViewModel constructed without an authenticated user"
    }

    private val _statistics = MutableStateFlow(UserStatistics())
    val statistics: StateFlow<UserStatistics> = _statistics.asStateFlow()

    /**
     * Real-time user profile, backed by a Firestore snapshot listener. The
     * header re-renders automatically when e.g. the profile picture is
     * updated elsewhere in the app, so callers don't need to manually
     * refresh after writes.
     */
    val user: StateFlow<User?> = repository.observeUser(userId)
        .catch { e ->
            Log.w("BeeLocalAppViewModel", "User snapshot listener failed", e)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            // Keep the listener alive briefly across config changes so we
            // don't churn Firestore registrations on rotation.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            repository.getStatistics(userId)
                ?.let { _statistics.value = it }
        }
    }
}
