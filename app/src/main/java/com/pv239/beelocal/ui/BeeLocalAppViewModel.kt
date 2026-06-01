package com.pv239.beelocal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pv239.beelocal.data.repository.AuthRepository
import com.pv239.beelocal.domain.FirestoreRepository
import com.pv239.beelocal.model.UserStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.pv239.beelocal.model.User
import javax.inject.Inject

/**
 * Backs the global app chrome (the [com.pv239.beelocal.ui.components.Header]).
 *
 * Exposes reactive streams of the current user's [UserStatistics] and
 * `profileImageUrl` so the header stays in sync with mutations made elsewhere
 * (e.g. when the user uploads a new profile picture from the profile screen,
 * the avatar in the header updates automatically).
 */
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

    val statistics: StateFlow<UserStatistics> = repository.observeStatistics(userId)
        .map { it ?: UserStatistics(userId = userId) }
        .catch { emit(UserStatistics(userId = userId)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserStatistics(userId = userId),
        )

    private val user: StateFlow<User?> = repository.observeUser(userId)
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val profileImageUrl: StateFlow<String?> = user
        .map { it?.profileImageUrl }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val username: StateFlow<String?> = user
        .map { it?.username }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}
