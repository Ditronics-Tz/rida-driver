package com.example.ridedriver.data.driver

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/** Pure, unit-testable business rules for the driver app. */
object DriverRules {

    /** Rida platform commission taken from each completed trip. */
    const val COMMISSION_RATE = 0.15

    /** Free waiting time at pickup before the waiting fee kicks in. */
    const val FREE_WAIT_SECONDS = 180L

    /** Waiting fee per started minute after the free window. */
    const val WAIT_FEE_PER_MIN_TZS = 100L

    val TZ: TimeZone = TimeZone.getTimeZone("Africa/Dar_es_Salaam")

    // -------------------------------------------------------------------
    // Trip lifecycle
    // -------------------------------------------------------------------

    /** The only allowed forward transition from each phase (null = terminal). */
    fun nextPhase(phase: TripPhase): TripPhase? = when (phase) {
        TripPhase.TO_PICKUP -> TripPhase.ARRIVED
        TripPhase.ARRIVED -> TripPhase.IN_TRIP
        TripPhase.IN_TRIP -> TripPhase.COMPLETED
        TripPhase.COMPLETED, TripPhase.CANCELLED -> null
    }

    fun canCancel(phase: TripPhase): Boolean = phase == TripPhase.TO_PICKUP || phase == TripPhase.ARRIVED

    fun primaryActionLabel(phase: TripPhase): String = when (phase) {
        TripPhase.TO_PICKUP -> "I've arrived"
        TripPhase.ARRIVED -> "Start trip"
        TripPhase.IN_TRIP -> "Complete trip"
        TripPhase.COMPLETED, TripPhase.CANCELLED -> "Done"
    }

    // -------------------------------------------------------------------
    // Money
    // -------------------------------------------------------------------

    fun waitingFee(waitedSeconds: Long): Long {
        val billable = waitedSeconds - FREE_WAIT_SECONDS
        if (billable <= 0) return 0
        val minutes = (billable + 59) / 60
        return minutes * WAIT_FEE_PER_MIN_TZS
    }

    fun commission(fareTzs: Long): Long = roundTo50((fareTzs * COMMISSION_RATE))

    fun net(fareTzs: Long): Long = fareTzs - commission(fareTzs)

    private fun roundTo50(value: Double): Long = ((value / 50.0).roundToLong()) * 50

    /** Local fare estimate used by the simulator: base + per km, rounded to TSh 500. */
    fun estimateFare(distanceMeters: Long, durationSeconds: Long, type: VehicleType): Long {
        val (base, perKm, perMin, minimum) = when (type) {
            VehicleType.BODA -> listOf(1000L, 500L, 30L, 1500L)
            VehicleType.BAJAJI -> listOf(1500L, 650L, 40L, 2000L)
            VehicleType.CAR -> listOf(2500L, 900L, 60L, 4000L)
            VehicleType.XL -> listOf(3500L, 1200L, 80L, 6000L)
        }
        val raw = base + perKm * distanceMeters / 1000.0 + perMin * durationSeconds / 60.0
        val rounded = ((raw / 500.0).roundToLong()) * 500
        return maxOf(minimum, rounded)
    }

    fun completeTrip(trip: ActiveTrip, nowMillis: Long, rating: Int? = null): TripRecord {
        val waited = trip.arrivedAtMillis?.let { arrived ->
            val end = trip.startedAtMillis ?: nowMillis
            (end - arrived) / 1000
        } ?: 0L
        val waitFee = waitingFee(waited)
        val fare = trip.offer.fareTzs + waitFee
        val duration = trip.startedAtMillis?.let { (nowMillis - it) / 1000 } ?: trip.offer.tripDurationSeconds
        val commission = commission(fare)
        return TripRecord(
            id = trip.id,
            passengerName = trip.offer.passengerName,
            pickup = trip.offer.pickup,
            dropoff = trip.offer.dropoff,
            fareTzs = fare,
            commissionTzs = commission,
            netTzs = fare - commission,
            waitingFeeTzs = waitFee,
            distanceMeters = trip.offer.tripDistanceMeters,
            durationSeconds = duration.coerceAtLeast(60),
            status = TripStatus.COMPLETED,
            endedAtMillis = nowMillis,
            paymentMethod = trip.offer.paymentMethod,
            ratingGiven = rating
        )
    }

    fun cancelledTrip(trip: ActiveTrip, reason: String, nowMillis: Long): TripRecord = TripRecord(
        id = trip.id,
        passengerName = trip.offer.passengerName,
        pickup = trip.offer.pickup,
        dropoff = trip.offer.dropoff,
        fareTzs = 0,
        commissionTzs = 0,
        netTzs = 0,
        distanceMeters = 0,
        durationSeconds = 0,
        status = TripStatus.CANCELLED,
        endedAtMillis = nowMillis,
        paymentMethod = trip.offer.paymentMethod,
        cancelReason = reason
    )

    // -------------------------------------------------------------------
    // Offers
    // -------------------------------------------------------------------

    fun offerSecondsLeft(offer: RideOffer, nowMillis: Long): Int {
        val elapsed = ((nowMillis - offer.receivedAtMillis) / 1000).toInt()
        return (offer.expiresInSeconds - elapsed).coerceAtLeast(0)
    }

    // -------------------------------------------------------------------
    // Earnings
    // -------------------------------------------------------------------

    fun periodStart(period: EarningsPeriod, nowMillis: Long): Long {
        val cal = Calendar.getInstance(TZ, Locale.US).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        when (period) {
            EarningsPeriod.TODAY -> Unit
            EarningsPeriod.WEEK -> {
                cal.firstDayOfWeek = Calendar.MONDAY
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) cal.add(Calendar.DAY_OF_MONTH, -1)
            }
            EarningsPeriod.MONTH -> cal.set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    fun summarize(
        records: List<TripRecord>,
        period: EarningsPeriod,
        nowMillis: Long,
        onlineMinutes: Long = 0
    ): EarningsSummary {
        val start = periodStart(period, nowMillis)
        val inPeriod = records.filter { it.endedAtMillis in start..nowMillis }.sortedByDescending { it.endedAtMillis }
        val done = inPeriod.filter { it.status == TripStatus.COMPLETED }
        val daily = dailyBuckets(done, period, start, nowMillis)
        return EarningsSummary(
            period = period,
            grossTzs = done.sumOf { it.fareTzs },
            commissionTzs = done.sumOf { it.commissionTzs },
            netTzs = done.sumOf { it.netTzs },
            trips = done.size,
            distanceMeters = done.sumOf { it.distanceMeters },
            onlineMinutes = onlineMinutes,
            daily = daily,
            records = inPeriod
        )
    }

    private fun dailyBuckets(done: List<TripRecord>, period: EarningsPeriod, start: Long, now: Long): List<DayEarning> {
        val cal = Calendar.getInstance(TZ, Locale.US)
        if (period == EarningsPeriod.TODAY) {
            // 4-hour slots across today.
            return (0 until 6).map { slot ->
                val from = start + slot * 4 * 3_600_000L
                val to = from + 4 * 3_600_000L
                val bucket = done.filter { it.endedAtMillis in from until to }
                DayEarning(label = "%02d".format(slot * 4), netTzs = bucket.sumOf { it.netTzs }, trips = bucket.size)
            }
        }
        val days = mutableListOf<DayEarning>()
        cal.timeInMillis = start
        val labels = arrayOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val maxDays = if (period == EarningsPeriod.WEEK) 7 else cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        repeat(maxDays) {
            val from = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val to = cal.timeInMillis
            val bucket = done.filter { it.endedAtMillis in from until to }
            val label = if (period == EarningsPeriod.WEEK) {
                val c = Calendar.getInstance(TZ, Locale.US).apply { timeInMillis = from }
                labels[c.get(Calendar.DAY_OF_WEEK)]
            } else {
                (it + 1).toString()
            }
            if (from <= now || period == EarningsPeriod.WEEK) {
                days += DayEarning(label, bucket.sumOf { r -> r.netTzs }, bucket.size)
            }
        }
        return days
    }

    // -------------------------------------------------------------------
    // Geo
    // -------------------------------------------------------------------

    fun distanceMeters(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(bLat - aLat)
        val dLng = Math.toRadians(bLng - aLng)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(aLat)) * cos(Math.toRadians(bLat)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(h))
    }
}
