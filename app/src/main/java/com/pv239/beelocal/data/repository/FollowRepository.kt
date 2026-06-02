package com.pv239.beelocal.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pv239.beelocal.domain.FirestoreCollections
import com.pv239.beelocal.model.FollowRequest
import com.pv239.beelocal.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Profile-visibility flag plus the [FollowRequest] lifecycle (create / accept /
 * deny). Splitting this out from [UserRepository] keeps the friends-list
 * mutation logic close to the policy that decides *whether* a friend can be
 * added without confirmation.
 */
@Singleton
class FollowRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository,
) {

    private val usersCollection get() = firestore.collection(FirestoreCollections.USERS.value)
    private val requestsCollection
        get() = firestore.collection(FirestoreCollections.FOLLOW_REQUESTS.value)

    /**
     * Toggle whether the user's profile is publicly followable.
     *
     * Switching to public does **not** auto-accept already-pending requests;
     * those still require an explicit accept/deny (so the user always
     * consciously sees who asked while they were private).
     */
    suspend fun updateProfileVisibility(userId: String, isPublic: Boolean) {
        usersCollection.document(userId)
            .update("isProfilePublic", isPublic)
            .await()
    }

    /**
     * Request to follow [toUserId] from [fromUser].
     *
     * - If the target user has a **public** profile, no confirmation is needed
     *   and [toUserId] is immediately added to [fromUser]'s friends list (i.e.
     *   the follower now sees the target's feed entries).
     * - If the target is **private**, a [FollowRequest] document is created
     *   instead and must be accepted by the target via [acceptFollowRequest].
     *
     * Returns `true` if the follow was accepted immediately, `false` if a
     * request was created and is awaiting approval.
     */
    suspend fun requestFollow(fromUser: User, toUserId: String): Boolean {
        if (fromUser.id == toUserId) return false
        val target = userRepository.getUser(toUserId)
            ?: throw IllegalStateException("Target user $toUserId does not exist")

        return if (target.profilePublic) {
            userRepository.addFriend(fromUser.id, toUserId)
            true
        } else {
            // Use a deterministic document id so duplicate requests are
            // impossible by construction, and perform the existence check +
            // write inside a single transaction to eliminate the TOCTOU race condition.
            val requestRef = requestsCollection.document("${fromUser.id}_$toUserId")

            firestore.runTransaction { tx ->
                val snapshot = tx.get(requestRef)
                if (!snapshot.exists()) {
                    val request = FollowRequest(
                        fromUserId = fromUser.id,
                        fromUsername = fromUser.username,
                        fromUserProfileImageUrl = fromUser.profileImageUrl,
                        toUserId = toUserId,
                    )
                    tx.set(requestRef, request)
                }
                null
            }.await()
            false
        }
    }

    /**
     * Returns the list of [FollowRequest]s currently awaiting [userId]'s
     * approval, newest first.
     */
    suspend fun getPendingFollowRequests(userId: String): List<FollowRequest> {
        return requestsCollection
            .whereEqualTo("toUserId", userId)
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(FollowRequest::class.java)
    }

    /**
     * Accepts a pending follow request: atomically deletes the request and
     * adds the target (`toUserId`) to the requester's (`fromUserId`) friends
     * list — meaning the requester now sees the target's shared feed.
     */
    suspend fun acceptFollowRequest(request: FollowRequest) {
        val requestRef = requestsCollection.document(request.id)
        val followerRef = usersCollection.document(request.fromUserId)

        firestore.runTransaction { tx ->
            tx.update(followerRef, "friends", FieldValue.arrayUnion(request.toUserId))
            tx.delete(requestRef)
        }.await()
    }

    /** Deny a pending follow request — just deletes the document. */
    suspend fun denyFollowRequest(requestId: String) {
        requestsCollection.document(requestId).delete().await()
    }
}
