package com.pv239.beelocal.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class DailyChallengeCompletion(
    @DocumentId
    val challengeId: String = "",
    val userId: String = "",
    val photoUrl: String = "",
    val completedAt: Timestamp? = null,
    val sharedToFeed: Boolean = false,
)