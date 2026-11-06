package com.example.ridedriver.data.remote

import retrofit2.http.*

data class SharedPoint(val lat: Double, val lng: Double, val address: String)

data class PublishTrip(
    val pickup: SharedPoint,
    val dropoff: SharedPoint,
    val capacity: Int,
    val seats: Int,
    val price: Long,
    val wait_minutes: Int,
)

data class Seat(val id: String, val status: String, val expires: Long)

data class PickupAction(val seat_id: String? = null, val pin: String? = null)

data class TransportFees(val enabled: Boolean, val shared_fee: Long, val hail_percent: Int)

data class SharedTrip(
    val id: String,
    val pickup: SharedPoint,
    val dropoff: SharedPoint,
    val vehicle: String,
    val seats: Int,
    val price: Long,
    val fee: Long,
    val status: String,
    val deadline: Long,
    val available: Int,
    val held: Int,
    val confirmed: Int,
    val server_time: Long,
    val manifest: List<Seat> = emptyList(),
    val my_seat: Seat? = null,
    val pin: String? = null,
)

data class TripPage(val trips: List<SharedTrip>)

interface TransportApi {
    @GET("transport/watch")
    suspend fun waitForUpdate(
        @Query("trip_id") tripId: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
    ): retrofit2.Response<Unit>

    @GET("transport/settings") suspend fun fees(): TransportFees

    @GET("shared-trips")
    suspend fun list(
        @Query("scope") scope: String,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): TripPage

    @GET("shared-trips/{id}") suspend fun detail(@Path("id") id: String): SharedTrip

    @POST("shared-trips")
    suspend fun publish(@Header("Idempotency-Key") key: String, @Body trip: PublishTrip): SharedTrip

    @POST("shared-trips/{id}/{action}")
    suspend fun act(
        @Path("id") id: String,
        @Path("action") action: String,
        @Header("Idempotency-Key") key: String,
        @Body request: PickupAction = PickupAction(),
    ): SharedTrip
}
