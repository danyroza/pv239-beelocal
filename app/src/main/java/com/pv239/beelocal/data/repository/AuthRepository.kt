package com.pv239.beelocal.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.pv239.beelocal.domain.FirestoreCollections
import com.pv239.beelocal.model.User
import com.pv239.beelocal.model.UserStatistics
import kotlinx.coroutines.suspendCancellableCoroutine
import jakarta.inject.Inject
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun login(email: String, password: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e ->
                    val message = when (e) {
                        is FirebaseAuthInvalidCredentialsException ->
                            "Incorrect email or password."
                        else -> e.message ?: "Login failed. Please try again."
                    }
                    cont.resume(Result.failure(Exception(message)))
                }
        }

    suspend fun register(email: String, password: String, username: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: run {
                        cont.resume(Result.failure(Exception("Registration failed. Please try again.")))
                        return@addOnSuccessListener
                    }

                    val user = User(
                        id = uid,
                        username = username,
                        usernameNormalized = username.lowercase(),
                        email = email
                    )
                    val statistics = UserStatistics(
                        userId = uid,
                        streak = 0,
                        xp = 0
                    )
                    val batch = firestore.batch()
                    batch.set(
                        firestore.collection(FirestoreCollections.USERS.value).document(uid),
                        user
                    )
                    batch.set(
                        firestore.collection(FirestoreCollections.USER_STATISTICS.value).document(uid),
                        statistics
                    )
                    batch.commit()
                        .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                        .addOnFailureListener { e ->
                            auth.currentUser?.delete()
                            cont.resume(Result.failure(Exception(e.message ?: "Failed to create user profile.")))
                        }
                }
                .addOnFailureListener { e ->
                    val message = when (e) {
                        is FirebaseAuthUserCollisionException ->
                            "An account with this email already exists."
                        is FirebaseAuthInvalidCredentialsException ->
                            "Invalid email address."
                        else -> e.message ?: "Registration failed. Please try again."
                    }
                    cont.resume(Result.failure(Exception(message)))
                }
        }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = requireNotNull(auth.currentUser) { "User not authenticated" }
        val email = requireNotNull(user.email) { "No email on account" }

        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        try {
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
        } catch (e: Exception) {
            throw Exception(changePasswordErrorMessage(e), e)
        }
    }

    private fun changePasswordErrorMessage(error: Exception): String = when (error) {
        is FirebaseAuthWeakPasswordException ->
            "Your new password is too weak. Please choose a stronger password."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect current password."
        is FirebaseAuthRecentLoginRequiredException ->
            "For security reasons, please sign in again and retry changing your password."
        else -> "Failed to change password. Please try again."
    }
}
