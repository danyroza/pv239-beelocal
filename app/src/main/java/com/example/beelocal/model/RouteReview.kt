package com.example.beelocal.model

import com.google.firebase.firestore.DocumentId

data class RouteReview(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val photoUrls: List<String> = emptyList()
)
