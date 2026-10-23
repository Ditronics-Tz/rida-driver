package com.example.ridedriver.data.driver

import com.example.ridedriver.data.remote.CompleteRideRequest
import com.example.ridedriver.data.remote.DriverApi
import com.example.ridedriver.data.remote.DriverCancelRequest
import com.example.ridedriver.data.remote.DriverLocationRequest
import com.example.ridedriver.data.remote.DriverPointDto
import com.example.ridedriver.data.remote.DriverRideDto
import com.example.ridedriver.data.remote.DriverStatusRequest
import com.example.ridedriver.data.remote.RatePassengerRequest
import com.example.ridedriver.data.remote.RidaApiClient
import com.example.ridedriver.data.remote.RideOfferDto
import com.example.ridedriver.data.remote.UpdateDriverProfileRequest
import com.example.ridedriver.data.remote.VehicleDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Live REST implementation of [DriverRepository] that calls [DriverApi].
 */
class RemoteDriverRepository(
    private val api: DriverApi = RidaApiClient.driverApi
) : DriverRepository {

    private val _state = MutableStateFlow(DriverState())
    override val state: StateFlow<DriverState> = _state.asStateFlow()

    override suspend fun refreshProfile(): Result<DriverProfile> = runCatching {
        val dto = api.getProfile()
        val profile = DriverProfile(
            id = dto.id.orEmpty(),
            name = dto.fullName?.takeIf { it.isNotBlank() } ?: dto.name.orEmpty(),
            phone = dto.phone.orEmpty(),
            email = dto.email.orEmpty(),
            photoUrl = dto.profilePictureUrl,
            rating = dto.rating ?: 5.0,
            totalTrips = dto.totalTrips ?: 0,
            acceptanceRate = dto.acceptanceRate?.toInt() ?: 100,
            cancellationRate = dto.cancellationRate?.toInt() ?: 0,
            verification = when (dto.verificationStatus?.lowercase()) {
                "approved" -> VerificationStatus.APPROVED
                "rejected" -> VerificationStatus.REJECTED
                else -> VerificationStatus.PENDING
            }
        )
        _state.update { it.copy(profile = profile, online = dto.isOnline ?: it.online) }
        profile
    }

    override suspend fun updateProfile(fullName: String, phone: String?): Result<DriverProfile> = runCatching {
        val dto = api.updateProfile(UpdateDriverProfileRequest(fullName = fullName, phone = phone))
        val profile = _state.value.profile.copy(
            name = dto.fullName ?: fullName,
            phone = dto.phone ?: phone ?: _state.value.profile.phone
        )
        _state.update { it.copy(profile = profile) }
        profile
    }

    override suspend fun updateVehicle(vehicle: Vehicle): Result<Vehicle> = runCatching {
        val dto = api.updateVehicle(
            VehicleDto(
                vehicleType = vehicle.type.wire,
                make = vehicle.make,
                model = vehicle.model,
                color = vehicle.color,
                plateNumber = vehicle.plate,
                year = vehicle.year
            )
        )
        val updated = vehicle.copy(
            type = VehicleType.fromWire(dto.vehicleType),
            make = dto.make.orEmpty(),
            model = dto.model.orEmpty(),
            color = dto.color.orEmpty(),
            plate = dto.plateNumber.orEmpty(),
            year = dto.year
        )
        _state.update { it.copy(vehicle = updated) }
        updated
    }

    override suspend fun refreshDocuments(): Result<List<DriverDocument>> = runCatching {
        val resp = api.getDocuments()
        val docs = resp.documents.map { dto ->
            DriverDocument(
                id = dto.id,
                title = dto.title ?: dto.kind ?: "Document",
                description = dto.kind ?: "",
                status = DocumentStatus.fromWire(dto.status),
                expiresAt = dto.expiresAt,
                note = dto.note
            )
        }
        _state.update { it.copy(documents = docs) }
        docs
    }

    override suspend fun uploadDocument(docId: String, title: String): Result<DriverDocument> = runCatching {
        // Document upload endpoint or multipart
        DriverDocument(id = docId, title = title, description = "", status = DocumentStatus.PENDING)
    }

    override suspend fun setOnline(online: Boolean): Result<Unit> = runCatching {
        _state.update { it.copy(goingOnline = true) }
        try {
            val loc = _state.value.location
            api.setStatus(DriverStatusRequest(online = online, lat = loc?.lat, lng = loc?.lng))
            _state.update {
                it.copy(
                    online = online,
                    goingOnline = false,
                    onlineSinceMillis = if (online) System.currentTimeMillis() else null
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(goingOnline = false) }
            throw e
        }
    }

    override suspend fun updateLocation(lat: Double, lng: Double, bearing: Float?, address: String?) {
        _state.update { current ->
            current.copy(location = Place(lat, lng, address ?: current.location?.address ?: ""))
        }
        runCatching {
            api.updateLocation(DriverLocationRequest(lat = lat, lng = lng, bearing = bearing))
        }
    }

    override suspend fun acceptOffer(offerId: String): Result<ActiveTrip> = runCatching {
        val idempotency = RidaApiClient.generateIdempotencyKey()
        val rideDto = api.acceptOffer(offerId, idempotency)
        val trip = mapRideDtoToActiveTrip(rideDto)
        _state.update { it.copy(offer = null, trip = trip) }
        trip
    }

    override suspend fun declineOffer(offerId: String): Result<Unit> = runCatching {
        api.declineOffer(offerId)
        _state.update { if (it.offer?.id == offerId) it.copy(offer = null) else it }
    }

    override suspend fun advanceTripPhase(): Result<TripPhase> = runCatching {
        val currentTrip = _state.value.trip ?: error("No active trip")
        val idempotency = RidaApiClient.generateIdempotencyKey()
        val next = DriverRules.nextPhase(currentTrip.phase) ?: error("Cannot advance finished trip")

        when (next) {
            TripPhase.ARRIVED -> {
                api.markArrived(currentTrip.id, idempotency)
                val updated = currentTrip.copy(phase = next, arrivedAtMillis = System.currentTimeMillis())
                _state.update { it.copy(trip = updated) }
                next
            }
            TripPhase.IN_TRIP -> {
                api.startRide(currentTrip.id, idempotency)
                val updated = currentTrip.copy(phase = next, startedAtMillis = System.currentTimeMillis())
                _state.update { it.copy(trip = updated) }
                next
            }
            TripPhase.COMPLETED -> {
                val loc = _state.value.location
                val done = api.completeRide(currentTrip.id, idempotency, CompleteRideRequest(lat = loc?.lat, lng = loc?.lng))
                val record = mapRideDtoToTripRecord(done)
                _state.update { it.copy(trip = null, lastCompleted = record, history = listOf(record) + it.history) }
                next
            }
            else -> next
        }
    }

    override suspend fun cancelActiveTrip(reason: String): Result<Unit> = runCatching {
        val currentTrip = _state.value.trip ?: error("No active trip")
        val idempotency = RidaApiClient.generateIdempotencyKey()
        val cancelled = api.cancelRide(currentTrip.id, idempotency, DriverCancelRequest(reason))
        val record = mapRideDtoToTripRecord(cancelled)
        _state.update { it.copy(trip = null, history = listOf(record) + it.history) }
    }

    override suspend fun ratePassenger(tripId: String, score: Int): Result<Unit> = runCatching {
        api.ratePassenger(tripId, RatePassengerRequest(score = score))
        _state.update { current ->
            val updated = current.history.map { if (it.id == tripId) it.copy(ratingGiven = score) else it }
            val last = if (current.lastCompleted?.id == tripId) current.lastCompleted.copy(ratingGiven = score) else current.lastCompleted
            current.copy(history = updated, lastCompleted = last)
        }
    }

    override suspend fun dismissCompletedTrip() {
        _state.update { it.copy(lastCompleted = null) }
    }

    override suspend fun getEarnings(period: EarningsPeriod): EarningsSummary {
        val now = System.currentTimeMillis()
        return try {
            val dto = api.getEarnings(period.wire)
            EarningsSummary(
                period = period,
                grossTzs = dto.grossTzs,
                commissionTzs = dto.commissionTzs,
                netTzs = dto.netTzs,
                trips = dto.trips,
                distanceMeters = dto.distanceMeters,
                onlineMinutes = dto.onlineMinutes,
                daily = emptyList(),
                records = _state.value.history
            )
        } catch (_: Exception) {
            DriverRules.summarize(_state.value.history, period, now, _state.value.onlineMinutesToday)
        }
    }

    override suspend fun refreshHistory(): Result<List<TripRecord>> = runCatching {
        val resp = api.getRides()
        val records = resp.rides.map(::mapRideDtoToTripRecord)
        _state.update { it.copy(history = records) }
        records
    }

    private fun mapRideDtoToActiveTrip(dto: DriverRideDto): ActiveTrip {
        val pickup = mapPoint(dto.pickup)
        val dropoff = mapPoint(dto.dropoff)
        val fare = dto.finalFare ?: dto.estimatedFare ?: 5000L
        val offer = RideOffer(
            id = dto.id,
            passengerName = dto.passenger?.fullName ?: dto.passenger?.name ?: "Passenger",
            passengerRating = dto.passenger?.rating ?: 5.0,
            passengerPhone = dto.passenger?.phone,
            pickup = pickup,
            dropoff = dropoff,
            fareTzs = fare,
            distanceToPickupMeters = 500,
            tripDistanceMeters = dto.distanceMeters ?: 4000L,
            tripDurationSeconds = dto.durationSeconds ?: 600L,
            paymentMethod = dto.paymentMethod ?: "cash",
            vehicleType = VehicleType.fromWire(dto.vehicleType)
        )
        val phase = when (dto.status.lowercase()) {
            "arrived" -> TripPhase.ARRIVED
            "in_progress", "started" -> TripPhase.IN_TRIP
            else -> TripPhase.TO_PICKUP
        }
        return ActiveTrip(id = dto.id, offer = offer, phase = phase)
    }

    private fun mapRideDtoToTripRecord(dto: DriverRideDto): TripRecord {
        val pickup = mapPoint(dto.pickup)
        val dropoff = mapPoint(dto.dropoff)
        val fare = dto.finalFare ?: dto.estimatedFare ?: 0L
        val comm = dto.commissionTzs ?: DriverRules.commission(fare)
        val net = dto.driverEarningsTzs ?: (fare - comm)
        return TripRecord(
            id = dto.id,
            passengerName = dto.passenger?.fullName ?: dto.passenger?.name ?: "Passenger",
            pickup = pickup,
            dropoff = dropoff,
            fareTzs = fare,
            commissionTzs = comm,
            netTzs = net,
            distanceMeters = dto.distanceMeters ?: 0L,
            durationSeconds = dto.durationSeconds ?: 0L,
            status = if (dto.status.equals("cancelled", true)) TripStatus.CANCELLED else TripStatus.COMPLETED,
            endedAtMillis = System.currentTimeMillis(),
            paymentMethod = dto.paymentMethod ?: "cash",
            cancelReason = dto.cancelReason
        )
    }

    private fun mapPoint(dto: DriverPointDto): Place =
        Place(lat = dto.lat, lng = dto.lng, address = dto.address ?: "")
}
