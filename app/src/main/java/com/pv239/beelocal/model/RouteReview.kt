package com.pv239.beelocal.model

import com.google.firebase.firestore.DocumentId

data class RouteReview(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val rating: Int = 1,
    val comment: String = "",
    val photoUrls: List<String> = emptyList()
) {
    init {
        require(rating in 1..5) { "Rating must be between 1 and 5, got $rating" }
    }
}
