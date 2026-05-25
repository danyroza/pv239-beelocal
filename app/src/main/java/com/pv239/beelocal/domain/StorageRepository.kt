package com.pv239.beelocal.domain

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Firebase Cloud Storage operations.
 *
 * User-uploaded images are stored under:
 *   `users-content/<user-id>/<image-id>.jpg`
 *
 * where `<image-id>` is a generated UUID.
 */
@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage,
) {

    companion object {
        private const val USERS_CONTENT_FOLDER = "users-content"
    }

    /**
     * Uploads an image from a content [Uri] to `users-content/<userId>/<uuid>.jpg`
     * and returns the generated image ID and the public download URL.
     *
     * @param context Application context for reading the URI.
     * @param imageUri Content URI pointing to the image file.
     * @param userId  The authenticated user's ID (used as the sub-folder).
     * @return An [UploadResult] containing the generated UUID and download URL.
     */
    suspend fun uploadUserImage(
        context: Context,
        imageUri: Uri,
        userId: String,
    ): UploadResult {
        val imageId = UUID.randomUUID().toString()

        val ref = storage.reference.child("$USERS_CONTENT_FOLDER/$userId/$imageId.jpg")

        ref.putFile(imageUri).await()
        val downloadUrl = ref.downloadUrl.await().toString()

        return UploadResult(imageId = imageId, downloadUrl = downloadUrl)
    }

    /**
     * Deletes a previously uploaded user image at
     * `users-content/<userId>/<imageId>.jpg`.
     *
     * Used as a compensating action when the surrounding operation
     * (e.g. Firestore submission) fails after the upload succeeded, so we
     * don't leave orphaned blobs in storage.
     *
     * @param userId  The user the image belongs to.
     * @param imageId The image ID returned from [uploadUserImage].
     */
    suspend fun deleteUserImage(userId: String, imageId: String) {
        val ref = storage.reference.child("$USERS_CONTENT_FOLDER/$userId/$imageId.jpg")
        ref.delete().await()
    }

    /**
     * Holds the result of a successful image upload.
     */
    data class UploadResult(
        /** The generated UUID used as the image filename (without extension). */
        val imageId: String,
        /** The publicly accessible download URL of the uploaded image. */
        val downloadUrl: String,
    )
}
