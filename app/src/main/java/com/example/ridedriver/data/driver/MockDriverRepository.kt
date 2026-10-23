package com.example.ridedriver.data.driver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * High-fidelity on-device driver simulator.
 * Generates realistic incoming ride offers around Dar es Salaam when online,
 * manages the complete trip lifecycle, waiting timers, cash collection, and earnings.
 */
class MockDriverRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : DriverRepository {

    private val _state = MutableStateFlow(createInitialState())
    override val state: StateFlow<DriverState> = _state.asStateFlow()

    private var offerTickerJob: Job? = null
    private var offerDispatchJob: Job? = null
    private var locationUpdatesCount = 0

    init {
        // Pre-populate with realistic completed trip history
        seedTripHistory()
    }

    private fun createInitialState(): DriverState {
        val defaultProfile = DriverProfile(
            id = "drv_tz_001",
            name = "Juma Bakari Mzee",
            phone = "+255712345678",
            email = "juma.mzee@rida.co.tz",
            photoUrl = null,
            rating = 4.92,
            totalTrips = 348,
            acceptanceRate = 96,
            cancellationRate = 2,
            memberSince = "Jan 2026",
            verification = VerificationStatus.APPROVED
        )

        val defaultVehicle = Vehicle(
            type = VehicleType.CAR,
            make = "Toyota",
            model = "IST",
            color = "Silver",
            plate = "T 421 DKL",
            year = 2017
        )

        val defaultDocs = listOf(
            DriverDocument(
                id = "doc_license",
                title = "Driver's License (Class C)",
                description = "Valid Tanzania Driving License",
                status = DocumentStatus.APPROVED,
                expiresAt = "2028-11-30"
            ),
            DriverDocument(
                id = "doc_insurance",
                title = "Vehicle Insurance (Comprehensive)",
                description = "Jubilee Insurance coverage",
                status = DocumentStatus.APPROVED,
                expiresAt = "2027-04-15"
            ),
            DriverDocument(
                id = "doc_latra",
                title = "LATRA Commercial Permit",
                description = "Ride hailing authorization badge",
                status = DocumentStatus.APPROVED,
                expiresAt = "2026-12-31"
            ),
            DriverDocument(
                id = "doc_inspection",
                title = "Vehicle Inspection Certificate",
                description = "Police Vehicle Inspection sticker",
                status = DocumentStatus.APPROVED,
                expiresAt = "2027-01-20"
            )
        )

        return DriverState(
            online = false,
            profile = defaultProfile,
            vehicle = defaultVehicle,
            documents = defaultDocs,
            location = Place(lat = -6.7924, lng = 39.2083, address = "Dar es Salaam, Posta")
        )
    }

    private fun seedTripHistory() {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 3600 * 1000L

        val trips = listOf(
            TripRecord(
                id = "trip_hist_1",
                passengerName = "Amina Salim",
                pickup = Place(-6.8163, 39.2804, "Kariakoo Market, Swahili St"),
                dropoff = Place(-6.7725, 39.2412, "Mlimani City Mall, Sam Nujoma Rd"),
                fareTzs = 9500,
                commissionTzs = 1425,
                netTzs = 8075,
                waitingFeeTzs = 0,
                distanceMeters = 8400,
                durationSeconds = 1140,
                status = TripStatus.COMPLETED,
                endedAtMillis = now - 3600 * 1000 * 2,
                paymentMethod = "cash",
                ratingGiven = 5
            ),
            TripRecord(
                id = "trip_hist_2",
                passengerName = "Baraka John",
                pickup = Place(-6.7924, 39.2083, "Posta Mpya, Kivukoni"),
                dropoff = Place(-6.7562, 39.2501, "Masaki Peninsula, Haile Selassie Rd"),
                fareTzs = 12000,
                commissionTzs = 1800,
                netTzs = 10200,
                waitingFeeTzs = 500,
                distanceMeters = 10200,
                durationSeconds = 1380,
                status = TripStatus.COMPLETED,
                endedAtMillis = now - 3600 * 1000 * 5,
                paymentMethod = "mobile_money",
                ratingGiven = 5
            ),
            TripRecord(
                id = "trip_hist_3",
                passengerName = "Rehema Mwangi",
                pickup = Place(-6.7621, 39.2241, "Mwenge Bus Terminal"),
                dropoff = Place(-6.7911, 39.2398, "Sinza Palestina Hospital"),
                fareTzs = 6000,
                commissionTzs = 900,
                netTzs = 5100,
                waitingFeeTzs = 0,
                distanceMeters = 4200,
                durationSeconds = 620,
                status = TripStatus.COMPLETED,
                endedAtMillis = now - dayMillis,
                paymentMethod = "cash",
                ratingGiven = 4
            ),
            TripRecord(
                id = "trip_hist_4",
                passengerName = "Emanuel Tesha",
                pickup = Place(-6.8290, 39.2610, "Ilala Boma"),
                dropoff = Place(-6.8720, 39.2010, "Julius Nyerere Int'l Airport (JNIA)"),
                fareTzs = 18500,
                commissionTzs = 2775,
                netTzs = 15725,
                waitingFeeTzs = 0,
                distanceMeters = 13600,
                durationSeconds = 1750,
                status = TripStatus.COMPLETED,
                endedAtMillis = now - dayMillis * 2,
                paymentMethod = "cash",
                ratingGiven = 5
            )
        )

        _state.update { it.copy(history = trips) }
    }

    override suspend fun refreshProfile(): Result<DriverProfile> {
        return Result.success(_state.value.profile)
    }

    override suspend fun updateProfile(fullName: String, phone: String?): Result<DriverProfile> {
        _state.update { current ->
            val updated = current.profile.copy(
                name = fullName.trim(),
                phone = phone?.trim() ?: current.profile.phone
            )
            current.copy(profile = updated)
        }
        return Result.success(_state.value.profile)
    }

    override suspend fun updateVehicle(vehicle: Vehicle): Result<Vehicle> {
        _state.update { it.copy(vehicle = vehicle) }
        return Result.success(vehicle)
    }

    override suspend fun refreshDocuments(): Result<List<DriverDocument>> {
        return Result.success(_state.value.documents)
    }

    override suspend fun uploadDocument(docId: String, title: String): Result<DriverDocument> {
        var updatedDoc: DriverDocument? = null
        _state.update { current ->
            val docs = current.documents.map { doc ->
                if (doc.id == docId) {
                    val updated = doc.copy(status = DocumentStatus.PENDING, note = "Uploaded, under review")
                    updatedDoc = updated
                    updated
                } else doc
            }
            current.copy(documents = docs)
        }
        return Result.success(updatedDoc ?: DriverDocument(docId, title, "", DocumentStatus.PENDING))
    }

    override suspend fun setOnline(online: Boolean): Result<Unit> {
        _state.update { it.copy(goingOnline = true) }
        delay(400) // Realistic toggle response

        _state.update { current ->
            current.copy(
                online = online,
                goingOnline = false,
                onlineSinceMillis = if (online) System.currentTimeMillis() else null,
                offer = if (!online) null else current.offer
            )
        }

        if (online) {
            startSimulatingOffers()
        } else {
            stopSimulatingOffers()
        }
        return Result.success(Unit)
    }

    override suspend fun updateLocation(lat: Double, lng: Double, bearing: Float?, address: String?) {
        locationUpdatesCount++
        _state.update { current ->
            val place = Place(lat = lat, lng = lng, address = address ?: current.location?.address ?: "Current location")
            current.copy(location = place)
        }
    }

    private fun startSimulatingOffers() {
        offerDispatchJob?.cancel()
        offerDispatchJob = scope.launch {
            // First offer arrives quickly after going online (3-6s), subsequent every 15-30s
            delay(3500)
            while (isActive) {
                val current = _state.value
                if (current.online && current.trip == null && current.offer == null) {
                    val newOffer = generateRandomOffer(current.location)
                    _state.update { it.copy(offer = newOffer) }
                    startOfferCountdown(newOffer)
                }
                delay(20000)
            }
        }
    }

    private fun stopSimulatingOffers() {
        offerDispatchJob?.cancel()
        offerDispatchJob = null
        offerTickerJob?.cancel()
        offerTickerJob = null
    }

    private fun startOfferCountdown(offer: RideOffer) {
        offerTickerJob?.cancel()
        offerTickerJob = scope.launch {
            var secondsLeft = offer.expiresInSeconds
            while (secondsLeft > 0 && isActive) {
                delay(1000)
                secondsLeft--
                val curOffer = _state.value.offer
                if (curOffer == null || curOffer.id != offer.id) break
                if (secondsLeft <= 0) {
                    // Auto-expired
                    _state.update { it.copy(offer = null) }
                    break
                }
            }
        }
    }

    private fun generateRandomOffer(driverLoc: Place?): RideOffer {
        val samples = listOf(
            Pair(
                Place(-6.8163, 39.2804, "Kariakoo Market, Swahili St"),
                Place(-6.7725, 39.2412, "Mlimani City Mall, Sam Nujoma Rd")
            ),
            Pair(
                Place(-6.7562, 39.2501, "Shoppers Plaza, Mikocheni"),
                Place(-6.7924, 39.2083, "Posta Mpya, Azikiwe St")
            ),
            Pair(
                Place(-6.7621, 39.2241, "Mwenge Bus Stand"),
                Place(-6.8041, 39.2482, "Ubungo Maji, Morogoro Rd")
            ),
            Pair(
                Place(-6.7480, 39.2740, "Slipway Hotel, Masaki"),
                Place(-6.8180, 39.2880, "Clock Tower, Samora Ave")
            ),
            Pair(
                Place(-6.7820, 39.2250, "Sinza Mori, Shekilango Rd"),
                Place(-6.8720, 39.2010, "JNIA Terminal 3, Nyerere Rd")
            )
        )

        val pair = samples.random()
        val passengerNames = listOf("Amina Nassor", "Kelvin Mushi", "Grace Mollel", "Zuberi Mwinyi", "Fatma Rashid", "David Lyimo")
        val pName = passengerNames.random()
        val rating = (45 + (0..5).random()).toDouble() / 10.0 // 4.5 to 5.0

        val tripDist = (3500..12000).random().toLong()
        val tripDur = (tripDist / 7).coerceAtLeast(300) // approx 25 km/h
        val pickupDist = (400..2200).random().toLong()
        val fare = DriverRules.estimateFare(tripDist, tripDur, _state.value.vehicle.type)

        return RideOffer(
            id = "offer_" + UUID.randomUUID().toString().take(8),
            passengerName = pName,
            passengerRating = rating,
            passengerPhone = "+2557" + (10000000..99999999).random().toString(),
            pickup = pair.first,
            dropoff = pair.second,
            fareTzs = fare,
            distanceToPickupMeters = pickupDist,
            tripDistanceMeters = tripDist,
            tripDurationSeconds = tripDur,
            paymentMethod = if ((1..10).random() > 3) "cash" else "mobile_money",
            vehicleType = _state.value.vehicle.type,
            expiresInSeconds = RideOffer.OFFER_TIMEOUT_SECONDS,
            receivedAtMillis = System.currentTimeMillis()
        )
    }

    override suspend fun acceptOffer(offerId: String): Result<ActiveTrip> {
        val current = _state.value
        val offer = current.offer
        if (offer == null || offer.id != offerId) {
            return Result.failure(IllegalStateException("Offer expired or no longer available"))
        }

        offerTickerJob?.cancel()
        val trip = ActiveTrip(
            id = "trip_" + UUID.randomUUID().toString().take(8),
            offer = offer,
            phase = TripPhase.TO_PICKUP,
            acceptedAtMillis = System.currentTimeMillis()
        )

        _state.update {
            it.copy(
                offer = null,
                trip = trip
            )
        }
        return Result.success(trip)
    }

    override suspend fun declineOffer(offerId: String): Result<Unit> {
        offerTickerJob?.cancel()
        _state.update { current ->
            if (current.offer?.id == offerId) current.copy(offer = null) else current
        }
        return Result.success(Unit)
    }

    override suspend fun advanceTripPhase(): Result<TripPhase> {
        val currentTrip = _state.value.trip ?: return Result.failure(IllegalStateException("No active trip"))
        val next = DriverRules.nextPhase(currentTrip.phase) ?: return Result.failure(IllegalStateException("Trip is already finished"))

        val now = System.currentTimeMillis()
        val updatedTrip = when (next) {
            TripPhase.ARRIVED -> currentTrip.copy(phase = next, arrivedAtMillis = now)
            TripPhase.IN_TRIP -> currentTrip.copy(phase = next, startedAtMillis = now)
            TripPhase.COMPLETED -> currentTrip.copy(phase = next)
            else -> currentTrip.copy(phase = next)
        }

        if (next == TripPhase.COMPLETED) {
            val record = DriverRules.completeTrip(updatedTrip, now)
            _state.update { current ->
                val newHistory = listOf(record) + current.history
                current.copy(
                    trip = null,
                    lastCompleted = record,
                    history = newHistory
                )
            }
        } else {
            _state.update { it.copy(trip = updatedTrip) }
        }

        return Result.success(next)
    }

    override suspend fun cancelActiveTrip(reason: String): Result<Unit> {
        val currentTrip = _state.value.trip ?: return Result.failure(IllegalStateException("No active trip to cancel"))
        val now = System.currentTimeMillis()
        val record = DriverRules.cancelledTrip(currentTrip, reason, now)

        _state.update { current ->
            current.copy(
                trip = null,
                history = listOf(record) + current.history
            )
        }
        return Result.success(Unit)
    }

    override suspend fun ratePassenger(tripId: String, score: Int): Result<Unit> {
        _state.update { current ->
            val updated = current.history.map {
                if (it.id == tripId) it.copy(ratingGiven = score) else it
            }
            val last = if (current.lastCompleted?.id == tripId) current.lastCompleted.copy(ratingGiven = score) else current.lastCompleted
            current.copy(history = updated, lastCompleted = last)
        }
        return Result.success(Unit)
    }

    override suspend fun dismissCompletedTrip() {
        _state.update { it.copy(lastCompleted = null) }
    }

    override suspend fun getEarnings(period: EarningsPeriod): EarningsSummary {
        val current = _state.value
        val now = System.currentTimeMillis()
        return DriverRules.summarize(current.history, period, now, onlineMinutes = current.onlineMinutesToday)
    }

    override suspend fun refreshHistory(): Result<List<TripRecord>> {
        return Result.success(_state.value.history)
    }
}
