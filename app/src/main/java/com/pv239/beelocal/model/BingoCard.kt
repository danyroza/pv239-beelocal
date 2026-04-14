package com.pv239.beelocal.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class BingoCard(
    @DocumentId
    val id: String = "",
    val weekStartDate: Timestamp = Timestamp.now(),
    val tasks: List<BingoTask> = emptyList()
)
