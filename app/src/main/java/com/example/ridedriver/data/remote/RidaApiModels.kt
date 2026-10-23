package com.example.ridedriver.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---------------------------------------------------------------------------
// Auth & Session Models (shared with the passenger app)
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = false)
data class RegisterRequest(
    val email: String,
    val password: String,
    val role: String? = "driver"
)

@JsonClass(generateAdapter = false)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = false)
data class GoogleChallengeResponse(
    val challengeId: String,
    val nonce: String,
    val expiresAt: String
)

@JsonClass(generateAdapter = false)
data class GoogleExchangeRequest(
    val idToken: String,
    val challengeId: String
)

@JsonClass(generateAdapter = false)
data class RefreshRequest(
    val refreshToken: String
)

@JsonClass(generateAdapter = false)
data class UserDto(
    val id: String,
    val name: String? = null,
    @param:Json(name = "full_name") val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @param:Json(name = "phone_number") val phoneNumber: String? = null,
    val role: String? = null,
    val status: String? = null,
    @param:Json(name = "is_active") val isActive: Boolean? = true,
    @param:Json(name = "phone_verified_at") val phoneVerifiedAt: String? = null,
    @param:Json(name = "suspended_until") val suspendedUntil: String? = null,
    val profileCompleted: Boolean? = null,
    @param:Json(name = "profile_completed") val profileCompletedSnake: Boolean? = null
) {
    fun isProfileComplete(): Boolean? = profileCompleted ?: profileCompletedSnake
    fun displayName(): String = fullName?.takeIf { it.isNotBlank() } ?: name.orEmpty()
    fun displayPhone(): String = phoneNumber?.takeIf { it.isNotBlank() } ?: phone.orEmpty()
}

@JsonClass(generateAdapter = false)
data class SessionResponse(
    val user: UserDto,
    val accessToken: String? = null,
    val expiresIn: Long? = null,
    val refreshToken: String? = null,
    val sessionId: String? = null,
    val onboardingRequired: List<String>? = emptyList(),
    val restricted: Boolean? = false,
    val message: String? = null,
    val profileCompleted: Boolean? = null,
    @param:Json(name = "profile_completed") val profileCompletedSnake: Boolean? = null,
    // snake_case aliases for compatibility
    val token: String? = null,
    @param:Json(name = "access_token") val accessTokenSnake: String? = null,
    @param:Json(name = "refresh_token") val refreshTokenSnake: String? = null,
    @param:Json(name = "expires_in") val expiresInSnake: Long? = null
) {
    fun getEffectiveAccessToken(): String = accessToken ?: accessTokenSnake ?: token ?: ""
    fun getEffectiveRefreshToken(): String = refreshToken ?: refreshTokenSnake ?: ""
    fun isProfileComplete(): Boolean? = profileCompleted ?: profileCompletedSnake ?: user.isProfileComplete()
}

@JsonClass(generateAdapter = false)
data class MeResponse(
    val user: UserDto,
    val onboardingRequired: List<String>? = emptyList(),
    val profileCompleted: Boolean? = null,
    @param:Json(name = "profile_completed") val profileCompletedSnake: Boolean? = null
) {
    fun isProfileComplete(): Boolean? = profileCompleted ?: profileCompletedSnake ?: user.isProfileComplete()
}

// ---------------------------------------------------------------------------
// OTP & Phone Verification
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = false)
data class RequestOtpRequest(
    val phone: String
)

@JsonClass(generateAdapter = false)
data class VerifyOtpRequest(
    val code: String
)

@JsonClass(generateAdapter = false)
data class OtpResponse(
    val message: String? = null,
    @param:Json(name = "expires_in") val expiresIn: Int? = null
)

// ---------------------------------------------------------------------------
// Support
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = false)
data class CreateTicketRequest(
    val subject: String,
    val description: String,
    val category: String = "driver"
)

@JsonClass(generateAdapter = false)
data class TicketDto(
    val id: String,
    val subject: String,
    val description: String,
    val category: String,
    val status: String,
    @param:Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = false)
data class TicketListResponse(
    val tickets: List<TicketDto> = emptyList(),
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0
)
