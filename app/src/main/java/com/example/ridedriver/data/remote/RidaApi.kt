package com.example.ridedriver.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.PUT
import retrofit2.http.Header
import retrofit2.http.Path
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

    @GET("driver/profile/me")
    suspend fun getProfile(): Map<String, Any?>

    @PUT("driver/profile/me")
    suspend fun updateProfile(@Body request: Map<String, String>): Map<String, Any?>

    @Multipart
    @POST("driver/profile/me/images")
    suspend fun uploadProfileImage(@Part image: MultipartBody.Part): Map<String, Any?>

    @GET("wallet")
    suspend fun wallet(): Map<String, Any?>

    @POST("driver/payouts")
    suspend fun requestPayout(@Header("Idempotency-Key") key: String, @Body body: DriverPayoutRequest): Map<String, Any?>

    @GET("support/tickets/{id}")
    suspend fun supportCase(@Path("id") id: String): Map<String, Any?>

    @POST("support/tickets/{id}/messages")
    suspend fun replyToCase(@Path("id") id: String, @Header("Idempotency-Key") key: String, @Body body: Map<String, String>): Map<String, Any?>

    @POST("account/export-request")
    suspend fun requestDataExport(@Body body: Map<String, String> = emptyMap()): Map<String, Any?>

    @POST("account/deletion-request")
    suspend fun requestAccountDeletion(@Body body: Map<String, String> = emptyMap()): Map<String, Any?>

    @POST("support/tickets")
    suspend fun createSupportTicket(@Body request: CreateTicketRequest): TicketDto

    @GET("support/tickets")
    suspend fun getSupportTickets(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): TicketListResponse
}
