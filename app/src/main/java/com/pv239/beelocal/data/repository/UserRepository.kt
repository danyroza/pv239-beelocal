package com.pv239.beelocal.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import com.pv239.beelocal.domain.FirestoreCollections
import com.pv239.beelocal.domain.FirestoreConfig.USERS_SEARCH_PAGE_SIZE
import com.pv239.beelocal.domain.Page
import com.pv239.beelocal.model.User
import com.pv239.beelocal.model.UserStatistics
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User identity, profile metadata, friends list and per-user statistics
 * (streak / XP) operations.
 *
 * Split out from the legacy monolithic `FirestoreRepository` so consumers
 * only pull in the slice of Firestore they actually need.
 */
@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    private val usersCollection get() = firestore.collection(FirestoreCollections.USERS.value)
    private val statisticsCollection
        get() = firestore.collection(FirestoreCollections.USER_STATISTICS.value)

    // -------------------------------------------------------------------------
    // User document
    // -------------------------------------------------------------------------

    suspend fun getUser(userId: String): User? {
        val snapshot = usersCollection.document(userId).get().await()
        if (!snapshot.exists()) return null
        return snapshot.toObject<User>()
    }

    /**
     * Reactive stream of [userId]'s user document. Emits the latest [User]
     * whenever it changes in Firestore (e.g. profile picture upload, friends
     * list mutation). Emits `null` if the document does not exist.
     *
     * The underlying Firestore snapshot listener is automatically detached
     * when the collector cancels.
     */
    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val registration = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.toObject<User>())
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveUser(user: User) {
        val userToSave = user.copy(
            usernameNormalized = user.username.lowercase().trim()
        )
        usersCollection.document(user.id).set(userToSave).await()
    }

    /**
     * Returns the first page of users whose username starts with [query].
     *
     * Usage:
     * ```
     * val first = repo.searchUsers("an")
     * val second = repo.searchUsers("an", lastVisible = first.cursor)
     * ```
     */
    suspend fun searchUsers(
        query: String,
        lastVisible: DocumentSnapshot? = null,
    ): Page<User> {
        val normalizedQuery = query.lowercase().trim()

        val baseQuery = usersCollection
            .whereGreaterThanOrEqualTo("usernameNormalized", normalizedQuery)
            .whereLessThanOrEqualTo(
                "usernameNormalized",
                normalizedQuery + "\uf8ff",
            )
            .orderBy("usernameNormalized")
            .limit(USERS_SEARCH_PAGE_SIZE)

        val firestoreQuery =
            if (lastVisible != null) baseQuery.startAfter(lastVisible) else baseQuery

        val snapshot = firestoreQuery.get().await()
        val hasMore = snapshot.size() == USERS_SEARCH_PAGE_SIZE.toInt()
        return Page(
            items = snapshot.toObjects(User::class.java),
            cursor = if (hasMore) snapshot.documents.lastOrNull() else null,
            hasMore = hasMore,
        )
    }

    /**
     * Update the user's profile picture URL (typically after a fresh upload to
     * Cloud Storage). Pass `null` to clear it back to the default avatar.
     */
    suspend fun updateProfileImage(userId: String, profileImageUrl: String?) {
        usersCollection.document(userId)
            .update("profileImageUrl", profileImageUrl)
            .await()
    }

    suspend fun addFriend(currentUserId: String, friendId: String) {
        usersCollection.document(currentUserId)
            .update("friends", FieldValue.arrayUnion(friendId))
            .await()
    }

    suspend fun removeFriend(currentUserId: String, friendId: String) {
        usersCollection.document(currentUserId)
            .update("friends", FieldValue.arrayRemove(friendId))
            .await()
    }

    // -------------------------------------------------------------------------
    // User statistics (streak / XP)
    // -------------------------------------------------------------------------

    /**
     * Reactive stream of [userId]'s [UserStatistics] document. Emits whenever
     * streak / xp changes so headers and summaries can react to mutations
     * made elsewhere in the app.
     */
    fun observeStatistics(userId: String): Flow<UserStatistics?> = callbackFlow {
        val registration = statisticsCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.toObject<UserStatistics>())
            }
        awaitClose { registration.remove() }
    }

    suspend fun getStatistics(userId: String): UserStatistics? {
        val snapshot = statisticsCollection.document(userId).get().await()
        if (!snapshot.exists()) return null
        return snapshot.toObject<UserStatistics>()
    }

    suspend fun saveStatistics(statistics: UserStatistics) {
        statisticsCollection.document(statistics.userId).set(statistics).await()
    }

    suspend fun updateStreak(userId: String, newStreak: Int) {
        statisticsCollection.document(userId)
            .update(
                mapOf(
                    "streak" to newStreak,
                    "lastStreakUpdate" to Timestamp.now(),
                )
            )
            .await()
    }

    suspend fun updateXp(userId: String, newXp: Int) {
        statisticsCollection.document(userId)
            .update("xp", newXp)
            .await()
    }

    /**
     * Atomically adjusts the user's XP balance by [delta] (positive to award,
     * negative to spend). Uses [FieldValue.increment] so concurrent writes
     * compose cleanly, and merges into the document so missing statistics
     * documents are created on first use.
     */
    suspend fun awardXp(userId: String, delta: Int) {
        if (delta == 0) return
        statisticsCollection.document(userId)
            .set(
                mapOf(
                    "userId" to userId,
                    "xp" to FieldValue.increment(delta.toLong()),
                ),
                SetOptions.merge(),
            )
            .await()
    }

    // -------------------------------------------------------------------------
    // Internal helpers for sibling repositories that need raw document refs
    // (kept package-private to discourage external use).
    // -------------------------------------------------------------------------

    internal fun userDocRef(userId: String) = usersCollection.document(userId)
    internal fun statisticsDocRef(userId: String) = statisticsCollection.document(userId)
}
