package com.pv239.beelocal.model

import com.google.firebase.Timestamp

data class UserStatistics(
    val userId: String = "",
    val streak: Int = 0,
    val xp: Int = 0,
    val lastStreakUpdate: Timestamp? = null,
)