package com.pv239.beelocal.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class DailyChallenge(
    @DocumentId
    val id: String = "",
    val imageUrl: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val radiusMeters: Double = 500.0,
    val date: Timestamp? = null,
    val is360View: Boolean = false
)
