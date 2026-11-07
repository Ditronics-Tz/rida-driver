package com.example.ridedriver.data.driver

import com.example.ridedriver.data.remote.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume

class RemoteDriverStateMachineTest {
    private fun ride(status: String = "assigned") = DriverRideDto("ride", status,
        pickup = DriverPointDto(-6.8, 39.2), dropoff = DriverPointDto(-6.9, 39.3))
    private class Harness {
        var handler: (String, Array<out Any?>) -> Any? = { name, _ -> error(name) }
        val api = Proxy.newProxyInstance(DriverApi::class.java.classLoader, arrayOf(DriverApi::class.java)) { _, m, args -> handler(m.name, args ?: emptyArray()) } as DriverApi
        val repo = RemoteDriverRepository(api) { "stable-key" }
    }
    @Test fun delayedPollCannotUndoAcceptedOffer() = runBlocking {
        val h = Harness()
        val pollStarted = CompletableDeferred<Unit>()
        lateinit var poll: Continuation<Response<DriverRideDto>>
        h.handler = { name, args -> when (name) {
            "getCurrentRide" -> { @Suppress("UNCHECKED_CAST")
                poll = args.last() as Continuation<Response<DriverRideDto>>; pollStarted.complete(Unit); COROUTINE_SUSPENDED }
            "acceptOffer" -> ride()
            "getOffers" -> OfferListResponse()
            else -> error(name)
        } }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { h.repo.syncRides() }
        pollStarted.await()
        val accepted = async(start = CoroutineStart.UNDISPATCHED) { h.repo.acceptOffer("ride") }
        poll.resume(Response.success(null))
        job.join(); assertTrue(accepted.await().isSuccess)
        assertEquals("ride", h.repo.state.value.trip?.id)
    }
    @Test fun rejectedRatingDoesNotBecomeSuccessfulLocalRating() = runBlocking {
        val h = Harness()
        h.handler = { name, _ -> when (name) {
            "acceptOffer" -> ride()
            "markArrived" -> ride("driver_arrived")
            "startRide" -> ride("in_progress")
            "completeRide" -> ride("completed")
            else -> error(name)
        } }
        h.repo.acceptOffer("ride").getOrThrow()
        repeat(3) { h.repo.advanceTripPhase().getOrThrow() }
        h.handler = { name, _ -> when (name) {
            "ratePassenger" -> Response.error<Unit>(409, "conflict".toResponseBody())
            else -> error(name)
        } }
        assertTrue(h.repo.ratePassenger("ride", 5).isFailure)
        assertNull(h.repo.state.value.lastCompleted?.ratingGiven)
        assertNull(h.repo.state.value.history.single().ratingGiven)
    }
    @Test fun successfulAvailabilitySurvivesSubsequentLocationFailure() = runBlocking {
        val h = Harness()
        h.handler = { name, _ -> when (name) {
            "updateLocation" -> Response.success(Unit)
            "setStatus" -> DriverStatusDto(online = true)
            else -> error(name)
        } }
        h.repo.updateLocation(-6.8, 39.2, null, null)
        h.handler = { name, _ -> when (name) {
            "updateLocation" -> Response.error<Unit>(503, "unavailable".toResponseBody())
            "setStatus" -> DriverStatusDto(online = true)
            else -> error(name)
        } }
        h.repo.setOnline(true)
        assertTrue(h.repo.state.value.online)
        assertFalse(h.repo.state.value.goingOnline)
    }
    @Test fun cancellationPropagatesToCaller() = runBlocking {
        val h = Harness()
        h.handler = { _, _ -> throw CancellationException("screen left") }
        try { h.repo.refreshHistory(); fail("Cancellation was swallowed") } catch (_: CancellationException) { }
    }
    @Test fun duplicatePhaseTapCannotStartTripAfterFirstTapMarksArrival() = runBlocking {
        val h = Harness()
        h.handler = { name, _ -> if (name == "acceptOffer") ride() else error(name) }
        h.repo.acceptOffer("ride").getOrThrow()
        lateinit var arrived: Continuation<DriverRideDto>
        var starts = 0
        h.handler = { name, args -> when (name) {
            "markArrived" -> { @Suppress("UNCHECKED_CAST")
                arrived = args.last() as Continuation<DriverRideDto>; COROUTINE_SUSPENDED }
            "startRide" -> { starts++; ride("in_progress") }
            else -> error(name)
        } }
        val first = async(start = CoroutineStart.UNDISPATCHED) { h.repo.advanceTripPhase() }
        val duplicate = async(start = CoroutineStart.UNDISPATCHED) { h.repo.advanceTripPhase() }
        arrived.resume(ride("driver_arrived"))
        assertTrue(first.await().isSuccess)
        assertTrue(duplicate.await().isFailure)
        assertEquals(0, starts)
        assertEquals(TripPhase.ARRIVED, h.repo.state.value.trip?.phase)
    }
    @Test fun completedTripIsAcknowledgedEvenWhenNextOfferFetchFails() = runBlocking {
        val h = Harness()
        h.handler = { name, _ -> if (name == "acceptOffer") ride() else error(name) }
        h.repo.acceptOffer("ride").getOrThrow()
        h.handler = { name, _ -> when (name) {
            "getCurrentRide" -> Response.success(null)
            "ride" -> ride("completed")
            "getOffers" -> throw java.io.IOException("offline")
            else -> error(name)
        } }
        try { h.repo.syncRides() } catch (_: Exception) { }
        assertNull(h.repo.state.value.trip)
        assertEquals("ride", h.repo.state.value.lastCompleted?.id)
    }
    @Test fun unsupportedPersonalProfileSaveNeverClaimsPersistence() = runBlocking {
        val h = Harness()
        h.handler = { _, _ -> error("Unsupported endpoint must not be called") }
        assertTrue(h.repo.updateProfile("New name", "+255712345678").isFailure)
    }

    @Test fun pollingSameOfferDoesNotRestartItsExpiryClock() = runBlocking {
        val h = Harness()
        h.handler = { name, _ -> when (name) {
            "getCurrentRide" -> Response.success(null)
            "getOffers" -> OfferListResponse(listOf(RideOfferDto("offer", pickup = DriverPointDto(-6.8,39.2), dropoff = DriverPointDto(-6.9,39.3))))
            else -> error(name)
        } }
        h.repo.syncRides()
        val first = h.repo.state.value.offer
        h.repo.syncRides()
        assertSame(first, h.repo.state.value.offer)
    }

}
