package com.example.ridedriver.data.driver

import kotlinx.coroutines.flow.StateFlow

/**
 * Single entry point for all driver business logic.
 * Implemented by [MockDriverRepository] (offline simulator) and [RemoteDriverRepository] (live REST API).
 */
interface DriverRepository {
    val state: StateFlow<DriverState>

    suspend fun refreshProfile(): Result<DriverProfile>
    suspend fun updateProfile(fullName: String, phone: String?): Result<DriverProfile>
    suspend fun updateVehicle(vehicle: Vehicle): Result<Vehicle>
    suspend fun refreshDocuments(): Result<List<DriverDocument>>
    suspend fun uploadDocument(docId: String, title: String): Result<DriverDocument>

    suspend fun setOnline(online: Boolean): Result<Unit>
    suspend fun updateLocation(lat: Double, lng: Double, bearing: Float? = null, address: String? = null)

    suspend fun acceptOffer(offerId: String): Result<ActiveTrip>
    suspend fun declineOffer(offerId: String): Result<Unit>

    suspend fun advanceTripPhase(): Result<TripPhase>
    suspend fun cancelActiveTrip(reason: String): Result<Unit>
    suspend fun ratePassenger(tripId: String, score: Int): Result<Unit>
    suspend fun dismissCompletedTrip()

    suspend fun getEarnings(period: EarningsPeriod): EarningsSummary
    suspend fun refreshHistory(): Result<List<TripRecord>>
}
