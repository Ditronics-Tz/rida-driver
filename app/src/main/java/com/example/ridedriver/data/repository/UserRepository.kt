package com.example.ridedriver.data.repository

import android.content.Context
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.remote.RidaApiClient
import com.example.ridedriver.data.remote.RidaApiErrors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

object UserRepository {
    suspend fun refreshSessionFromApi(context: Context) {
        try {
            val me = RidaApiClient.api.me()
            require(me.user.role == "driver") { "Use a driver account to sign in to this app." }
            val user = me.user
            val current = SessionManager.observe(context).first()
            if (!current.loggedIn || current.userId.isNotBlank() && current.userId != user.id) return
            SessionManager.save(context, user.displayName(), user.email?.substringBefore("@")?.let { "@$it" }.orEmpty(),
                user.displayPhone(), user.email.orEmpty(), me.isProfileComplete())
            val profile = RidaApiClient.api.getProfile()
            ProfilePhotoContract.photoUrl(profile, RidaApiClient.BASE_URL)?.let {
                SessionManager.updateProfilePicture(context, it)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            SessionManager.clear(context)
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) SessionManager.clear(context)
        } catch (_: java.io.IOException) {
            // Keep the persisted session available while offline.
        }
    }

    suspend fun updateProfileOnServer(context: Context, fullName: String, phone: String?, vehicleType: String? = null): Result<Unit> = try {
        val original = SessionManager.observe(context).first()
        val request = mutableMapOf("full_name" to fullName.trim())
        phone?.let { request["phone_number"] = it }
        vehicleType?.let { request["vehicle_type"] = it }
        RidaApiClient.api.updateProfile(request)
        val current = SessionManager.observe(context).first()
        if (!current.loggedIn || current.userId != original.userId) throw CancellationException("The signed-in account changed.")
        SessionManager.updateProfile(context, fullName.trim(), phone ?: current.phone, current.email)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(IllegalArgumentException(RidaApiErrors.message(e, "Could not save your profile. Try again."), e))
    }
}
