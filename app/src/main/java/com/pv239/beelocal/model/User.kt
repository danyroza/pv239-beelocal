package com.pv239.beelocal.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val username: String = "",
    val usernameNormalized: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val friends: List<String> = emptyList(),
    val notificationSettings: NotificationSettings = NotificationSettings()
)
