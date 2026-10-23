package com.example.ridedriver.ui.navigation

object DriverRoutes {
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val FORGOT_PASSWORD = "forgot_password"

    const val HOME = "driver_home"
    const val EARNINGS = "driver_earnings"
    const val TRIPS = "driver_trips"
    const val TRIP_DETAIL = "driver_trip_detail/{tripId}"
    const val PROFILE = "driver_profile"
    const val VEHICLE_EDIT = "driver_vehicle_edit"
    const val DOCUMENTS = "driver_documents"
    const val SETTINGS = "driver_settings"
    const val SUPPORT = "driver_support"

    fun tripDetail(tripId: String): String = "driver_trip_detail/$tripId"
}
