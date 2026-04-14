package com.pv239.beelocal.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Exclude

data class RoutePoint(
    val name: String = "",
    val description: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val quizQuestion: String = "",
    @get:Exclude
    val quizAnswer: String = ""
)
