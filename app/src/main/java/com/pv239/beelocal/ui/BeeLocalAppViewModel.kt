package com.pv239.beelocal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pv239.beelocal.domain.FirestoreRepository
import com.pv239.beelocal.model.User
import com.pv239.beelocal.model.UserStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BeeLocalAppViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _statistics = MutableStateFlow(UserStatistics())
    val statistics: StateFlow<UserStatistics> = _statistics.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    init {
        loadStatistics()
        loadUser()
    }

    /**
     * Both loaders are only reachable from the authenticated `MainGraph` (the
     * NavHost gates `BeelocalApp` behind a logged-in user), so a missing uid
     * here would be a programmer error — fail fast rather than silently
     * falling back to a stub account.
     */
    private fun loadStatistics() {
        val userId: String = checkNotNull(auth.currentUser?.uid) {
            "loadStatistics called without an authenticated user"
        }
        viewModelScope.launch {
            repository.getStatistics(userId)
                ?.let { _statistics.value = it }
        }
    }

    private fun loadUser() {
        val userId: String = checkNotNull(auth.currentUser?.uid) {
            "loadUser called without an authenticated user"
        }
        viewModelScope.launch {
            repository.getUser(userId)?.let { _user.value = it }
        }
    }
}
