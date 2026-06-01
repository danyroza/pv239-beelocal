package com.pv239.beelocal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pv239.beelocal.data.repository.AuthRepository
import com.pv239.beelocal.domain.FirestoreRepository
import com.pv239.beelocal.model.User
import com.pv239.beelocal.model.UserStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    val isLoggedIn: Boolean
        get() = authRepository.currentUser != null

    private val userId: String? = auth.currentUser?.uid
    private val emptyStatistics = UserStatistics(userId = userId.orEmpty())

    val statistics: StateFlow<UserStatistics> = (userId?.let { uid ->
        repository.observeStatistics(uid)
            .map { it ?: UserStatistics(userId = uid) }
            .catch { emit(UserStatistics(userId = uid)) }
    } ?: flowOf(emptyStatistics))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyStatistics,
        )

    private val user: StateFlow<User?> = (userId?.let { uid ->
        repository.observeUser(uid)
            .catch { emit(null) }
    } ?: flowOf(null))
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
