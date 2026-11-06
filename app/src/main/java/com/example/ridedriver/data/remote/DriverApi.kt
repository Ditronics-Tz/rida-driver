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

/** Authenticated driver and ride endpoints under /api/v1. */
interface DriverApi {
    @GET("driver/verification/me") suspend fun verification(): Verification
    @PUT("driver/profile/me") suspend fun identifiers(@Body input: VehicleIdentifiers): VehicleIdentifiers
    @POST("driver/verification/me/submit") suspend fun submitDocuments(): Verification
    @POST("driver/documents/presign") suspend fun presign(@Body input: DocumentUpload): UploadTicket
    @POST("driver/documents/{id}/confirm") suspend fun confirm(@Path("id") id: String): DocumentDto
    @retrofit2.http.Multipart @POST("driver/documents") suspend fun upload(
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part,
        @retrofit2.http.Part("type") type: okhttp3.RequestBody,
        @retrofit2.http.Part("expires_at") expiry: okhttp3.RequestBody
    ): DocumentDto

    // Profile, vehicle & documents --------------------------------------------

    @GET("driver/profile/me")
    suspend fun getProfile(): DriverProfileDto

    @PUT("driver/profile/me")
    suspend fun updateProfile(@Body request: UpdateDriverProfileRequest): DriverProfileDto

    @GET("driver/vehicle")
    suspend fun getVehicle(): Response<VehicleDto>

    @PUT("driver/vehicle")
    suspend fun updateVehicle(@Body vehicle: VehicleDto): VehicleDto

    @GET("driver/verification/me")
    suspend fun getDocuments(): DocumentListResponse

    // Availability & location ---------------------------------------------------

    @PUT("driver/availability")
    suspend fun setStatus(@Body request: DriverStatusRequest, @Header("Idempotency-Key") key: String = RidaApiClient.generateIdempotencyKey()): DriverStatusDto

    @POST("driver/location")
    suspend fun updateLocation(@Body request: DriverLocationRequest, @Header("Idempotency-Key") key: String = RidaApiClient.generateIdempotencyKey()): Response<Unit>

    // Offers ----------------------------------------------------------------------

    @GET("driver/offers")
    suspend fun getOffers(): OfferListResponse

    @POST("rides/{offerId}/accept")
    suspend fun acceptOffer(
        @Path("offerId") offerId: String,
        @Header("Idempotency-Key") idempotencyKey: String
    ): DriverRideDto

    @POST("rides/{offerId}/decline")
    suspend fun declineOffer(@Path("offerId") offerId: String, @Header("Idempotency-Key") key: String = RidaApiClient.generateIdempotencyKey()): Response<Unit>

    // Active ride -----------------------------------------------------------------

    @GET("rides/{id}") suspend fun ride(@Path("id") id:String):DriverRideDto

    @GET("driver/rides/current")
    suspend fun getCurrentRide(): Response<DriverRideDto>

    @POST("rides/{rideId}/arrived")
    suspend fun markArrived(
        @Path("rideId") rideId: String,
        @Header("Idempotency-Key") idempotencyKey: String
    ): DriverRideDto

    @POST("rides/{rideId}/start")
    suspend fun startRide(
        @Path("rideId") rideId: String,
        @Header("Idempotency-Key") idempotencyKey: String
    ): DriverRideDto

    @POST("rides/{rideId}/complete")
    suspend fun completeRide(
        @Path("rideId") rideId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CompleteRideRequest
    ): DriverRideDto

    @POST("rides/{rideId}/cancel")
    suspend fun cancelRide(
        @Path("rideId") rideId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: DriverCancelRequest
    ): DriverRideDto

    @POST("rides/{rideId}/rating")
    suspend fun ratePassenger(
        @Path("rideId") rideId: String,
        @Body request: RatePassengerRequest
    ): Response<Unit>

    // History & earnings ------------------------------------------------------------

    @GET("rides")
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
    @param:Json(name = "vehicle_type") val vehicleType: String? = null,
    @param:Json(name = "plate_number") val plateNumber: String? = null,
    val id: String? = null,
    @param:Json(name = "full_name") val fullName: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    @param:Json(name = "profile_picture_url") val profilePictureUrl: String? = null,
    @param:Json(name="average_rating") val rating: Double? = null,
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
    @param:Json(name = "type") val kind: String? = null,
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

data class VehicleIdentifiers(val licence_number: String = "", val vehicle_card_number: String = "",
    val plate_number: String = "", val vehicle_type: String = "", val passenger_seats: Int = 0)
data class Verification(val profile: VehicleIdentifiers, val documents: List<DocumentDto>, val required_types: List<String>)
data class DocumentUpload(val type: String, val content_type: String, val size: Long, val expires_at: String)
data class UploadTicket(val document: DocumentDto, val upload: UploadTarget)
data class UploadTarget(val url: String, val headers: Map<String,String> = emptyMap())
