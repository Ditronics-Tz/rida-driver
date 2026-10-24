package com.example.ridedriver.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.remote.RidaApiClient
import com.example.ridedriver.data.remote.RidaApiErrors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

object ProfilePhotoRepository {
    suspend fun upload(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = SessionManager.observe(context).first()
            val bytes = context.contentResolver.openInputStream(uri)?.use(ProfilePhotoContract::readBytes)
                ?: throw IllegalArgumentException("Couldn't open this photo. Choose another image.")
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            val mime = options.outMimeType
            val extension = ProfilePhotoContract.extensions[mime]
            require(options.outWidth > 0 && options.outHeight > 0 && extension != null) {
                "Choose a JPG, PNG or WebP photo smaller than 5 MB."
            }
            val part = MultipartBody.Part.createFormData(
                "profile_picture", "profile.$extension", bytes.toRequestBody(mime.toMediaType())
            )
            val response = RidaApiClient.api.uploadProfileImage(part)
            val url = ProfilePhotoContract.photoUrl(response, RidaApiClient.BASE_URL)
                ?: ProfilePhotoContract.photoUrl(RidaApiClient.api.getProfile(), RidaApiClient.BASE_URL)
                ?: throw IllegalStateException("Couldn't load the uploaded photo. Try again.")
            val current = SessionManager.observe(context).first()
            if (!current.loggedIn || current.userId != session.userId || current.email != session.email) {
                throw CancellationException("The signed-in account changed.")
            }
            SessionManager.updateProfilePicture(context, url, forceRefresh = true)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = when {
                e is IllegalArgumentException || e is IllegalStateException -> e.message ?: "Couldn't upload your photo. Try again."
                e is HttpException && e.code() == 413 -> "Choose a photo smaller than 5 MB."
                e is HttpException && e.code() == 415 -> "Choose a JPG, PNG or WebP photo."
                e is HttpException && e.code() == 404 -> "Photo uploads are not available yet. Try again later."
                else -> RidaApiErrors.message(e, "Couldn't upload your photo. Try again.")
            }
            Result.failure(IllegalArgumentException(message, e))
        }
    }
}
