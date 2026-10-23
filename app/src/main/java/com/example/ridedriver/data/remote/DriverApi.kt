package com.example.ridedriver.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Driver routes under `/api/v1/driver/` (the router exists on the backend and is
 * protected by the same bearer token as the passenger routes).
 *
 * NOTE: the exact contract is not documented in this workspace. These paths/fields
 * follow the passenger API's conventions (snake_case, `*_tzs`, `limit/offset`,
 * `Idempotency-Key` on mutations). Adjust here once the backend spec is shared and
 * set `USE_MOCK_DRIVER_API=false` in app/build.gradle.kts.
 */
interface DriverApi {

    // Profile, vehicle & documents --------------------------------------------

    @GET("driver/profile/me")
    suspend fun getProfile(): DriverProfileDto

    @PUT("driver/profile/me")
    suspend fun updateProfile(@Body request: UpdateDriverProfileRequest): DriverProfileDto

    @GET("driver/vehicle")
    suspend fun getVehicle(): Response<VehicleDto>

    @PUT("driver/vehicle")
    suspend fun updateVehicle(@Body vehicle: VehicleDto): VehicleDto

    @GET("driver/documents")
    suspend fun getDocuments(): DocumentListResponse

    // Availability & location ---------------------------------------------------

    @POST("driver/status")
    suspend fun setStatus(@Body request: DriverStatusRequest): DriverStatusDto

    @POST("driver/location")
    suspend fun updateLocation(@Body request: DriverLocationRequest): Response<Unit>

    // Offers ----------------------------------------------------------------------

    @GET("driver/offers")
    suspend fun getOffers(): OfferListResponse

    @POST("driver/offers/{offerId}/accept")
    suspend fun acceptOffer(
        @Path("offerId") offerId: String,
        @Header("Idempotency-Key") idempotencyKey: String
    ): DriverRideDto

    @POST("driver/offers/{offerId}/decline")
    suspend fun declineOffer(@Path("offerId") offerId: String): Response<Unit>

    // Active ride -----------------------------------------------------------------

    @GET("driver/rides/current")
    suspend fun getCurrentRide(): Response<DriverRideDto>

    @POST("driver/rides/{rideId}/arrived")
    suspend fun markArrived(
        @Path("rideId") rideId: String,
        @Header("Idempotency-Key") idempotencyKey: String
    ): DriverRideDto

    @POST("driver/rides/{rideId}/start")
    suspend fun startRide(
        @Path("rideId") rideId: String,
        @Header("Idempotency-Key") idempotencyKey: String
    ): DriverRideDto

    @POST("driver/rides/{rideId}/complete")
    suspend fun completeRide(
        @Path("rideId") rideId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CompleteRideRequest
    ): DriverRideDto

    @POST("driver/rides/{rideId}/cancel")
    suspend fun cancelRide(
        @Path("rideId") rideId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: DriverCancelRequest
    ): DriverRideDto

    @POST("driver/rides/{rideId}/rating")
    suspend fun ratePassenger(
        @Path("rideId") rideId: String,
        @Body request: RatePassengerRequest
    ): Response<Unit>

    // History & earnings ------------------------------------------------------------

    @GET("driver/rides")
    suspend fun getRides(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("status") status: String? = null
    ): DriverRideListResponse

    @GET("driver/earnings")
    suspend fun getEarnings(@Query("period") period: String): EarningsDto
}

// -----------------------------------------------------------------------------
// DTOs
// -----------------------------------------------------------------------------

@JsonClass(generateAdapter = false)
data class DriverPointDto(
    val lat: Double,
    val lng: Double,
    val address: String? = null
)

@JsonClass(generateAdapter = false)
data class DriverProfileDto(
    val id: String? = null,
    @param:Json(name = "full_name") val fullName: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    @param:Json(name = "profile_picture_url") val profilePictureUrl: String? = null,
    val rating: Double? = null,
    @param:Json(name = "total_trips") val totalTrips: Int? = null,
    @param:Json(name = "acceptance_rate") val acceptanceRate: Double? = null,
    @param:Json(name = "cancellation_rate") val cancellationRate: Double? = null,
    @param:Json(name = "verification_status") val verificationStatus: String? = null,
    @param:Json(name = "is_online") val isOnline: Boolean? = null,
    @param:Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = false)
data class UpdateDriverProfileRequest(
    @param:Json(name = "full_name") val fullName: String,
    val phone: String? = null
)

@JsonClass(generateAdapter = false)
data class VehicleDto(
    @param:Json(name = "vehicle_type") val vehicleType: String? = null,
    val make: String? = null,
    val model: String? = null,
    val color: String? = null,
    @param:Json(name = "plate_number") val plateNumber: String? = null,
    val year: Int? = null
)

@JsonClass(generateAdapter = false)
data class DocumentDto(
    val id: String,
    val kind: String? = null,
    val title: String? = null,
    val status: String? = null,
    @param:Json(name = "expires_at") val expiresAt: String? = null,
    val note: String? = null
)

@JsonClass(generateAdapter = false)
data class DocumentListResponse(
    val documents: List<DocumentDto> = emptyList()
)

@JsonClass(generateAdapter = false)
data class DriverStatusRequest(
    val online: Boolean,
    val lat: Double? = null,
    val lng: Double? = null
)

@JsonClass(generateAdapter = false)
data class DriverStatusDto(
    val online: Boolean? = null,
    val status: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = false)
data class DriverLocationRequest(
    val lat: Double,
    val lng: Double,
    val bearing: Float? = null,
    val speed: Float? = null,
    val accuracy: Float? = null
)

@JsonClass(generateAdapter = false)
data class PassengerSummaryDto(
    val id: String? = null,
    val name: String? = null,
    @param:Json(name = "full_name") val fullName: String? = null,
    val phone: String? = null,
    val rating: Double? = null
)

@JsonClass(generateAdapter = false)
data class RideOfferDto(
    val id: String,
    @param:Json(name = "ride_id") val rideId: String? = null,
    val passenger: PassengerSummaryDto? = null,
    val pickup: DriverPointDto,
    val dropoff: DriverPointDto,
    @param:Json(name = "estimated_fare_tzs") val estimatedFareTzs: Long? = null,
    @param:Json(name = "estimated_fare") val estimatedFare: Long? = null,
    @param:Json(name = "pickup_distance_meters") val pickupDistanceMeters: Long? = null,
    @param:Json(name = "estimated_distance_meters") val estimatedDistanceMeters: Long? = null,
    @param:Json(name = "estimated_duration_seconds") val estimatedDurationSeconds: Long? = null,
    @param:Json(name = "payment_method") val paymentMethod: String? = null,
    @param:Json(name = "vehicle_type") val vehicleType: String? = null,
    @param:Json(name = "expires_in") val expiresIn: Int? = null
)

@JsonClass(generateAdapter = false)
data class OfferListResponse(
    val offers: List<RideOfferDto> = emptyList()
)

@JsonClass(generateAdapter = false)
data class DriverRideDto(
    val id: String,
    val status: String,
    val passenger: PassengerSummaryDto? = null,
    val pickup: DriverPointDto,
    val dropoff: DriverPointDto,
    @param:Json(name = "vehicle_type") val vehicleType: String? = null,
    @param:Json(name = "estimated_fare") val estimatedFare: Long? = null,
    @param:Json(name = "final_fare") val finalFare: Long? = null,
    @param:Json(name = "commission_tzs") val commissionTzs: Long? = null,
    @param:Json(name = "driver_earnings_tzs") val driverEarningsTzs: Long? = null,
    @param:Json(name = "distance_meters") val distanceMeters: Long? = null,
    @param:Json(name = "duration_seconds") val durationSeconds: Long? = null,
    @param:Json(name = "payment_method") val paymentMethod: String? = null,
    @param:Json(name = "cancel_reason") val cancelReason: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "arrived_at") val arrivedAt: String? = null,
    @param:Json(name = "started_at") val startedAt: String? = null,
    @param:Json(name = "completed_at") val completedAt: String? = null
)

@JsonClass(generateAdapter = false)
data class DriverRideListResponse(
    val rides: List<DriverRideDto> = emptyList(),
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0
)

@JsonClass(generateAdapter = false)
data class CompleteRideRequest(
    @param:Json(name = "final_fare") val finalFare: Long? = null,
    @param:Json(name = "waiting_fee_tzs") val waitingFeeTzs: Long? = null,
    val lat: Double? = null,
    val lng: Double? = null
)

@JsonClass(generateAdapter = false)
data class DriverCancelRequest(
    val reason: String
)

@JsonClass(generateAdapter = false)
data class RatePassengerRequest(
    val score: Int,
    val comment: String? = null
)

@JsonClass(generateAdapter = false)
data class EarningsDto(
    val period: String? = null,
    @param:Json(name = "gross_tzs") val grossTzs: Long = 0,
    @param:Json(name = "commission_tzs") val commissionTzs: Long = 0,
    @param:Json(name = "net_tzs") val netTzs: Long = 0,
    val trips: Int = 0,
    @param:Json(name = "distance_meters") val distanceMeters: Long = 0,
    @param:Json(name = "online_minutes") val onlineMinutes: Long = 0
)
