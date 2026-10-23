package com.example.ridedriver.data.driver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverRulesTest {

    @Test
    fun testPhaseTransitions() {
        assertEquals(TripPhase.ARRIVED, DriverRules.nextPhase(TripPhase.TO_PICKUP))
        assertEquals(TripPhase.IN_TRIP, DriverRules.nextPhase(TripPhase.ARRIVED))
        assertEquals(TripPhase.COMPLETED, DriverRules.nextPhase(TripPhase.IN_TRIP))
        assertNull(DriverRules.nextPhase(TripPhase.COMPLETED))
        assertNull(DriverRules.nextPhase(TripPhase.CANCELLED))
    }

    @Test
    fun testCancellationRules() {
        assertTrue(DriverRules.canCancel(TripPhase.TO_PICKUP))
        assertTrue(DriverRules.canCancel(TripPhase.ARRIVED))
        assertFalse(DriverRules.canCancel(TripPhase.IN_TRIP))
        assertFalse(DriverRules.canCancel(TripPhase.COMPLETED))
    }

    @Test
    fun testCommissionAndNet() {
        val fare = 10000L
        val commission = DriverRules.commission(fare)
        assertEquals(1500L, commission) // 15%
        val net = DriverRules.net(fare)
        assertEquals(8500L, net)
    }

    @Test
    fun testWaitingFee() {
        // First 3 minutes (180s) are free
        assertEquals(0L, DriverRules.waitingFee(0L))
        assertEquals(0L, DriverRules.waitingFee(120L))
        assertEquals(0L, DriverRules.waitingFee(180L))

        // Billable at 100 TZS per minute
        assertEquals(100L, DriverRules.waitingFee(181L))
        assertEquals(100L, DriverRules.waitingFee(240L))
        assertEquals(200L, DriverRules.waitingFee(241L))
        assertEquals(500L, DriverRules.waitingFee(480L))
    }

    @Test
    fun testFareEstimation() {
        // 5 km (5000m), 15 mins (900s)
        val bodaFare = DriverRules.estimateFare(5000L, 900L, VehicleType.BODA)
        val carFare = DriverRules.estimateFare(5000L, 900L, VehicleType.CAR)
        val xlFare = DriverRules.estimateFare(5000L, 900L, VehicleType.XL)

        assertTrue(bodaFare < carFare)
        assertTrue(carFare < xlFare)
        assertTrue(carFare >= 4000L) // Minimum car fare
    }

    @Test
    fun testTripCompletionSummary() {
        val now = 1761213600000L
        val offer = RideOffer(
            id = "o1",
            passengerName = "Amina",
            passengerRating = 4.9,
            passengerPhone = "+255712345678",
            pickup = Place(-6.8, 39.2, "Kariakoo"),
            dropoff = Place(-6.7, 39.2, "Mwenge"),
            fareTzs = 10000L,
            distanceToPickupMeters = 500,
            tripDistanceMeters = 6000,
            tripDurationSeconds = 900,
            paymentMethod = "cash"
        )
        val trip = ActiveTrip(
            id = "t1",
            offer = offer,
            phase = TripPhase.IN_TRIP,
            acceptedAtMillis = now - 1200_000,
            arrivedAtMillis = now - 920_000, // 120s wait (under 180s free limit, 0 waiting fee)
            startedAtMillis = now - 800_000
        )

        val record = DriverRules.completeTrip(trip, now, rating = 5)
        assertEquals("t1", record.id)
        assertEquals(TripStatus.COMPLETED, record.status)
        assertEquals(0L, record.waitingFeeTzs)
        assertEquals(10000L, record.fareTzs)
        assertEquals(1500L, record.commissionTzs)
        assertEquals(8500L, record.netTzs)
        assertEquals(5, record.ratingGiven)
    }

    @Test
    fun testEarningsSummarization() {
        val now = 1761213600000L
        val records = listOf(
            TripRecord(
                id = "1",
                passengerName = "Baraka",
                pickup = Place(0.0, 0.0, "P1"),
                dropoff = Place(0.0, 0.0, "D1"),
                fareTzs = 10000L,
                commissionTzs = 1500L,
                netTzs = 8500L,
                distanceMeters = 5000,
                durationSeconds = 600,
                status = TripStatus.COMPLETED,
                endedAtMillis = now - 1000
            ),
            TripRecord(
                id = "2",
                passengerName = "Rehema",
                pickup = Place(0.0, 0.0, "P2"),
                dropoff = Place(0.0, 0.0, "D2"),
                fareTzs = 6000L,
                commissionTzs = 900L,
                netTzs = 5100L,
                distanceMeters = 3000,
                durationSeconds = 400,
                status = TripStatus.COMPLETED,
                endedAtMillis = now - 2000
            )
        )

        val summary = DriverRules.summarize(records, EarningsPeriod.TODAY, now)
        assertEquals(16000L, summary.grossTzs)
        assertEquals(2400L, summary.commissionTzs)
        assertEquals(13600L, summary.netTzs)
        assertEquals(2, summary.trips)
    }
}
