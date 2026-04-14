package com.pv239.beelocal.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class UserRouteProgress(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val routeId: String = "",
    val completedPointIds: List<String> = emptyList(),
    val isCompleted: Boolean = false,
    val startedAt: Timestamp? = null,
    val completedAt: Timestamp? = null
)
