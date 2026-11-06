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

data class PickupAction(val seat_id: String? = null, val pin: String? = null, val reason: String? = null)

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
 val capacity: Int = 0,
 val version: Long = 0,
 val fee_version: Int = 0,
 val driver_name: String = "",
 val passenger_names: Map<String,String> = emptyMap(),
 val receipt: TransportReceipt? = null,
)

data class TransportReceipt(val mode:String,val trip_id:String,val gross:Long,val fee:Long,val net:Long,val passengers:Int,val fee_version:Int,val status:String)
data class TransportChange(val id:Long,val trip_id:String,val version:Long,val type:String)
data class TransportChanges(val cursor:Long,val events:List<TransportChange>,val reset:Boolean=false)
data class SharedRatingInput(val score:Int,val comment:String="",val seat_id:String?=null)
data class SharedSOSInput(val lat:Double,val lng:Double,val accuracy:Double=0.0,val silent:Boolean=false)
data class SharedSupportInput(val subject:String,val description:String,val category:String="ride",val shared_trip_id:String)
data class TripPage(val trips: List<SharedTrip>)

interface TransportApi {
 @POST("shared-trips/{id}/rating") suspend fun rate(@Path("id") id:String,@Body input:SharedRatingInput):Map<String,Any>
 @POST("shared-trips/{id}/sos") suspend fun sos(@Path("id") id:String,@Body input:SharedSOSInput):Map<String,Any>
 @POST("support/tickets") suspend fun support(@Body input:SharedSupportInput):Map<String,Any>

    @GET("transport/watch")
    suspend fun waitForUpdate(
        @Query("trip_id") tripId: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
     @Query("after") after:Long=0,
 @Query("radius_km") radius:Double=10.0,
 ): TransportChanges

    @GET("transport/settings") suspend fun fees(): TransportFees

    @GET("shared-trips")
    suspend fun list(
        @Query("scope") scope: String,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
 @Query("radius_km") radius:Double=10.0,
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

internal fun mergeTransportSnapshots(previous:List<SharedTrip>,incoming:List<SharedTrip>):List<SharedTrip> = incoming.map{next->previous.find{it.id==next.id}?.takeIf{it.version>next.version}?:next}
internal fun remainingHoldSeconds(expires:Long,serverTime:Long,baselineMillis:Long,nowMonotonicMillis:Long):Long = (expires-serverTime-((nowMonotonicMillis-baselineMillis).coerceAtLeast(0)/1000)).coerceAtLeast(0)
