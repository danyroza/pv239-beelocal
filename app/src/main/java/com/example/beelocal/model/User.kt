package com.example.beelocal.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val streak: Int = 0,
    val friends: List<String> = emptyList(),
    val notificationSettings: NotificationSettings = NotificationSettings()
)
