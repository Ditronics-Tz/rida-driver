package com.example.ridedriver.data.repository

import android.content.Context
import com.example.ridedriver.data.driver.DriverRepositoryProvider
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.remote.RidaApiClient
import com.example.ridedriver.data.remote.UpdateDriverProfileRequest
import kotlinx.coroutines.flow.first

object UserRepository {

    suspend fun refreshSessionFromApi(context: Context) {
        val owner = SessionManager.observe(context).first()
        if (!owner.loggedIn) return
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
                profileCompleted = me.isProfileComplete(),
                expectedSessionId = owner.sessionId
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled } catch (_: Exception) { }

        try {
            DriverRepositoryProvider.get().refreshProfile()
            DriverRepositoryProvider.get().refreshDocuments()
            DriverRepositoryProvider.get().refreshHistory()
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled } catch (_: Exception) { }
    }

    suspend fun updateProfileOnServer(context: Context, fullName: String, phone: String?): Result<Unit> {
        val owner = SessionManager.observe(context).first()
        return try {
            DriverRepositoryProvider.get().updateProfile(fullName, phone).getOrThrow()
            val current = SessionManager.observe(context).first()
            SessionManager.updateProfile(context, fullName, phone ?: current.phone, current.email, expectedSessionId = owner.sessionId)
            Result.success(Unit)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
