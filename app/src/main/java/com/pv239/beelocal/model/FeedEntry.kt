package com.pv239.beelocal.model

import com.pv239.beelocal.model.types.FeedEntryType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class FeedEntry(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImageUrl: String? = null,
    val type: FeedEntryType = FeedEntryType.DAILY_CHALLENGE,
    val imageUrl: String = "",
    val location: GeoPoint? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val challengeId: String? = null,
    val routeId: String? = null
)
