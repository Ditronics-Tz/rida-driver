package com.example.ridedriver.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Shared Rida endpoints (auth, session, support). These are the same, documented
 * routes the passenger app uses. Driver-specific routes live in [DriverApi].
 */
interface RidaApi {

    // ---------------------------------------------------------------------------
    // Public Health & Auth
    // ---------------------------------------------------------------------------

    @GET("health")
    suspend fun getHealth(): Map<String, Any>

    @GET("ready")
    suspend fun getReady(): Response<Unit>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): SessionResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): SessionResponse

    @POST("auth/google/challenge")
    suspend fun googleChallenge(@Body body: Map<String, String> = emptyMap()): GoogleChallengeResponse

    @POST("auth/google")
    suspend fun googleExchange(@Body request: GoogleExchangeRequest): SessionResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): SessionResponse

    // ---------------------------------------------------------------------------
    // User & Phone Verification
    // ---------------------------------------------------------------------------

    @GET("me")
    suspend fun me(): MeResponse

    @POST("auth/logout")
    suspend fun logout(): Map<String, Any>

    @POST("auth/phone/request-otp")
    suspend fun requestOtp(@Body request: RequestOtpRequest): OtpResponse

    @POST("auth/phone/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): MeResponse

    // ---------------------------------------------------------------------------
    // Support
    // ---------------------------------------------------------------------------

    @POST("support/tickets")
    suspend fun createSupportTicket(@Body request: CreateTicketRequest): TicketDto

    @GET("support/tickets")
    suspend fun getSupportTickets(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): TicketListResponse
}
