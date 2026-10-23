package com.example.ridedriver.data.repository

import android.content.Context
import com.example.ridedriver.data.driver.DriverRepositoryProvider
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.remote.RidaApiClient
import com.example.ridedriver.data.remote.UpdateDriverProfileRequest
import kotlinx.coroutines.flow.first

object UserRepository {

    suspend fun refreshSessionFromApi(context: Context) {
        try {
            val me = RidaApiClient.api.me()
            val user = me.user
            val handle = user.email?.substringBefore("@")?.let { "@$it" }
                ?: user.phone?.let { "@${it.takeLast(4)}" }
                ?: ""
            SessionManager.save(
                context = context,
                name = user.displayName(),
                handle = handle,
                phone = user.displayPhone(),
                email = user.email ?: "",
                profileCompleted = me.isProfileComplete()
            )
        } catch (_: Exception) { }

        try {
            DriverRepositoryProvider.get().refreshProfile()
            DriverRepositoryProvider.get().refreshDocuments()
            DriverRepositoryProvider.get().refreshHistory()
        } catch (_: Exception) { }
    }

    suspend fun updateProfileOnServer(context: Context, fullName: String, phone: String?): Result<Unit> {
        return try {
            DriverRepositoryProvider.get().updateProfile(fullName, phone)
            val current = SessionManager.observe(context).first()
            SessionManager.updateProfile(context, fullName, phone ?: current.phone, current.email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
