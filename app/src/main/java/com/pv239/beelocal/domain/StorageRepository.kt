package com.pv239.beelocal.domain

import android.graphics.Bitmap
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Firebase Cloud Storage operations.
 *
 * User-uploaded images are stored under:
 *   `users-content/<user-id>/<image-id>`
 *
 * where `<image-id>` is a generated UUID (with `.jpg` extension).
 */
@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage,
) {

    companion object {
        private const val USERS_CONTENT_FOLDER = "users-content"
        private const val JPEG_QUALITY = 90
    }

    /**
     * Uploads a [Bitmap] as a JPEG to `users-content/<userId>/<uuid>.jpg`
     * and returns the public download URL.
     *
     * @param bitmap  The image to upload.
     * @param userId  The authenticated user's ID (used as the sub-folder).
     * @return A pair of (imageId, downloadUrl) — the generated UUID and the
     *         publicly accessible download URL of the uploaded image.
     */
    suspend fun uploadUserImage(
        bitmap: Bitmap,
        userId: String,
    ): UploadResult {
        val imageId = UUID.randomUUID().toString()

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
        val bytes = baos.toByteArray()

        val ref = storage.reference.child("$USERS_CONTENT_FOLDER/$userId/$imageId.jpg")

        ref.putBytes(bytes).await()
        val downloadUrl = ref.downloadUrl.await().toString()

        return UploadResult(imageId = imageId, downloadUrl = downloadUrl)
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
