package com.pv239.beelocal.model

import com.google.firebase.firestore.DocumentId

data class Route(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val authorId: String = "",
    val city: String = "",
    val points: List<RoutePoint> = emptyList(),
    val averageRating: Float = 0f,
    val reviewCount: Int = 0
)
