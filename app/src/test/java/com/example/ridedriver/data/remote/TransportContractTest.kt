package com.example.ridedriver.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class TransportContractTest {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Test
    fun heldSeatAndLeadingZeroCodeRemainDistinctFromConfirmation() {
        val json =
            """{"id":"t","vehicle":"bajaji","pickup":{"address":"Mbande","lat":-6.94,"lng":39.19},
            "dropoff":{"address":"Posta","lat":-6.82,"lng":39.28},"status":"waiting","seats":3,"price":1800,
            "fee":700,"deadline":3000,"available":2,"held":1,"confirmed":0,"server_time":2000,
            "my_seat":{"id":"s","status":"held","expires":2300},"pin":"004123"}"""
        val trip = moshi.adapter(SharedTrip::class.java).fromJson(json)!!
        assertEquals("004123", trip.pin)
        assertEquals(300L, trip.my_seat!!.expires - trip.server_time)
        assertEquals(0, trip.confirmed)
    }
}
