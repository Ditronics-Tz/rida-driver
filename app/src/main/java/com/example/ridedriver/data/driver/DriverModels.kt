package com.example.ridedriver.data.driver

/** Domain models used by every driver screen. Independent of the wire format. */

data class Place(
    val lat: Double,
    val lng: Double,
    val address: String
)

enum class VehicleType(val wire: String, val label: String) {
    BODA("boda", "Boda Boda"),
    BAJAJI("bajaji", "Bajaji"),
    CAR("car", "Car"),
    XL("xl", "XL");

    companion object {
        fun fromWire(value: String?): VehicleType =
            entries.firstOrNull { it.wire.equals(value, ignoreCase = true) } ?: CAR
    }
}

enum class VerificationStatus { APPROVED, PENDING, REJECTED }

data class DriverProfile(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val rating: Double = 5.0,
    val totalTrips: Int = 0,
    val acceptanceRate: Int = 100,
    val cancellationRate: Int = 0,
    val memberSince: String = "",
    val verification: VerificationStatus = VerificationStatus.PENDING
)

data class Vehicle(
    val type: VehicleType = VehicleType.CAR,
    val make: String = "",
    val model: String = "",
    val color: String = "",
    val plate: String = "",
    val year: Int? = null
) {
    val title: String get() = listOf(make, model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { type.label }
    val isComplete: Boolean get() = plate.isNotBlank() && make.isNotBlank()
}

enum class DocumentStatus(val label: String) {
    APPROVED("Approved"),
    PENDING("In review"),
    REJECTED("Rejected"),
    EXPIRED("Expired"),
    MISSING("Upload needed");

    companion object {
        fun fromWire(value: String?): DocumentStatus = when (value?.lowercase()) {
            "approved", "verified", "valid" -> APPROVED
            "pending", "in_review", "submitted" -> PENDING
            "rejected", "declined" -> REJECTED
            "expired" -> EXPIRED
            else -> MISSING
        }
    }
}

data class DriverDocument(
    val id: String,
    val title: String,
    val description: String,
    val status: DocumentStatus,
    val expiresAt: String? = null,
    val note: String? = null
)

data class RideOffer(
    val id: String,
    val passengerName: String,
    val passengerRating: Double,
    val passengerPhone: String?,
    val pickup: Place,
    val dropoff: Place,
    val fareTzs: Long,
    val distanceToPickupMeters: Long,
    val tripDistanceMeters: Long,
    val tripDurationSeconds: Long,
    val paymentMethod: String = "cash",
    val vehicleType: VehicleType = VehicleType.CAR,
    val expiresInSeconds: Int = OFFER_TIMEOUT_SECONDS,
    val receivedAtMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val OFFER_TIMEOUT_SECONDS = 15
    }
}

/** Ordered lifecycle of an accepted ride, from the driver's point of view. */
enum class TripPhase(val title: String) {
    TO_PICKUP("Heading to pickup"),
    ARRIVED("Waiting for passenger"),
    IN_TRIP("Trip in progress"),
    COMPLETED("Trip completed"),
    CANCELLED("Trip cancelled")
}

data class ActiveTrip(
    val id: String,
    val offer: RideOffer,
    val phase: TripPhase = TripPhase.TO_PICKUP,
    val acceptedAtMillis: Long = System.currentTimeMillis(),
    val arrivedAtMillis: Long? = null,
    val startedAtMillis: Long? = null
) {
    /** Where the driver is currently driving to. */
    val target: Place get() = if (phase == TripPhase.IN_TRIP) offer.dropoff else offer.pickup
}

enum class TripStatus { COMPLETED, CANCELLED }

data class TripRecord(
    val id: String,
    val passengerName: String,
    val pickup: Place,
    val dropoff: Place,
    val fareTzs: Long,
    val commissionTzs: Long,
    val netTzs: Long,
    val waitingFeeTzs: Long = 0,
    val distanceMeters: Long,
    val durationSeconds: Long,
    val status: TripStatus,
    val endedAtMillis: Long,
    val paymentMethod: String = "cash",
    val cancelReason: String? = null,
    val ratingGiven: Int? = null
)

enum class EarningsPeriod(val label: String, val wire: String) {
    TODAY("Today", "today"),
    WEEK("This week", "week"),
    MONTH("This month", "month")
}

data class DayEarning(val label: String, val netTzs: Long, val trips: Int)

data class EarningsSummary(
    val period: EarningsPeriod,
    val grossTzs: Long,
    val commissionTzs: Long,
    val netTzs: Long,
    val trips: Int,
    val distanceMeters: Long,
    val onlineMinutes: Long,
    val daily: List<DayEarning>,
    val records: List<TripRecord>
)

/** Everything the UI needs, in one observable snapshot. */
data class DriverState(
    val online: Boolean = false,
    val goingOnline: Boolean = false,
    val profile: DriverProfile = DriverProfile(),
    val vehicle: Vehicle = Vehicle(),
    val documents: List<DriverDocument> = emptyList(),
    val offer: RideOffer? = null,
    val trip: ActiveTrip? = null,
    val lastCompleted: TripRecord? = null,
    val history: List<TripRecord> = emptyList(),
    val location: Place? = null,
    val onlineSinceMillis: Long? = null,
    val onlineMinutesToday: Long = 0,
    val busy: Boolean = false,
    val message: String? = null
) {
    val canGoOnline: Boolean get() = vehicle.isComplete && documents.none { it.status == DocumentStatus.REJECTED || it.status == DocumentStatus.EXPIRED }
}
