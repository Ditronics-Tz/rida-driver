package com.example.ridedriver.ui.navigation

object DriverRoutes {
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val FORGOT_PASSWORD = "forgot_password"

    const val HOME = "driver_home"
    const val EARNINGS = "driver_earnings"
    const val TRIPS = "driver_trips"
    const val TRIP_DETAIL = "driver_trip_detail/{tripId}"
    const val EDIT_PROFILE = "driver_edit_profile"
    const val PROFILE = "driver_profile"
    const val VEHICLE_EDIT = "driver_vehicle_edit"
    const val DOCUMENTS = "driver_documents"
    const val SETTINGS = "driver_settings"
    const val BREAKDOWN = "earnings_breakdown"
    const val WALLET = "driver_wallet"
    const val PRIVACY = "driver_privacy"
    const val SUPPORT_CASE = "support_case/{caseId}"
    const val RIDE_SUPPORT = "ride_support/{rideId}"
    fun supportCase(id: String) = "support_case/${android.net.Uri.encode(id)}"
    fun rideSupport(id: String) = "ride_support/${android.net.Uri.encode(id)}"
    const val SUPPORT = "driver_support"

    fun tripDetail(tripId: String): String = "driver_trip_detail/$tripId"
}
