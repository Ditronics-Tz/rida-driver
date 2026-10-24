package com.example.ridedriver.ui.screens

import com.example.ridedriver.data.driver.*
import com.example.ridedriver.data.remote.DriverPayoutRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class DriverPagesTest {
    private fun trip(id: String, destination: String, status: TripStatus, time: Long) = TripRecord(
        id, "Passenger", Place(0.0, 0.0, "Kariakoo"), Place(0.0, 0.0, destination),
        10000, 1500, 8500, distanceMeters = 7000, durationSeconds = 1080, status = status, endedAtMillis = time
    )
    @Test fun rideSearchCombinesStatusDestinationAndNewestFirst() {
        val old = trip("old", "Mwenge", TripStatus.COMPLETED, 1000)
        val newest = trip("new", "MWENGE", TripStatus.CANCELLED, 3000)
        val other = trip("other", "Masaki", TripStatus.COMPLETED, 2000)
        val rows = listOf(old, newest, other)
        assertEquals(listOf("new", "old"), filterDriverRides(rows, "  mwenge ", null).map { it.id })
        assertEquals(listOf("old"), filterDriverRides(rows, "mwenge", TripStatus.COMPLETED).map { it.id })
        assertEquals(listOf("new", "other", "old"), filterDriverRides(rows, "Kariakoo", null).map { it.id })
        assertTrue(filterDriverRides(rows, "Airport", null).isEmpty())
    }
    @Test fun payoutAmountIsNumericInBackendWireFormat() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val json = moshi.adapter(DriverPayoutRequest::class.java).toJson(DriverPayoutRequest(12500, "mpesa", "+255712345678"))
        val payload = moshi.adapter(Map::class.java).fromJson(json)!!
        assertEquals(12500.0, payload["amount"])
        assertEquals("mpesa", payload["method"])
    }
    @Test fun moneyUsesWholeTanzanianShillingsWithoutIntegerOverflow() {
        assertEquals("TZS 12,500", driverMoney(12500))
        assertEquals("TZS 3,000,000,000", driverMoney(3_000_000_000))
    }
}
