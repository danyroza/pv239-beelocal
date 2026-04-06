package com.example.beelocal.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class UserRouteProgress(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val routeId: String = "",
    val currentPointIndex: Int = 0,
    val completedPoints: List<Int> = emptyList(),
    val isCompleted: Boolean = false,
    val startedAt: Timestamp = Timestamp.now(),
    val completedAt: Timestamp? = null
)
