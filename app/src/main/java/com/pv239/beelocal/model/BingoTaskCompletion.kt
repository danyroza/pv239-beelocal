package com.pv239.beelocal.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.pv239.beelocal.model.types.BingoTaskType

/**
 * Represents a user's completion of a single [BingoTask] from a [BingoCard].
 *
 * Created when the user submits proof of a bingo task (typically a photo,
 * or a location check-in depending on [BingoTaskType]). Holds all the data
 * needed to render the achievement on the social feed.
 */
data class BingoTaskCompletion(
    @DocumentId
    val id: String = "",

    // Author info (denormalized for feed display)
    val userId: String = "",
    val username: String = "",
    val userProfileImageUrl: String? = null,

    // References to the originating bingo card / task
    val bingoCardId: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val taskType: BingoTaskType = BingoTaskType.PHOTO,

    // Submission content
    val photoUrl: String? = null,
    val caption: String = "",
    val location: GeoPoint? = null,

    // When the task was completed
    val completedAt: Timestamp = Timestamp.now()
)
