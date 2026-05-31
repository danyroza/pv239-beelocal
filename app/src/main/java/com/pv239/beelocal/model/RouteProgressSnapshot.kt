package com.pv239.beelocal.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Lightweight snapshot of a user's in-progress journey.
 *
 * Stored under  users/{userId}/routeProgress/{routeId}  in Firestore.
 *
 * [completedPointIds] holds the **string index** of each completed checkpoint
 * (e.g. "0", "1", "2"). Using strings keeps the Firestore arrayUnion calls
 * simple and avoids integer / long type ambiguity in the SDK.
 *
 * [lastAnswers] maps checkpoint index strings to the answer the user submitted,
 * so the answer field can be pre-filled when they revisit a previous checkpoint.
 *
 * [isCompleted] becomes true once every checkpoint has been answered; at that
 * point the document is kept for history but the journey cannot be restarted.
 */
data class RouteProgressSnapshot(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val routeId: String = "",

    /** String index ("0", "1", …) of each fully answered checkpoint. */
    val completedPointIds: List<String> = emptyList(),

    /**
     * Maps checkpoint index (as string) → the answer the user typed.
     * Populated on a correct answer so it can be pre-filled on revisit.
     */
    val lastAnswers: Map<String, String> = emptyMap(),

    val isCompleted: Boolean = false,
    val startedAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
)