package com.pv239.beelocal.model

import com.google.firebase.firestore.GeoPoint

data class RoutePoint(
    val name: String = "",
    val description: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val quizQuestion: String = "",
    val quizAnswer: String = ""
)
