package com.pv239.beelocal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pv239.beelocal.domain.FirestoreRepository
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

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
//        val userId = auth.currentUser?.uid ?: return
        val userId = auth.currentUser?.uid ?: "test-user-001" // TODO: remove this line
        viewModelScope.launch {
            repository.getStatistics(userId)
                ?.let { _statistics.value = it }
        }
    }
}