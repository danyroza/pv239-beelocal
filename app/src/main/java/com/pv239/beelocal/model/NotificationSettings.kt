package com.pv239.beelocal.model

data class NotificationSettings(
    val emailEnabled: Boolean = true,
    val phoneEnabled: Boolean = true
)
