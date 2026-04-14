package com.pv239.beelocal.model

import com.google.firebase.firestore.DocumentId

data class UserBingoProgress(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val bingoCardId: String = "",
    val completedTaskIds: List<String> = emptyList()
)
